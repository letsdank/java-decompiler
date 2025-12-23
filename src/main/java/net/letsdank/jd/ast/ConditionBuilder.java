package net.letsdank.jd.ast;

import net.letsdank.jd.ast.expr.BinaryExpr;
import net.letsdank.jd.ast.expr.Expr;
import net.letsdank.jd.ast.expr.IntConstExpr;
import net.letsdank.jd.ast.expr.NullExpr;
import net.letsdank.jd.bytecode.Opcode;
import net.letsdank.jd.bytecode.insn.JumpInsn;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Построитель условных выражений из JVM условных переходов.
 * Преобразует IF-инструкции в AST-выражения сравнений.
 */
public final class ConditionBuilder {

    /**
     * Строит выражение условия для fallthrough-ветки.
     * Инвертирует условие перехода, чтобы получить условие для "then"-блока.
     */
    public Expr buildForFallthrough(JumpInsn jump, Deque<Expr> stackBefore) {
        Expr branchCondition = buildForJump(jump, stackBefore);
        if (branchCondition == null) return null;
        return invertCondition(branchCondition);
    }

    /**
     * Строит выражение условия для jump-ветки (когда условие истинно).
     */
    public Expr buildForJump(JumpInsn jump, Deque<Expr> stackBefore) {
        Opcode op = jump.opcode();
        Deque<Expr> stack = new ArrayDeque<>(stackBefore);

        return switch (op) {
            // унарные сравнения с нулем
            case IFEQ -> comparison("==", stack.pop(), new IntConstExpr(0));
            case IFNE -> comparison("!=", stack.pop(), new IntConstExpr(0));
            case IFLT -> comparison("<", stack.pop(), new IntConstExpr(0));
            case IFGE -> comparison(">=", stack.pop(), new IntConstExpr(0));
            case IFGT -> comparison(">", stack.pop(), new IntConstExpr(0));
            case IFLE -> comparison("<=", stack.pop(), new IntConstExpr(0));

            // бинарные int-сравнения
            case IF_ICMPEQ -> {
                Expr right = stack.pop();
                Expr left = stack.pop();
                yield comparison("==", left, right);
            }
            case IF_ICMPNE -> {
                Expr right = stack.pop();
                Expr left = stack.pop();
                yield comparison("!=", left, right);
            }
            case IF_ICMPLT -> {
                Expr right = stack.pop();
                Expr left = stack.pop();
                yield comparison("<", left, right);
            }
            case IF_ICMPGE -> {
                Expr right = stack.pop();
                Expr left = stack.pop();
                yield comparison(">=", left, right);
            }
            case IF_ICMPGT -> {
                Expr right = stack.pop();
                Expr left = stack.pop();
                yield comparison(">", left, right);
            }
            case IF_ICMPLE -> {
                Expr right = stack.pop();
                Expr left = stack.pop();
                yield comparison("<=", left, right);
            }

            // ссылочные сравнения
            case IF_ACMPEQ -> {
                Expr right = stack.pop();
                Expr left = stack.pop();
                yield comparison("==", left, right);
            }
            case IF_ACMPNE -> {
                Expr right = stack.pop();
                Expr left = stack.pop();
                yield comparison("!=", left, right);
            }

            // null проверки
            case IFNULL -> comparison("==", stack.pop(), new NullExpr());
            case IFNONNULL -> comparison("!=", stack.pop(), new NullExpr());

            default -> null;
        };
    }

    /**
     * Инвертирует условие: == -> !=, < -> >=, и т.д.
     */
    private Expr invertCondition(Expr condition) {
        if (!(condition instanceof BinaryExpr be)) {
            return condition; // не можем инвертировать
        }

        String invertedOp = switch (be.op()) {
            case "==" -> "!=";
            case "!=" -> "==";
            case "<" -> ">=";
            case ">=" -> "<";
            case ">" -> "<=";
            case "<=" -> ">";
            default -> null;
        };

        if (invertedOp == null) {
            return condition; // не поддерживаем инверсию для этого оператора
        }

        return new BinaryExpr(invertedOp, be.left(), be.right());
    }

    private Expr comparison(String op, Expr left, Expr right) {
        return new BinaryExpr(op, left, right);
    }
}
