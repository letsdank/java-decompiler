package net.letsdank.jd.ast;

import net.letsdank.jd.ast.expr.*;
import net.letsdank.jd.ast.stmt.AssignStmt;
import net.letsdank.jd.ast.stmt.BlockStmt;
import net.letsdank.jd.ast.stmt.ReturnStmt;
import net.letsdank.jd.bytecode.insn.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Стековый интерпретатор байткода для одного basic block'а.
 * Очень примитивный: только int-операции и возвраты.
 */
public final class ExpressionBuilder {
    private final LocalNameProvider localNames;

    public ExpressionBuilder(LocalNameProvider localNames) {
        this.localNames = localNames;
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
            } else if (insn instanceof ConstantPoolInsn) {
                // вызовы методов, getstatic и т.п. пока не превращаем в Expr
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
}
