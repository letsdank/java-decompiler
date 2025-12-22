package net.letsdank.jd.ast.expr;

import org.jetbrains.annotations.NotNull;

public record NullExpr() implements Expr {
    @NotNull
    @Override
    public String toString() {
        return "null";
    }
}
