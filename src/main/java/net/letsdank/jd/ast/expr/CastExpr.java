package net.letsdank.jd.ast.expr;

import org.jetbrains.annotations.NotNull;

public record CastExpr(String typeName, Expr value) implements Expr {
    @NotNull
    @Override
    public String toString() {
        return "(" + typeName + ") " + value;
    }
}
