package net.letsdank.jd.ast.expr;

import org.jetbrains.annotations.NotNull;

public record ArrayAccessExpr(Expr array, Expr index) implements Expr {
    @NotNull
    @Override
    public String toString() {
        return array + "[" + index + "]";
    }
}
