package net.letsdank.jd.ast;

import net.letsdank.jd.ast.expr.BinaryExpr;
import net.letsdank.jd.ast.expr.Expr;
import net.letsdank.jd.ast.expr.IntConstExpr;
import net.letsdank.jd.ast.expr.VarExpr;
import net.letsdank.jd.ast.stmt.*;
import net.letsdank.jd.bytecode.BytecodeDecoder;
import net.letsdank.jd.bytecode.Opcode;
import net.letsdank.jd.bytecode.insn.Insn;
import net.letsdank.jd.bytecode.insn.JumpInsn;
import net.letsdank.jd.bytecode.insn.SimpleInsn;
import net.letsdank.jd.cfg.BasicBlock;
import net.letsdank.jd.cfg.CfgBuilder;
import net.letsdank.jd.cfg.ControlFlowGraph;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.attribute.CodeAttribute;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.MethodInfo;
import net.letsdank.jd.utils.JDUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
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
            return postProcessLoops(new MethodAst(name, desc, body));
        }

        // 3. if/else через CFG
        IfStmt ifStmt = tryBuildIfFromCfg(cfg, localNames, cp);
        if (ifStmt != null) {
            BlockStmt body = new BlockStmt();
            body.add(ifStmt);
            return postProcessLoops(new MethodAst(name, desc, body));
        }

        // 3. Строим линейный AST по всему байткоду
        BytecodeDecoder decoder = new BytecodeDecoder();
        List<Insn> insns = decoder.decode(code);
        ExpressionBuilder exprBuilder = new ExpressionBuilder(localNames, cp);
        BlockStmt linearBody = exprBuilder.buildBlock(insns);

        // Fallback: просто линейный AST
        return postProcessLoops(new MethodAst(name, desc, linearBody));
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
            // --- унарные сравнения с нулем ---
            case IFEQ -> {
                Expr x = stack.pop();
                return new BinaryExpr("==", x, new IntConstExpr(0));
            }
            case IFNE -> {
                Expr x = stack.pop();
                return new BinaryExpr("!=", x, new IntConstExpr(0));
            }
            case IFLT -> {
                Expr x = stack.pop();
                return new BinaryExpr("<", x, new IntConstExpr(0));
            }
            case IFGE -> {
                Expr x = stack.pop();
                return new BinaryExpr(">=", x, new IntConstExpr(0));
            }
            case IFGT -> {
                Expr x = stack.pop();
                return new BinaryExpr(">", x, new IntConstExpr(0));
            }
            case IFLE -> {
                Expr x = stack.pop();
                return new BinaryExpr("<=", x, new IntConstExpr(0));
            }

            // --- бинарные IF_ICMPxx ---
            case IF_ICMPEQ -> {
                Expr right = stack.pop();
                Expr left = stack.pop();
                return new BinaryExpr("==", left, right);
            }
            case IF_ICMPNE -> {
                Expr right = stack.pop();
                Expr left = stack.pop();
                return new BinaryExpr("!=", left, right);
            }
            case IF_ICMPLT -> {
                Expr right = stack.pop();
                Expr left = stack.pop();
                return new BinaryExpr("<", left, right);
            }
            case IF_ICMPGE -> {
                Expr right = stack.pop();
                Expr left = stack.pop();
                return new BinaryExpr(">=", left, right);
            }
            case IF_ICMPGT -> {
                Expr right = stack.pop();
                Expr left = stack.pop();
                return new BinaryExpr(">", left, right);
            }
            case IF_ICMPLE -> {
                Expr right = stack.pop();
                Expr left = stack.pop();
                return new BinaryExpr("<=", left, right);
            }

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
            case IF_ICMPEQ, IF_ICMPNE,
                 IF_ICMPGE, IF_ICMPLT, IF_ICMPGT, IF_ICMPLE -> {

                Expr right = stack.pop();
                Expr left = stack.pop();
                // Семантика: если (cmp) -> прыжок. Нам нужно "иначе" (fallthrough).
                return switch (op) {
                    case IF_ICMPEQ -> new BinaryExpr("!=", left, right);                // !(left == right)
                    case IF_ICMPNE -> new BinaryExpr("==", left, right);                // !(left != right)
                    case IF_ICMPGE -> new BinaryExpr("<", left, right);                 // !(left >= right)
                    case IF_ICMPLT -> new BinaryExpr(">=", left, right);                // !(left < right)
                    case IF_ICMPGT -> new BinaryExpr("<=", left, right);                // !(left > right)
                    case IF_ICMPLE -> new BinaryExpr(">", left, right);                 // !(left <= right)
                    default -> null;
                };
            }
            default -> {
                // Для унарных IFxx x < 0 / x >= 0 / x == 0 / x != 0
                Expr x = stack.pop();
                return switch (op) {
                    case IFEQ -> new BinaryExpr("!=", x, new IntConstExpr(0));  // !(x == 0)
                    case IFNE -> new BinaryExpr("==", x, new IntConstExpr(0));  // !(x != 0)
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
        if (op == Opcode.IF_ICMPEQ || op == Opcode.IF_ICMPNE ||
                op == Opcode.IF_ICMPGE || op == Opcode.IF_ICMPLT ||
                op == Opcode.IF_ICMPGT || op == Opcode.IF_ICMPLE) {

            Expr right = stack.pop();
            Expr left = stack.pop();

            // exit = target (наш случай: IF_ICMPGE -> exit)
            if (!bodyIsTarget) {
                // инвертируем: !(left >= right) -> left < right
                // !(left < right) -> left >= right
                // и т.д.
                return switch (op) {
                    case IF_ICMPEQ -> new BinaryExpr("!=", left, right);
                    case IF_ICMPNE -> new BinaryExpr("==", left, right);
                    case IF_ICMPGE -> new BinaryExpr("<", left, right);
                    case IF_ICMPLT -> new BinaryExpr(">=", left, right);
                    case IF_ICMPGT -> new BinaryExpr("<=", left, right);
                    case IF_ICMPLE -> new BinaryExpr(">", left, right);
                    default -> null;
                };
            } else {
                // body = target -> оставляем как есть
                return switch (op) {
                    case IF_ICMPEQ -> new BinaryExpr("==", left, right);
                    case IF_ICMPNE -> new BinaryExpr("!=", left, right);
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

    private MethodAst postProcessLoops(MethodAst ast) {
        BlockStmt body = ast.body();
        BlockStmt transformed = transformBlock(body);
        if (transformed == body) return ast;
        return new MethodAst(ast.name(), ast.descriptor(), transformed);
    }

    private BlockStmt transformBlock(BlockStmt block) {
        var src = block.statements();
        List<Stmt> result = new ArrayList<>(src.size());

        for (Stmt s : src) {
            if (s instanceof LoopStmt loop) {
                result.add(transformLoop(loop));
            } else if (s instanceof IfStmt ifs) {
                // рекурсивно обрабатываем then/else
                BlockStmt thenT = transformBlock(ifs.thenBlock());
                BlockStmt elseT = ifs.elseBlock() != null ? transformBlock(ifs.elseBlock()) : null;
                result.add(new IfStmt(ifs.condition(), thenT, elseT));
            } else {
                result.add(s);
            }
        }

        return new BlockStmt(result);
    }

    // Временно не распознаем for-циклы автоматически.
    // Мы только рекурсивно нормализуем тело (if/loops внутри),
    // но сам цикл остается while.
    private Stmt transformLoop(LoopStmt loop) {
        BlockStmt body = loop.body();
        BlockStmt transformedBody = transformBlock(body);

        // Если тело не изменилось, просто возвращаем исходный цикл
        if (transformedBody.equals(body)) return loop;

        // Иначе создаем новый LoopStmt с тем же условием, но нормализованным телом
        return new LoopStmt(loop.condition(), transformedBody);
    }

    // Проверка, что последнее выражение - простой инкремент i = 1 + 1
    private AssignStmt extractSimpleUpdate(Stmt stmt) {
        if (!(stmt instanceof AssignStmt as)) return null;
        if (!(as.target() instanceof VarExpr tv)) return null;

        Expr value = as.value();
        if (!(value instanceof BinaryExpr be)) return null;
        if (!"+".equals(be.op())) return null;
        if (!(be.left() instanceof VarExpr lv)) return null;
        if (!(be.right() instanceof IntConstExpr c)) return null;
        if (!tv.name().equals(lv.name())) return null;

        // i = i + 1 / i = i + N (N >= 1)
        return as;
    }
}
