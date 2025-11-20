package net.letsdank.jd.ast;

import net.letsdank.jd.ast.expr.*;
import net.letsdank.jd.ast.stmt.AssignStmt;
import net.letsdank.jd.ast.stmt.BlockStmt;
import net.letsdank.jd.ast.stmt.ExprStmt;
import net.letsdank.jd.ast.stmt.ReturnStmt;
import net.letsdank.jd.bytecode.insn.*;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.cp.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Стековый интерпретатор байткода для одного basic block'а.
 * Очень примитивный: только int-операции и возвраты.
 */
public final class ExpressionBuilder {
    private final LocalNameProvider localNames;
    private final ConstantPool cp;

    public ExpressionBuilder(LocalNameProvider localNames, ConstantPool cp) {
        this.localNames = localNames;
        this.cp = cp;
    }

    /**
     * Превращает список инструкций в блок операторов.
     * Для условных блоков можно передать список без последнего JumpInsn.
     */
    public BlockStmt buildBlock(List<Insn> insns) {
        BlockStmt block = new BlockStmt();
        Deque<Expr> stack = new ArrayDeque<>();

        for (Insn insn : insns) {
            if (insn instanceof SimpleInsn s) {
                switch (s.opcode()) {
                    // арифметика
                    case IADD -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr("+", left, right));
                    }
                    case INEG -> {
                        Expr v = stack.pop();
                        stack.push(new UnaryExpr("-", v));
                    }

                    // возвраты
                    case IRETURN -> {
                        Expr v = stack.pop();
                        block.add(new ReturnStmt(v));
                    }
                    case RETURN -> {
                        block.add(new ReturnStmt(null));
                    }

                    // целочисленные константы iconst_*
                    case ICONST_M1 -> stack.push(new IntConstExpr(-1));
                    case ICONST_0 -> stack.push(new IntConstExpr(0));
                    case ICONST_1 -> stack.push(new IntConstExpr(1));
                    case ICONST_2 -> stack.push(new IntConstExpr(2));
                    case ICONST_3 -> stack.push(new IntConstExpr(3));
                    case ICONST_4 -> stack.push(new IntConstExpr(4));
                    case ICONST_5 -> stack.push(new IntConstExpr(5));

                    // локалки без операнда: iload_0..3
                    case ILOAD_0 -> stack.push(varExpr(0));
                    case ILOAD_1 -> stack.push(varExpr(1));
                    case ILOAD_2 -> stack.push(varExpr(2));
                    case ILOAD_3 -> stack.push(varExpr(3));

                    // istore_0..3: присваивание
                    case ISTORE_0 -> {
                        Expr value = stack.pop();
                        block.add(new AssignStmt(varExpr(0), value));
                    }
                    case ISTORE_1 -> {
                        Expr value = stack.pop();
                        block.add(new AssignStmt(varExpr(1), value));
                    }
                    case ISTORE_2 -> {
                        Expr value = stack.pop();
                        block.add(new AssignStmt(varExpr(2), value));
                    }
                    case ISTORE_3 -> {
                        Expr value = stack.pop();
                        block.add(new AssignStmt(varExpr(3), value));
                    }

                    default -> {
                        // игнорируем всякие nop, etc (пока что)
                    }
                }
            } else if (insn instanceof IntOperandInsn io) {
                switch (io.opcode()) {
                    // bipush / sipush -> константные int
                    case BIPUSH, SIPUSH -> stack.push(new IntConstExpr(io.operand()));
                    default -> {
                        // другие инструкции с immediate пока игнорируем
                    }
                }
            } else if (insn instanceof LocalVarInsn lv) {
                switch (lv.opcode()) {
                    // ILOAD с явным индексом
                    case ILOAD -> {
                        int idx = lv.localIndex();
                        stack.push(varExpr(idx));
                    }
                    case ISTORE -> {
                        int idx = lv.localIndex();
                        Expr value = stack.pop();
                        block.add(new AssignStmt(varExpr(idx), value));
                    }
                    default -> {
                        // istore и т.п. пока не обрабатываем
                    }
                }
            } else if (insn instanceof JumpInsn) {
                // предположим, что block для ExpressionBuilder не включает
                // финальный JumpInsn (кроме специального случая cond-блока),
                // поэтому тут можно проигнорировать
            } else if (insn instanceof ConstantPoolInsn cpi) {
                // Если нет cp (теоретически), просто игнорируем
                if (cp == null) continue;

                switch (cpi.opcode()) {
                    case LDC -> {
                        // ldc #idx -> константа из constant pool
                        CpInfo e = cp.entry(cpi.cpIndex());
                        if (e instanceof CpString s) {
                            String text = cp.getUtf8(s.stringIndex());
                            stack.push(new StringLiteralExpr(text));
                        }
                    }
                    case GETSTATIC -> {
                        Expr fieldExpr = makeStaticFieldExpr(cpi.cpIndex());
                        if (fieldExpr != null) {
                            stack.push(fieldExpr);
                        }
                    }
                    case INVOKEVIRTUAL -> {
                        // 1. Определяем количество аргументов и void/non-void
                        String descriptor = resolveMethodDescriptor(cpi.cpIndex());
                        int argCount = argCountFromDescriptor(descriptor);
                        boolean isVoid = descriptor != null && descriptor.endsWith(")V");

                        int needed = argCount + 1; // объект + аргументы
                        if (stack.size() < needed) {
                            // не хватает на стеке - лучше пропустить, чем падать
                            break;
                        }

                        // 2. Снимаем аргументы (в обратном порядке) и target
                        List<Expr> args = new ArrayList<>(argCount);
                        for (int i = 0; i < argCount; i++) {
                            // Последний снятый аргумент - правый, поэтому добавляем в начало
                            args.add(0, stack.pop());
                        }
                        Expr target = stack.pop();

                        String methodName = resolveMethodName(cpi.cpIndex());
                        CallExpr call = new CallExpr(target, methodName, args);

                        if (isVoid) {
                            // println(...) и прочие void -> statement
                            block.add(new ExprStmt(call));
                        } else {
                            // результат используется дальше -> оставим в стеке
                            stack.push(call);
                        }
                    }
                    case INVOKESTATIC -> {
                        // Тут можно в дальнейшем поддерживать статические вызовы вида Foo.bar(x)
                    }
                    default -> {
                        // остальные инструкции с cp пока игнорируем
                    }
                }
            } else if (insn instanceof IincInsn inc) {
                int idx = inc.localIndex();
                int delta = inc.delta();
                // v = v + delta;
                VarExpr v = varExpr(idx);
                Expr rhs;
                if (delta == 1) {
                    // пока вместо v++ выведем v += 1
                    rhs = new BinaryExpr("+", v, new IntConstExpr(1));
                } else {
                    rhs = new BinaryExpr("+", v, new IntConstExpr(delta));
                }
                block.add(new AssignStmt(v, rhs));
            } else if (insn instanceof UnknownInsn) {
                // ничего
            }
        }

        return block;
    }

    private VarExpr varExpr(int index) {
        return new VarExpr(localNames.nameForLocal(index));
    }

    /**
     * Вспомогательный метод: симуляция стека до JumpInsn, чтобы вытащить
     * операнды условия. Нужен для if/if_icmp.
     */
    public Deque<Expr> simulateStackBeforeBranch(List<Insn> insns) {
        Deque<Expr> stack = new ArrayDeque<>();

        for (Insn insn : insns) {
            if (insn instanceof JumpInsn) {
                break; // не обрабатываем сам Jump
            }

            if (insn instanceof SimpleInsn s) {
                switch (s.opcode()) {
                    case IADD -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr("+", left, right));
                    }
                    case INEG -> {
                        Expr v = stack.pop();
                        stack.push(new UnaryExpr("-", v));
                    }

                    // iconst_*
                    case ICONST_M1 -> stack.push(new IntConstExpr(-1));
                    case ICONST_0 -> stack.push(new IntConstExpr(0));
                    case ICONST_1 -> stack.push(new IntConstExpr(1));
                    case ICONST_2 -> stack.push(new IntConstExpr(2));
                    case ICONST_3 -> stack.push(new IntConstExpr(3));
                    case ICONST_4 -> stack.push(new IntConstExpr(4));
                    case ICONST_5 -> stack.push(new IntConstExpr(5));

                    // iload_0..3
                    case ILOAD_0 -> stack.push(varExpr(0));
                    case ILOAD_1 -> stack.push(varExpr(1));
                    case ILOAD_2 -> stack.push(varExpr(2));
                    case ILOAD_3 -> stack.push(varExpr(3));

                    default -> {
                    }
                }
            } else if (insn instanceof LocalVarInsn lv) {
                switch (lv.opcode()) {
                    case ILOAD -> {
                        int idx = lv.localIndex();
                        stack.push(new VarExpr(localNames.nameForLocal(idx)));
                    }
                    default -> {
                    }
                }
            } else if (insn instanceof IntOperandInsn io) {
                switch (io.opcode()) {
                    case BIPUSH, SIPUSH -> stack.push(new IntConstExpr(io.operand()));
                    default -> {
                    }
                }
            }
        }

        return stack;
    }

    private String resolveMethodDescriptor(int cpIndex) {
        if (cp == null) return null;
        CpInfo e = cp.entry(cpIndex);
        if (e instanceof CpMethodref mr) {
            CpNameAndType nt = (CpNameAndType) cp.entry(mr.nameAndTypeIndex());
            return cp.getUtf8(nt.descriptorIndex());
        }
        return null;
    }

    private int argCountFromDescriptor(String descriptor) {
        if (descriptor == null) return 0;
        try {
            var params = DescriptorUtils.parseParameterTypes(descriptor);
            return params.size();
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    private String resolveMethodName(int cpIndex) {
        if (cp == null) return "method";

        CpInfo e = cp.entry(cpIndex);
        if (e instanceof CpMethodref mr) {
            CpNameAndType nt = (CpNameAndType) cp.entry(mr.nameAndTypeIndex());
            return cp.getUtf8(nt.nameIndex());
        }
        return "method";
    }

    private Expr makeStaticFieldExpr(int cpIndex) {
        if (cp == null) return null;
        CpInfo e = cp.entry(cpIndex);
        if (!(e instanceof CpFieldref fr)) {
            return null;
        }

        // owner internal name: java/lang/System
        String ownerInternal = cp.getClassName(fr.classIndex());
        String ownerSimple = simpleClassName(ownerInternal);

        CpNameAndType nt = (CpNameAndType) cp.entry(fr.nameAndTypeIndex());
        String fieldName = cp.getUtf8(nt.nameIndex());

        // System.out
        return new FieldAccessExpr(new VarExpr(ownerSimple), fieldName);
    }

    private String simpleClassName(String internalName) {
        // internalName: java/lang/System
        int slash = internalName.lastIndexOf('/');
        if (slash >= 0 && slash < internalName.length() - 1) {
            return internalName.substring(slash + 1);
        }
        return internalName.replace('/', '.');
    }
}
