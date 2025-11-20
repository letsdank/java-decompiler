package net.letsdank.jd.ast.expr;

public record VarExpr(String name) implements Expr {
    @Override
    public String toString() {
        return name;
    }
}
