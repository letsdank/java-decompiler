package net.letsdank.jd.ast.expr;

import java.util.List;
import java.util.stream.Collectors;

public record CallExpr(Expr target, String methodName, List<Expr> args) implements Expr {
    @Override
    public String toString() {
        String argsStr = args.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        if (target != null) {
            return target + "." + methodName + "(" + argsStr + ")";
        } else {
            return methodName + "(" + argsStr + ")";
        }
    }
}
