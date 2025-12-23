package net.letsdank.jd.ast;

import net.letsdank.jd.ast.expr.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Простые упрощения выражений: константная свертка и алгебраические тождества.
 */
public final class ExpressionSimplifier {

    public Expr simplify(Expr expr) {
        if (expr == null) return null;

        if (expr instanceof IntConstExpr ic) {
            return ic;
        }

        if (expr instanceof StringLiteralExpr sc) {
            return sc;
        }

        if (expr instanceof VarExpr v) {
            return v;
        }

        if (expr instanceof UnaryExpr ue) {
            Expr inner = simplify(ue.expr());
            if ("-".equals(ue.op()) && inner instanceof IntConstExpr ic) {
                return new IntConstExpr(-ic.value());
            }
            // двойное отрицание: -(-x) -> x
            if ("-".equals(ue.op()) && inner instanceof UnaryExpr innerU && "-".equals(innerU.op())) {
                return simplify(innerU.expr());
            }
            return new UnaryExpr(ue.op(), inner);
        }
        if (expr instanceof BinaryExpr be) {
            Expr left = simplify(be.left());
            Expr right = simplify(be.right());
            String op = be.op();

            // String конкатенация: "a" + "b" -> "ab"
            if ("+".equals(op) && left instanceof StringLiteralExpr ls && right instanceof StringLiteralExpr rs) {
                return new StringLiteralExpr(ls.value() + rs.value());
            }

            // Константная свертка для int
            if (left instanceof IntConstExpr li && right instanceof IntConstExpr ri) {
                int lv = li.value();
                int rv = ri.value();
                return switch (op) {
                    case "+" -> new IntConstExpr(lv + rv);
                    case "-" -> new IntConstExpr(lv - rv);
                    case "*" -> new IntConstExpr(lv * rv);
                    case "/" -> rv != 0 ? new IntConstExpr(lv / rv) : new BinaryExpr(op, left, right);
                    case "%" -> rv != 0 ? new IntConstExpr(lv % rv) : new BinaryExpr(op, left, right);
                    case "&" -> new IntConstExpr(lv & rv);
                    case "|" -> new IntConstExpr(lv | rv);
                    case "^" -> new IntConstExpr(lv ^ rv);
                    case "<<" -> new IntConstExpr(lv << rv);
                    case ">>" -> new IntConstExpr(lv >> rv);
                    case ">>>" -> new IntConstExpr(lv >>> rv);
                    default -> new BinaryExpr(op, left, right);
                };
            }

            // Алгебраические тождества
            if (right instanceof IntConstExpr rc) {
                int rv = rc.value();
                switch (op) {
                    case "+":
                        if (rv == 0) return left; // x + 0 -> x
                        break;
                    case "-":
                        if (rv == 0) return left; // x - 0 -> x
                    case "*":
                        if (rv == 1) return left; // x * 1 -> x
                        if (rv == 0) return new IntConstExpr(0); // x * 0 -> 0
                        break;
                    case "/":
                        if (rv == 1) return left; // x / 1 -> x
                        break;
                    case "&":
                        if (rv == 0) return new IntConstExpr(0); // x & 0 -> 0
                        break;
                    case "|":
                        if (rv == 0) return left; // x | 0 -> x
                        break;
                    case "^":
                        if (rv == 0) return left; // x ^ 0 -> x
                        break;
                    case "<<":
                    case ">>":
                    case ">>>":
                        if (rv == 0) return left; // x << 0 -> x
                        break;
                }
            }

            if (left instanceof IntConstExpr lc) {
                int lv = lc.value();
                switch (op) {
                    case "+":
                        if (lv == 0) return right; // 0 + x -> x
                        break;
                    case "*":
                        if (lv == 1) return right;
                        if (lv == 0) return new IntConstExpr(0); // 0 * x -> 0
                        break;
                    case "|":
                        if (lv == 0) return right; // 0 | x -> x
                        break;
                    case "^":
                        if (lv == 0) return right; // 0 ^ x -> x
                        break;
                }
            }

            return new BinaryExpr(op, left, right);
        }

        // Остальные типы выражений упрощаем рекурсивно, если у них есть дочерние узлы
        if (expr instanceof FieldAccessExpr fa) {
            Expr obj = simplify(fa.target());
            return new FieldAccessExpr(obj, fa.fieldName());
        }
        if (expr instanceof ArrayAccessExpr aa) {
            Expr arr = simplify(aa.array());
            Expr idx = simplify(aa.index());
            return new ArrayAccessExpr(arr, idx);
        }
        if (expr instanceof ArrayLengthExpr al) {
            return new ArrayLengthExpr(simplify(al.array()));
        }
        if (expr instanceof CastExpr ce) {
            return new CastExpr(ce.typeName(), simplify(ce.value()));
        }
        if (expr instanceof InstanceOfExpr io) {
            return new InstanceOfExpr(simplify(io.value()), io.typeName());
        }
        if (expr instanceof NewExpr ne) {
            // аргументы конструктора
            List<Expr> args = new ArrayList<>(ne.args().size());
            for (Expr a : ne.args()) args.add(simplify(a));
            return new NewExpr(ne.typeName(), List.copyOf(args));
        }
        if (expr instanceof CallExpr call) {
            List<Expr> args = new ArrayList<>(call.args().size());
            for (Expr a : call.args()) args.add(simplify(a));
            Expr target = simplify(call.target());
            return new CallExpr(target, call.ownerInternalName(), call.methodName(), List.copyOf(args));
        }
        if (expr instanceof TernaryExpr te) {
            Expr c = simplify(te.condition());
            Expr t = simplify(te.thenExpr());
            Expr e = simplify(te.elseExpr());
            return new TernaryExpr(c, t, e);
        }

        return expr; // по умолчанию: без изменений
    }
}
