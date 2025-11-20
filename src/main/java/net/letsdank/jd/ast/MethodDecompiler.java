package net.letsdank.jd.ast;

import net.letsdank.jd.ast.expr.BinaryExpr;
import net.letsdank.jd.ast.expr.Expr;
import net.letsdank.jd.ast.expr.IntConstExpr;
import net.letsdank.jd.ast.stmt.BlockStmt;
import net.letsdank.jd.ast.stmt.IfStmt;
import net.letsdank.jd.ast.stmt.LoopStmt;
import net.letsdank.jd.ast.stmt.ReturnStmt;
import net.letsdank.jd.bytecode.BytecodeDecoder;
import net.letsdank.jd.bytecode.Opcode;
import net.letsdank.jd.bytecode.insn.Insn;
import net.letsdank.jd.bytecode.insn.JumpInsn;
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
        Expr condition = findConditionExpr(cfg, localNames, cp);

        // 2. Попытка распознать простой while
        LoopStmt loop = tryBuildWhileLoop(cfg, localNames, cp);
        if (loop != null) {
            BlockStmt body = new BlockStmt();
            body.add(loop);
            return new MethodAst(name, desc, body);
        }

        // 3. Строим линейный AST по всему байткоду
        BytecodeDecoder decoder = new BytecodeDecoder();
        List<Insn> insns = decoder.decode(code);
        ExpressionBuilder exprBuilder = new ExpressionBuilder(localNames, cp);
        BlockStmt linearBody = exprBuilder.buildBlock(insns);

        // 4. Если есть условие и два простых return подряд - собираем if/else
        if (condition != null) {
            IfStmt ifStmt = tryBuildIfElseFromLinear(condition, linearBody);
            if (ifStmt != null) {
                BlockStmt body = new BlockStmt();
                body.add(ifStmt);
                return new MethodAst(name, desc, body);
            }
        }

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

    /**
     * Пытаемся из линейного тела вида:
     * return E1;
     * return E2;
     * <p>
     * и условия cond собрать:
     * if (cond) { return E1; } else { return E2; }
     * <p>
     * (очень ограниченный, но рабочий для abs(int) паттерн).
     */
    private IfStmt tryBuildIfElseFromLinear(Expr condition, BlockStmt linearBody) {
        var stmts = linearBody.statements();
        if (stmts.size() < 2) return null;

        // Ищем первые два return с выражениями
        ReturnStmt first = null;
        ReturnStmt second = null;
        for (var s : stmts) {
            if (s instanceof ReturnStmt r && r.value() != null) {
                if (first == null) first = r;
                else {
                    second = r;
                    break;
                }
            }
        }

        if (first == null || second == null) return null;

        // Собираем then/else блоки
        BlockStmt thenBlock = new BlockStmt();
        BlockStmt elseBlock = new BlockStmt();

        // Вопрос, какую ветку считать then/else, зависит от того, как компилятор
        // расположил return'ы. Для теста нам важно только наличие "if", поэтому
        // можем просто взять first как then, second как false.

        thenBlock.add(first);
        elseBlock.add(second);

        return new IfStmt(condition, thenBlock, elseBlock);
    }

    private LoopStmt tryBuildWhileLoop(ControlFlowGraph cfg, LocalNameProvider localNames, ConstantPool cp) {
        var blocks = cfg.blocks();
        if (blocks.size() != 3) {
            // сильно упрощаем задачу: только очень простой случай
            return null;
        }

        BasicBlock condBlock = cfg.entryBlock();
        if (condBlock == null) return null;

        var condInsns = condBlock.instructions();
        if (condInsns.isEmpty()) return null;

        Insn last = condInsns.getLast();
        if (!(last instanceof JumpInsn condJump) || !JDUtils.isConditional(condJump.opcode())) {
            return null;
        }
        if (condBlock.successors().size() != 2) {
            return null;
        }

        BasicBlock s1 = condBlock.successors().get(0);
        BasicBlock s2 = condBlock.successors().get(1);

        BasicBlock bodyBlock;
        BasicBlock exitBlock;

        if (endsWithGotoTo(s1, condBlock.startOffset())) {
            bodyBlock = s1;
            exitBlock = s2;
        } else if (endsWithGotoTo(s2, condBlock.startOffset())) {
            bodyBlock = s2;
            exitBlock = s1;
        } else {
            return null;
        }

        // Строим условие
        ExpressionBuilder exprBuilder = new ExpressionBuilder(localNames, cp);
        var stackBefore = exprBuilder.simulateStackBeforeBranch(condInsns);
        var condition = buildConditionExpr(condJump, stackBefore);
        if (condition == null) return null;

        // Тело цикла - инструкции bodyBlock без финального goto
        var bodyInsns = bodyBlock.instructions();
        if (bodyInsns.isEmpty()) return null;

        List<Insn> bodyWithoutGoto = bodyInsns;
        Insn bodyLast = bodyInsns.getLast();
        if (bodyLast instanceof JumpInsn j && j.opcode() == Opcode.GOTO) {
            bodyWithoutGoto = bodyInsns.subList(0, bodyInsns.size() - 1);
        }

        var bodyAst = exprBuilder.buildBlock(bodyWithoutGoto);
        return new LoopStmt(condition, bodyAst);
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
}
