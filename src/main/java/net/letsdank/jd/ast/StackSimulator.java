package net.letsdank.jd.ast;

import net.letsdank.jd.ast.expr.*;
import net.letsdank.jd.bytecode.insn.*;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.cp.CpInfo;
import net.letsdank.jd.model.cp.CpInteger;
import net.letsdank.jd.model.cp.CpString;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Симулятор стека для интерпретации байткода.
 * Отвечает за моделирование JVM-стека без побочных эффектов.
 */
public final class StackSimulator {
    private final LocalNameProvider localNames;
    private final ConstantPool cp;

    public StackSimulator(LocalNameProvider localNames, ConstantPool cp) {
        this.localNames = localNames;
        this.cp = cp;
    }

    /**
     * Симулирует стек до условного перехода (JumpInsn).
     * Используется для извлечения операндов условия.
     */
    public Deque<Expr> simulateUntilBranch(List<Insn> insns) {
        Deque<Expr> stack = new ArrayDeque<>();

        for (Insn insn : insns) {
            if (insn instanceof JumpInsn) {
                break; // не обрабатываем сам Jump
            }

            if (insn instanceof SimpleInsn s) {
                processSimpleInsn(s, stack);
            } else if (insn instanceof LocalVarInsn lv) {
                processLocalVarInsn(lv, stack);
            } else if (insn instanceof IntOperandInsn io) {
                processIntOperandInsn(io, stack);
            } else if (insn instanceof ConstantPoolInsn cpi) {
                processConstantPoolInsn(cpi, stack);
            }
        }

        return stack;
    }

    private void processSimpleInsn(SimpleInsn s, Deque<Expr> stack) {
        switch (s.opcode()) {
            // арифметика
            case IADD -> binaryOp(stack, "+");
            case ISUB -> binaryOp(stack, "-");
            case IMUL -> binaryOp(stack, "*");
            case IDIV -> binaryOp(stack, "/");
            case IREM -> binaryOp(stack, "%");
            case INEG -> binaryOp(stack, "-");

            // побитовые операции
            case IAND -> binaryOp(stack, "&");
            case IOR -> binaryOp(stack, "|");
            case IXOR -> binaryOp(stack, "^");

            // сдвиги
            case ISHL -> binaryOp(stack, "<<");
            case ISHR -> binaryOp(stack, ">>");
            case IUSHR -> binaryOp(stack, ">>>");

            // константы iconst_*
            case ICONST_M1 -> stack.push(new IntConstExpr(-1));
            case ICONST_0 -> stack.push(new IntConstExpr(0));
            case ICONST_1 -> stack.push(new IntConstExpr(1));
            case ICONST_2 -> stack.push(new IntConstExpr(2));
            case ICONST_3 -> stack.push(new IntConstExpr(3));
            case ICONST_4 -> stack.push(new IntConstExpr(4));
            case ICONST_5 -> stack.push(new IntConstExpr(5));

            // load операции
            case ILOAD_0, ALOAD_0 -> stack.push(varExpr(0));
            case ILOAD_1, ALOAD_1 -> stack.push(varExpr(1));
            case ILOAD_2, ALOAD_2 -> stack.push(varExpr(2));
            case ILOAD_3, ALOAD_3 -> stack.push(varExpr(3));

            // длина массива
            case ARRAYLENGTH -> {
                Expr array = stack.pop();
                stack.push(new ArrayLengthExpr(array));
            }

            // стековые операции
            case POP -> {
                if (!stack.isEmpty()) stack.pop();
            }
            case DUP -> {
                if (!stack.isEmpty()) stack.push(stack.peek());
            }

            default -> {
                // остальные игнорируем для симуляции условий
            }
        }
    }

    private void processLocalVarInsn(LocalVarInsn lv, Deque<Expr> stack) {
        switch (lv.opcode()) {
            case ILOAD, ALOAD -> stack.push(varExpr(lv.localIndex()));
            default -> {
                // store операции не влияют на стек в контексте условий
            }
        }
    }

    private void processIntOperandInsn(IntOperandInsn io, Deque<Expr> stack) {
        switch (io.opcode()) {
            case BIPUSH, SIPUSH -> stack.push(new IntConstExpr(io.operand()));
            default -> {
            }
        }
    }

    private void processConstantPoolInsn(ConstantPoolInsn cpi, Deque<Expr> stack) {
        if (cp == null) return;

        switch (cpi.opcode()) {
            case LDC -> {
                CpInfo entry = cp.entry(cpi.cpIndex());
                if (entry instanceof CpInteger i) {
                    stack.push(new IntConstExpr(i.value()));
                } else if (entry instanceof CpString s) {
                    String text = cp.getUtf8(s.stringIndex());
                    stack.push(new StringLiteralExpr(text));
                }
            }
            default -> {
            }
        }
    }

    private void binaryOp(Deque<Expr> stack, String op) {
        Expr right = stack.pop();
        Expr left = stack.pop();
        stack.push(new BinaryExpr(op, left, right));
    }

    private void unaryOp(Deque<Expr> stack, String op) {
        Expr value = stack.pop();
        stack.push(new UnaryExpr(op, value));
    }

    private VarExpr varExpr(int index) {
        return new VarExpr(localNames.nameForLocal(index));
    }
}
