package net.letsdank.jd.ast.expr;

import org.jetbrains.annotations.NotNull;

public record NewArrayExpr(String elementType, Expr size) implements Expr {
    @NotNull
    @Override
    public String toString() {
        return "new " + elementType + "[" + size + "]";
    }
}
