package net.letsdank.jd.ast.expr;

public record BinaryExpr(String op, Expr left, Expr right) implements Expr {
    @Override
    public String toString() {
        return "(" + left + " " + op + " " + right + ")";
    }
}
