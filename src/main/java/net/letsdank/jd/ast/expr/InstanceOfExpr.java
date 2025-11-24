package net.letsdank.jd.ast.expr;

import org.jetbrains.annotations.NotNull;

public record InstanceOfExpr(Expr value, String typeName) implements Expr {
    @NotNull
    @Override
    public String toString() {
        return value + " instanceof " + typeName;
    }
}
