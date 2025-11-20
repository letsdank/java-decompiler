package net.letsdank.jd.ast.expr;

public record UnaryExpr(String op, Expr expr) implements Expr {
    @Override
    public String toString() {
        return op + expr;
    }
}
