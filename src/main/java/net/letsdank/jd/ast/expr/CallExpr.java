package net.letsdank.jd.ast.expr;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Вызов метода
 *
 * @param target            Объект, на котором вызывается метод (или null для статических / top-level)
 * @param ownerInternalName Внутреннее имя класса-владельца из constant pool, например "net/letsdank/jd/fixtures/User"
 * @param methodName        Имя метода ("getName", "greet", "copy", ...)
 * @param args              Аргументы вызова
 */
public record CallExpr(
        Expr target,
        String ownerInternalName,
        String methodName,
        List<Expr> args) implements Expr {

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
