package net.letsdank.jd.ast.expr;

import org.jetbrains.annotations.NotNull;

public record ArrayLengthExpr(Expr array) implements Expr {
    @NotNull
    @Override
    public String toString() {
        return array + ".length";
    }
}
