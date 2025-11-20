package net.letsdank.jd.ast.expr;

public record IntConstExpr(int value) implements Expr {
    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
