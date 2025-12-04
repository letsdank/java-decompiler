package net.letsdank.jd.ast.expr;

import org.jetbrains.annotations.NotNull;

public record TernaryExpr(Expr condition, Expr thenExpr, Expr elseExpr) implements Expr {
    @NotNull
    @Override
    public String toString() {
        return "(" + condition + " ? " + thenExpr + " : " + elseExpr + ")";
    }
}
