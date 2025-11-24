package net.letsdank.jd.ast.expr;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * new TypeName(arg1, arg2, ...)
 */
public record NewExpr(String typeName, List<Expr> args) implements Expr {
    @NotNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("new ");
        sb.append(typeName).append("(");
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(args.get(i));
        }
        sb.append(")");
        return sb.toString();
    }
}
