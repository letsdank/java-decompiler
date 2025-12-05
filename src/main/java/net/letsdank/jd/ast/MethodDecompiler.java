package net.letsdank.jd.ast;

import net.letsdank.jd.ast.expr.*;
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
import net.letsdank.jd.model.attribute.AttributeInfo;
import net.letsdank.jd.model.attribute.BootstrapMethodsAttribute;
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
    private final DecompilerOptions options;

    public MethodDecompiler() {
        this(new DecompilerOptions());
    }

    public MethodDecompiler(DecompilerOptions options) {
        this.options = options;
    }

    public MethodAst decompile(MethodInfo method, ClassFile cf) {
        CodeAttribute codeAttr = method.findCodeAttribute();
        var cp = cf.constantPool();
        String name = cp.getUtf8(method.nameIndex());
        String desc = cp.getUtf8(method.descriptorIndex());
        if (codeAttr == null) {
            return new MethodAst(name, desc, new BlockStmt());
        }

        LocalNameProvider baseNames = new MethodLocalNameProvider(method.accessFlags(), desc);
        LocalNameProvider localNames = baseNames;

        // если есть LocalVariableTable - накрываем debug-именами
        if (codeAttr.localVariableAttribute() != null) {
            localNames = new LocalVariableNameProvider(baseNames, codeAttr.localVariableAttribute(), cp);
        }

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

        // Найдем BootstrapMethods на уровне класса (если есть)
        BootstrapMethodsAttribute bootstrap = null;
        for (AttributeInfo attr : cf.attributes()) {
            if (attr instanceof BootstrapMethodsAttribute bmAttr) {
                bootstrap = bmAttr;
                break;
            }
        }

        // Если в методе вообще нет условные/безусловных переходов -
        // его можно честно разобрать линейным стековым интерпретатором.
        if (!hasControlFlow(insns)) {
            ExpressionBuilder exprBuilder = new ExpressionBuilder(localNames, cp, options, bootstrap);
            BlockStmt linearBody = exprBuilder.buildBlock(insns);
            return postProcessLoops(new MethodAst(name, desc, linearBody));
        }

        // Пытаемся распознать простой if (...) { then } в начале метода
        MethodAst simpleIfAst = tryBuildSimpleIfAtEntry(cfg, localNames, cp, options, bootstrap, name, desc);
        if (simpleIfAst != null) {
            return postProcessLoops(simpleIfAst);
        }

        // Пытаемся распознать guard-return по CFG
        MethodAst guardAst = tryBuildGuardReturnAtEntry(cfg, localNames, cp, options, bootstrap, name, desc);
        if (guardAst != null) {
            return postProcessLoops(guardAst);
        }

        // ИНАЧЕ: сложный control flow - пока не пытаемся притворяться умными.
        BlockStmt placeholder = new BlockStmt();
        placeholder.add(new ExprStmt(
                new VarExpr("// TODO: complex control flow (experimental decompiler skipped)")
        ));
        return new MethodAst(name, desc, placeholder);
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
        BasicBlock entry = cfg.entryBlock();

        for (BasicBlock condBlock : cfg.blocks()) {
            var insns = condBlock.instructions();
            if (insns.isEmpty()) continue;

            Insn last = insns.getLast();
            if (!(last instanceof JumpInsn j) || !JDUtils.isConditional(j.opcode())) continue;
            if (condBlock.successors().size() != 2) continue;

            // if по CFG распознаем только если это условие в самом начале метода
            if (condBlock != entry) continue;

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

            ExpressionBuilder exprBuilder = new ExpressionBuilder(localNames, cp, options);
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
            ExpressionBuilder exprBuilder = new ExpressionBuilder(localNames, cp, options);
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

    private MethodAst tryBuildSimpleIfAtEntry(ControlFlowGraph cfg, LocalNameProvider localNames,
                                              ConstantPool cp, DecompilerOptions options,
                                              BootstrapMethodsAttribute boostrap, String name, String desc) {
        List<BasicBlock> blocks = cfg.blocks();
        if (blocks.isEmpty()) return null;

        BasicBlock entry = cfg.entryBlock();
        if (entry == null) return null;

        int entryIndex = blocks.indexOf(entry);
        if (entryIndex < 0) return null;

        var entryInsns = entry.instructions();
        if (entryInsns.isEmpty()) return null;

        Insn last = entryInsns.getLast();
        if (!(last instanceof JumpInsn j) || !JDUtils.isConditional(j.opcode())) {
            return null;
        }

        if (entry.successors().size() != 2) {
            return null;
        }

        BasicBlock s0 = entry.successors().get(0);
        BasicBlock s1 = entry.successors().get(1);

        // Определяем thenBlock и joinBlock так, чтобы thenBlock был именной веткой перехода
        BasicBlock thenBlock;
        BasicBlock joinBlock;

        if (j.targetOffset() == s0.startOffset()) {
            thenBlock = s0;
            joinBlock = s1;
        } else if (j.targetOffset() == s1.startOffset()) {
            thenBlock = s1;
            joinBlock = s0;
        } else {
            // target не совпадает ни с одним successor'ом - странный CFG
            return null;
        }

        // Не лезем в guard-return: thenBlock не должен заканчиваться return
        if (endsWithReturn(thenBlock)) {
            return null;
        }

        // Требуем простой линейной формы:
        // entry -> thenBlock -> joinBlock, без дополнительных веток.
        int thenIndex = blocks.indexOf(thenBlock);
        int joinIndex = blocks.indexOf(joinBlock);
        if (thenIndex < 0 || joinIndex < 0) return null;

        // Хотим порядок entry, then, join как три первых блока
        if (!(entryIndex == 0 && thenIndex == 1 && joinIndex == 2)) {
            return null;
        }

        // Больше трех блоков в CFG пока не поддерживаем в этом паттерне
        if (blocks.size() > 3) {
            return null;
        }

        // thenBlock и joinBlock не должны иметь своих jumps (только fallthrough или return)
        if (containsJump(thenBlock)) {
            return null;
        }
        if (containsJump(joinBlock)) {
            // Разрешим только return в конце joinBlock (он не JumpInsn, а SimpleInsn)
            // поэтому containsJump == true значит, что блок кончается условным/безусловным переходом.
            return null;
        }

        ExpressionBuilder exprBuilder = new ExpressionBuilder(localNames, cp, options, boostrap);
        BlockStmt body = new BlockStmt();

        // 1. Префикс entry-блока до JumpInsn -> обычный линейный код
        if (entryInsns.size() > 1) {
            List<Insn> prefixInsns = entryInsns.subList(0, entryInsns.size() - 1);
            if (!prefixInsns.isEmpty()) {
                BlockStmt prefixAst = exprBuilder.buildBlock(prefixInsns);
                prefixAst.statements().forEach(body::add);
            }
        }

        // 2. Условие: ветка перехода ведет в thenBlock
        Deque<Expr> stackBefore = exprBuilder.simulateStackBeforeBranch(entryInsns);
        Expr condExpr = buildConditionExpr(j, stackBefore);
        if (condExpr == null) {
            return null;
        }

        // 3. Тело then-блока
        BlockStmt thenAst = exprBuilder.buildBlock(thenBlock.instructions());
        IfStmt ifStmt = new IfStmt(condExpr, thenAst, null);
        body.add(ifStmt);

        // 4. Остальной код: joinBlock как хвост
        BlockStmt tailAst = exprBuilder.buildBlock(joinBlock.instructions());
        tailAst.statements().forEach(body::add);

        return new MethodAst(name, desc, body);
    }

    private MethodAst tryBuildGuardReturnAtEntry(ControlFlowGraph cfg, LocalNameProvider localNames,
                                                 ConstantPool cp, DecompilerOptions options,
                                                 BootstrapMethodsAttribute bootstrap, String name, String desc) {
        List<BasicBlock> blocks = cfg.blocks();
        if (blocks.isEmpty()) return null;

        BasicBlock entry = cfg.entryBlock();
        if (entry == null) return null;

        int entryIndex = blocks.indexOf(entry);
        if (entryIndex < 0) return null;

        ExpressionBuilder exprBuilder = new ExpressionBuilder(localNames, cp, options, bootstrap);
        BlockStmt body = new BlockStmt();

        int i = entryIndex;
        boolean anyGuard = false;

        // Идем по блокам, начиная с entry, и пытаемся собрать:
        // if (cond1) return ...;
        // if (cond2) return ...;
        // ...
        while (i < blocks.size()) {
            BasicBlock bb = blocks.get(i);
            var insns = bb.instructions();
            if (insns.isEmpty()) break;

            Insn last = insns.getLast();
            if (!(last instanceof JumpInsn j) || !JDUtils.isConditional(j.opcode())) {
                // этот блок больше не guard, отсюда начинается "основное" тело
                return null;
            }

            if (bb.successors().size() != 2) {
                return null;
            }

            BasicBlock s0 = bb.successors().get(0);
            BasicBlock s1 = bb.successors().get(1);

            // Определяем, какой successor - прыжок, а какой - fallthrough
            BasicBlock retBlock = null;
            BasicBlock contBlock = null;

            if (endsWithReturn(s0)) {
                retBlock = s0;
                contBlock = s1;
            } else if (endsWithReturn(s1)) {
                retBlock = s1;
                contBlock = s0;
            } else {
                // странный CFG, не наш случай
                break;
            }

            // прыжок должен вести именно в retBlock (guard = branch)
            if (j.targetOffset() != retBlock.startOffset()) {
                break;
            }

            // требуем линейной формы: contBlock должен быть следующим блоком в списке
            int nextIndex = i + 1;
            if (nextIndex >= blocks.size() || blocks.get(nextIndex) != contBlock) {
                // CFG уже нетривиальная (ветки переплетены) - пока не лезем
                break;
            }

            // 1. Префикс entry-блока: все инструкции до JumpInsn - это просто линейный код
            if (insns.size() > 1) {
                List<Insn> prefixInsns = insns.subList(0, insns.size() - 1);
                if (!prefixInsns.isEmpty()) {
                    BlockStmt prefixAst = exprBuilder.buildBlock(prefixInsns);
                    prefixAst.statements().forEach(body::add);
                }
            }

            // 2. Условие: нужно получить его так, чтобы оно соответствовало ветке,
            //    которая ведет в guardBlock.
            Deque<Expr> stackBefore = exprBuilder.simulateStackBeforeBranch(insns);
            Expr condExpr = buildConditionExpr(j, stackBefore);
            if (condExpr == null) {
                break;
            }

            // 3. AST guard-блока: собираем его как обычный block и забираем ReturnStmt
            BlockStmt retAst = exprBuilder.buildBlock(retBlock.instructions());
            ReturnStmt retStmt = extractLastReturn(retAst);
            if (retStmt == null) {
                return null;
            }

            BlockStmt thenBlock = new BlockStmt();
            thenBlock.add(retStmt);
            body.add(new IfStmt(condExpr, thenBlock, null));

            anyGuard = true;

            // Переходим к следующему блоку (contBlock), который может быть
            // либо новым guard'ом, либо уже "основным" кодом
            i = nextIndex;
        }

        if (!anyGuard) {
            // Даже один guard не собрали - не наш паттерн
            return null;
        }

        // 4. Основной код: просто линейно расписываем contBlock
        // Сейчас поддерживаем только полностью линейный хвост без новых JumpInsn.
        for (int k = i; k < blocks.size(); k++) {
            BasicBlock bb = blocks.get(k);
            if (containsJump(bb)) {
                // дальше начинается сложный control flow - лучше честно откатиться
                return null;
            }
            BlockStmt tailAst = exprBuilder.buildBlock(bb.instructions());
            tailAst.statements().forEach(body::add);
        }

        return new MethodAst(name, desc, body);
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
            return switch (s.opcode()) {
                case IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, RETURN -> true;
                default -> false;
            };
        }
        return false;
    }

    private Stmt tryCombineIfReturnAndNextReturn(IfStmt ifs, Stmt next) {
        // интересует только if без else
        BlockStmt elseBlock = ifs.elseBlock();
        if (elseBlock != null && !elseBlock.statements().isEmpty()) {
            return null;
        }

        List<Stmt> thenStmts = ifs.thenBlock().statements();
        if (thenStmts.size() != 1) return null;
        if (!(thenStmts.getFirst() instanceof ReturnStmt thenRet)) return null;
        if (!(next instanceof ReturnStmt elseRet)) return null;

        Expr thenValue = thenRet.value();
        Expr elseValue = elseRet.value();

        Expr ternary = new TernaryExpr(ifs.condition(), thenValue, elseValue);
        return new ReturnStmt(ternary);
    }

    private Stmt trySimplifyBooleanIfReturn(IfStmt ifs) {
        BlockStmt thenBlock = ifs.thenBlock();
        BlockStmt elseBlock = ifs.elseBlock();
        if (elseBlock == null) return null;

        List<Stmt> thenStmts = thenBlock.statements();
        List<Stmt> elseStmts = elseBlock.statements();
        if (thenStmts.size() != 1 || elseStmts.size() != 1) return null;

        if (!(thenStmts.getFirst() instanceof ReturnStmt thenRet)) return null;
        if (!(elseStmts.getFirst() instanceof ReturnStmt elseRet)) return null;

        if (!(thenRet.value() instanceof IntConstExpr tval)) return null;
        if (!(elseRet.value() instanceof IntConstExpr eval)) return null;

        int tv = tval.value();
        int ev = eval.value();

        if (tv == 1 && ev == 0) {
            // if (cond) return 1; else return 0;  -> return cond;
            return new ReturnStmt(ifs.condition());
        } else if (tv == 0 && ev == 1) {
            // if (cond) return 0; else return 1;  -> return !cond;
            return new ReturnStmt(new UnaryExpr("!", ifs.condition()));
        }

        return null;
    }

    private Stmt trySimplifyIfAssign(IfStmt ifs) {
        BlockStmt thenBlock = ifs.thenBlock();
        BlockStmt elseBlock = ifs.elseBlock();
        if (elseBlock == null) return null;

        List<Stmt> thenStmts = thenBlock.statements();
        List<Stmt> elseStmts = elseBlock.statements();
        if (thenStmts.size() != 1 || elseStmts.size() != 1) return null;

        if (!(thenStmts.getFirst() instanceof AssignStmt thenAs)) return null;
        if (!(elseStmts.getFirst() instanceof AssignStmt elseAs)) return null;

        Expr tTarget = thenAs.target();
        Expr eTarget = elseAs.target();

        // требования: присваиваем в одну и ту же "локацию" (переменную, поле и т.п.)
        if (!tTarget.equals(eTarget)) {
            return null;
        }

        Expr cond = ifs.condition();
        Expr thenVal = thenAs.value();
        Expr elseVal = elseAs.value();

        Expr ternary = new TernaryExpr(cond, thenVal, elseVal);
        return new AssignStmt(tTarget, ternary);
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
        boolean returnsBoolean = ast.descriptor() != null && ast.descriptor().endsWith(")Z");
        BlockStmt transformed = transformBlock(body, returnsBoolean);
        if (transformed == body) return ast;
        return new MethodAst(ast.name(), ast.descriptor(), transformed);
    }

    private BlockStmt transformBlock(BlockStmt block, boolean returnsBoolean) {
        var src = block.statements();
        List<Stmt> result = new ArrayList<>(src.size());

        for (int i = 0; i < src.size(); i++) {
            Stmt s = src.get(i);

            if (s instanceof LoopStmt loop) {
                result.add(transformLoop(loop, returnsBoolean));
            } else if (s instanceof IfStmt ifs) {
                // сначала рекурсивно обрабатываем then/else
                BlockStmt thenT = transformBlock(ifs.thenBlock(), returnsBoolean);
                BlockStmt elseT = ifs.elseBlock() != null
                        ? transformBlock(ifs.elseBlock(), returnsBoolean)
                        : null;

                IfStmt normalized = new IfStmt(ifs.condition(), thenT, elseT);

                // 1. if (...) return ...; else return ...; -> return cond ? ... : ...;
                if (i + 1 < src.size()) {
                    Stmt next = src.get(i + 1);
                    Stmt combined = tryCombineIfReturnAndNextReturn(normalized, next);
                    if (combined != null) {
                        result.add(combined);
                        i++; // съедаем следующий stmt
                        continue;
                    }
                }

                // 2. boolean-паттерны: if (cond) return 1; else return 0/1;
                if (returnsBoolean) {
                    Stmt boolSimplified = trySimplifyBooleanIfReturn(normalized);
                    if (boolSimplified != null) {
                        result.add(boolSimplified);
                        continue;
                    }
                }

                // 3. if (cond) x = 1; else x = b; -> x = cond ? a : b;
                Stmt assignSimplified = trySimplifyIfAssign(normalized);
                if (assignSimplified != null) {
                    result.add(assignSimplified);
                    continue;
                }

                result.add(normalized);
            } else {
                result.add(s);
            }
        }

        return new BlockStmt(result);
    }

    private Stmt transformLoop(LoopStmt loop, boolean returnsBoolean) {
        // Сначала прогоняем тело цикла через transformBlock рекурсивно,
        // чтобы нормализовать вложенные if/loops.
        BlockStmt originalBody = loop.body();
        BlockStmt normalizedBody = transformBlock(originalBody, returnsBoolean);

        var stmts = normalizedBody.statements();
        if (!stmts.isEmpty()) {
            Stmt last = stmts.getLast();
            AssignStmt update = extractSimpleUpdate(last);
            if (update != null) {
                // Нашли паттерн "i = i + N" в конце тела цикла -> можно собрать for
                var bodyWithoutUpdate = new ArrayList<Stmt>(stmts.size() - 1);
                bodyWithoutUpdate.addAll(stmts.subList(0, stmts.size() - 1));
                BlockStmt forBody = new BlockStmt(bodyWithoutUpdate);

                // Пока init не выделяем, используем только condition и update.
                return new ForStmt(null, loop.condition(), update, forBody);
            }
        }

        // Если тело не изменилось и паттерн for не нашли, просто возвращаем исходный цикл
        if (normalizedBody.equals(originalBody)) return loop;

        // Иначе хотя бы обновим тело while на нормализованную версию
        return new LoopStmt(loop.condition(), normalizedBody);
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

    private boolean hasControlFlow(List<Insn> insns) {
        for (Insn i : insns) {
            if (i instanceof JumpInsn) return true;
            // сюда же можно будет добавить TABLESWITCH/LOOKUPSWITCH, если они не как JumpInsn
        }
        return false;
    }

    // Проверка, есть ли Jump в блоке
    private boolean containsJump(BasicBlock bb) {
        var insns = bb.instructions();
        if (insns.isEmpty()) return false;
        Insn last = insns.getLast();
        return last instanceof JumpInsn;
    }

    // Достаем последний ReturnStmt из блока
    private ReturnStmt extractLastReturn(BlockStmt block) {
        var stmts = block.statements();
        if (stmts.isEmpty()) return null;
        if (stmts.getLast() instanceof ReturnStmt rs) return rs;
        return null;
    }
}
