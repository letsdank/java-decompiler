package net.letsdank.jd.ast.expr;

import org.jetbrains.annotations.NotNull;

public record CastExpr(String typeName, Expr value) implements Expr {
    @NotNull
    @Override
    public String toString() {
        String tn = typeName;
        if (tn.startsWith("java.lang.")) {
            tn = tn.substring("java.lang.".length());
        }

        return "(" + tn + ") " + value;
    }
}
