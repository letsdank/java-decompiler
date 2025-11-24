package net.letsdank.jd.ast.expr;

import org.jetbrains.annotations.NotNull;

public record InstanceOfExpr(Expr value, String typeName) implements Expr {
    @NotNull
    @Override
    public String toString() {
        String tn = typeName;
        if (tn.startsWith("java.lang.")) {
            tn = tn.substring("java.lang.".length());
        }

        return value + " instanceof " + tn;
    }
}
