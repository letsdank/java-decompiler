package net.letsdank.jd.ast.expr;

public record FieldAccessExpr(Expr target, String fieldName) implements Expr {
    @Override
    public String toString() {
        return target + "." + fieldName;
    }
}
