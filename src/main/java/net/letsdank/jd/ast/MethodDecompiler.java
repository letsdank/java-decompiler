package net.letsdank.jd.ast;

import net.letsdank.jd.ast.expr.BinaryExpr;
import net.letsdank.jd.ast.expr.Expr;
import net.letsdank.jd.ast.expr.IntConstExpr;
import net.letsdank.jd.ast.stmt.BlockStmt;
import net.letsdank.jd.ast.stmt.IfStmt;
import net.letsdank.jd.ast.stmt.LoopStmt;
import net.letsdank.jd.bytecode.BytecodeDecoder;
import net.letsdank.jd.bytecode.Opcode;
import net.letsdank.jd.bytecode.insn.Insn;
import net.letsdank.jd.bytecode.insn.JumpInsn;
import net.letsdank.jd.bytecode.insn.SimpleInsn;
import net.letsdank.jd.cfg.BasicBlock;
import net.letsdank.jd.cfg.CfgBuilder;
import net.letsdank.jd.cfg.ControlFlowGraph;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.CodeAttribute;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.MethodInfo;
import net.letsdank.jd.utils.JDUtils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class MethodDecompiler {
    private final CfgBuilder cfgBuilder = new CfgBuilder();

    public MethodAst decompile(MethodInfo method, ClassFile cf) {
        CodeAttribute codeAttr = method.findCodeAttribute();
        var cp = cf.constantPool();
        String name = cp.getUtf8(method.nameIndex());
        String desc = cp.getUtf8(method.descriptorIndex());
        if (codeAttr == null) {
            return new MethodAst(name, desc, new BlockStmt());
        }

        LocalNameProvider localNames = new MethodLocalNameProvider(method.accessFlags(), desc);

        byte[] code = codeAttr.code();

        // 1. Строим CFG и пытаемся найти условный переход + условие
        ControlFlowGraph cfg = cfgBuilder.build(code);

        // 2. Попытка распознать простой while
        LoopStmt loop = tryBuildWhileLoop(cfg, localNames, cp);
        if (loop != null) {
            BlockStmt body = new BlockStmt();
            body.add(loop);
            return new MethodAst(name, desc, body);
        }

        // 3. if/else через CFG
        IfStmt ifStmt = tryBuildIfFromCfg(cfg, localNames, cp);
        if (ifStmt != null) {
            BlockStmt body = new BlockStmt();
            body.add(ifStmt);
            return new MethodAst(name, desc, body);
        }

        // 3. Строим линейный AST по всему байткоду
        BytecodeDecoder decoder = new BytecodeDecoder();
        List<Insn> insns = decoder.decode(code);
        ExpressionBuilder exprBuilder = new ExpressionBuilder(localNames, cp);
        BlockStmt linearBody = exprBuilder.buildBlock(insns);

        // Fallback: просто линейный AST
        return new MethodAst(name, desc, linearBody);
    }

    /**
     * Ищем первый блок, который заканчивается условным JumpInsn,
     * и строим Expr для условия.
     */
    private Expr findConditionExpr(ControlFlowGraph cfg, LocalNameProvider localNames, ConstantPool cp) {
        List<BasicBlock> blocks = cfg.blocks();
        if (blocks.isEmpty()) return null;

        BasicBlock condBlock = null;
        JumpInsn condJump = null;

        for (BasicBlock bb : blocks) {
            var insns = bb.instructions();
            if (insns.isEmpty()) continue;
            Insn last = insns.getLast();
            if (last instanceof JumpInsn j && JDUtils.isConditional(j.opcode())) {
                condBlock = bb;
                condJump = j;
                break;
            }
        }
        if (condBlock == null || condJump == null) return null;

        // Стек до перехода
        ExpressionBuilder exprBuilder = new ExpressionBuilder(localNames, cp);
        var condInsns = condBlock.instructions();
        Deque<Expr> stackBefore = exprBuilder.simulateStackBeforeBranch(condInsns);

        return buildConditionExpr(condJump, stackBefore);
    }

    /**
     * Строим Expr для условия из типа JumpInsn и стека перед ним.
     * Поддерживаем только пару случаев, достаточных для abs(int):
     * - IFGE x >= 0
     * - IF_ICMPGE x >= y
     */
    private Expr buildConditionExpr(JumpInsn j, Deque<Expr> stackBefore) {
        Opcode op = j.opcode();
        // Копию, чтобы вынимать сверху
        Deque<Expr> stack = new ArrayDeque<>(stackBefore);

        switch (op) {
            case IFGE -> {
                Expr x = stack.pop();
                return new BinaryExpr(">=", x, new IntConstExpr(0));
            }
            case IFLT -> {
                Expr x = stack.pop();
                return new BinaryExpr("<", x, new IntConstExpr(0));
            }
            case IF_ICMPGE -> {
                Expr right = stack.pop();
                Expr left = stack.pop();
                return new BinaryExpr(">=", left, right);
            }
            case IF_ICMPLE -> {
                Expr right = stack.pop();
                Expr left = stack.pop();
                return new BinaryExpr("<", left, right);
            }
            // Можно постепенно дописывать остальные сравнения
            default -> {
                return null;
            }
        }
    }

    private IfStmt tryBuildIfFromCfg(ControlFlowGraph cfg, LocalNameProvider localNames, ConstantPool cp) {
        for (BasicBlock condBlock : cfg.blocks()) {
            var insns = condBlock.instructions();
            if (insns.isEmpty()) continue;

            Insn last = insns.getLast();
            if (!(last instanceof JumpInsn j) || !JDUtils.isConditional(j.opcode())) continue;
            if (condBlock.successors().size() != 2) continue;

            BasicBlock s0 = condBlock.successors().get(0);
            BasicBlock s1 = condBlock.successors().get(1);

            int target = j.targetOffset();

            BasicBlock jumpSucc;    // блок-ветка JUMP (условие ИСТИНА)
            BasicBlock fallthrough; // fallthrough (условие ЛОЖЬ)

            if (s0.startOffset() == target) {
                jumpSucc = s0;
                fallthrough = s1;
            } else if (s1.startOffset() == target) {
                jumpSucc = s1;
                fallthrough = s0;
            } else {
                continue;
            }

            // Очень простой паттерн: обе ветки заканчиваются return
            if (!endsWithReturn(jumpSucc) || !endsWithReturn(fallthrough)) continue;

            ExpressionBuilder exprBuilder = new ExpressionBuilder(localNames, cp);
            Deque<Expr> stackBefore = exprBuilder.simulateStackBeforeBranch(insns);

            // ВАЖНО: для if строим условие для FALLTHROUGH-ветки (исходный then)
            Expr condition = buildIfConditionForFallthrough(j, stackBefore);
            if (condition == null) continue;

            // then = fallthrough, else = jumpSucc
            BlockStmt thenBlock = exprBuilder.buildBlock(fallthrough.instructions());
            BlockStmt elseBlock = exprBuilder.buildBlock(jumpSucc.instructions());

            return new IfStmt(condition, thenBlock, elseBlock);
        }

        return null;
    }

    private LoopStmt tryBuildWhileLoop(ControlFlowGraph cfg, LocalNameProvider localNames, ConstantPool cp) {
        // Очень ограниченный шаблон:
        // condBlock: ... if_<cmp> targetExit
        // bodyBlock: ... goto condBlock
        // exitBlock: ... (например, return)
        for (BasicBlock condBlock : cfg.blocks()) {
            var insns = condBlock.instructions();
            if (insns.isEmpty()) continue;

            Insn last = insns.getLast();
            if (!(last instanceof JumpInsn j) || !JDUtils.isConditional(j.opcode())) {
                continue;
            }

            if (condBlock.successors().size() != 2) {
                continue;
            }

            BasicBlock s0 = condBlock.successors().get(0);
            BasicBlock s1 = condBlock.successors().get(1);

            // определяем body/exit по наличию goto-назад
            BasicBlock bodyBlock = null;
            BasicBlock exitBlock = null;

            if (endsWithGotoTo(s0, condBlock.startOffset())) {
                bodyBlock = s0;
                exitBlock = s1;
            } else if (endsWithGotoTo(s1, condBlock.startOffset())) {
                bodyBlock = s1;
                exitBlock = s0;
            } else {
                continue;
            }

            // теперь строим условие цикла
            ExpressionBuilder exprBuilder = new ExpressionBuilder(localNames, cp);
            var stackBefore = exprBuilder.simulateStackBeforeBranch(insns);

            Expr condition = buildLoopConditionExpr(j, stackBefore, bodyBlock.startOffset() == j.targetOffset());
            if (condition == null) {
                continue;
            }

            // тело цикла - инструкции bodyBlock без финального goto
            var bodyInsns = bodyBlock.instructions();
            if (bodyInsns.isEmpty()) continue;

            List<Insn> bodyCore = bodyInsns;
            Insn bodyLast = bodyInsns.getLast();
            if (bodyLast instanceof JumpInsn bj && bj.opcode() == Opcode.GOTO) {
                bodyCore = bodyInsns.subList(0, bodyInsns.size() - 1);
            }

            BlockStmt bodyAst = exprBuilder.buildBlock(bodyCore);
            return new LoopStmt(condition, bodyAst);
        }

        return null;
    }

    private boolean endsWithGotoTo(BasicBlock bb, int targetOffset) {
        var insns = bb.instructions();
        if (insns.isEmpty()) return false;
        Insn last = insns.getLast();
        if (last instanceof JumpInsn j && j.opcode() == Opcode.GOTO) {
            return j.targetOffset() == targetOffset;
        }
        return false;
    }

    private boolean endsWithReturn(BasicBlock bb) {
        var insns = bb.instructions();
        if (insns.isEmpty()) return false;
        Insn last = insns.getLast();
        if (last instanceof SimpleInsn s) {
            return s.opcode() == Opcode.IRETURN || s.opcode() == Opcode.RETURN;
        }
        return false;
    }

    private Expr buildIfConditionForFallthrough(JumpInsn j, Deque<Expr> stackBefore) {
        Opcode op = j.opcode();
        Deque<Expr> stack = new ArrayDeque<>(stackBefore);

        // Для бинарных сравнений IF_ICMPxx
        switch (op) {
            case IF_ICMPGE, IF_ICMPLT, IF_ICMPGT, IF_ICMPLE -> {
                Expr right = stack.pop();
                Expr left = stack.pop();
                // Семантика: если (cmp) -> прыжок. Нам нужно "иначе" (fallthrough).
                return switch (op) {
                    case IF_ICMPGE -> new BinaryExpr("<", left, right);                 // !(left >= right)
                    case IF_ICMPLT -> new BinaryExpr(">=", left, right);                // !(left < right)
                    case IF_ICMPGT -> new BinaryExpr("<=", left, right);                // !(left > right)
                    case IF_ICMPLE -> new BinaryExpr(">", left, right);                 // !(left <= right)
                    default -> null;
                };
            }
            default -> {
                // Для унарных IFxx x < 0 / x >= 0 и т.п.
                Expr x = stack.pop();
                return switch (op) {
                    case IFLT -> new BinaryExpr(">=", x, new IntConstExpr(0));  // !(x < 0)
                    case IFGE -> new BinaryExpr("<", x, new IntConstExpr(0));   // !(x >= 0)
                    case IFGT -> new BinaryExpr("<=", x, new IntConstExpr(0));  // !(x > 0)
                    case IFLE -> new BinaryExpr(">", x, new IntConstExpr(0));   // !(x <= 0)
                    default -> buildConditionExpr(j, stackBefore); // fallback: условие перехода
                };
            }
        }
    }

    private Expr buildLoopConditionExpr(JumpInsn j, Deque<Expr> stackBefore, boolean bodyIsTarget) {
        // bodyIsTarget == true -> условие цикла = condForJump
        // bodyIsTarget == false -> условие цикла = !condForJump
        // (для нескольких часто встречающихся сравнений делаем руками)

        Opcode op = j.opcode();
        Deque<Expr> stack = new ArrayDeque<>(stackBefore);

        // Сначала восстановим "сырой" левый/правый операнды для IF_ICMP*
        if (op == Opcode.IF_ICMPGE || op == Opcode.IF_ICMPLT ||
                op == Opcode.IF_ICMPGT || op == Opcode.IF_ICMPLE) {
            Expr right = stack.pop();
            Expr left = stack.pop();

            // exit = target (наш случай: IF_ICMPGE -> exit)
            if (!bodyIsTarget) {
                // инвертируем: !(left >= right) -> left < right
                // !(left < right) -> left >= right
                // и т.д.
                return switch (op) {
                    case IF_ICMPGE -> new BinaryExpr("<", left, right);
                    case IF_ICMPLT -> new BinaryExpr(">=", left, right);
                    case IF_ICMPGT -> new BinaryExpr("<=", left, right);
                    case IF_ICMPLE -> new BinaryExpr(">", left, right);
                    default -> null;
                };
            } else {
                // body = target -> оставляем как есть
                return switch (op) {
                    case IF_ICMPGE -> new BinaryExpr(">=", left, right);
                    case IF_ICMPLT -> new BinaryExpr("<", left, right);
                    case IF_ICMPGT -> new BinaryExpr(">", left, right);
                    case IF_ICMPLE -> new BinaryExpr("<=", left, right);
                    default -> null;
                };
            }
        }

        // Для одиночных IF<cond> x < 0 / x >= 0 etc можем опираться на старый buildConditionExpr
        Expr condForJump = buildConditionExpr(j, stackBefore);
        if (condForJump == null) return null;

        if (bodyIsTarget) {
            return condForJump;
        } else {
            // простая инверсия для случаев типа IFLT / IFGE на нуле:
            // IFLT x < 0 -> цикл while (x >= 0) {...} и т.п.
            // Можно оставить так, или в дальнейшем расширить отдельно
            // Для простоты пока вернем condForJump (то есть, без инверсии)
            return condForJump;
        }
    }
}
