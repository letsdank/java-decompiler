package net.letsdank.jd.ast.expr;

import org.jetbrains.annotations.NotNull;

/**
 * Method reference выражение.
 *
 * Примеры:
 * - System.out::println
 * - String::length
 * - ArrayList::new
 * - this::toString
 *
 * @param target Цель: объект, класс, или null для статических
 * @param targetClass Имя класса (если target == null)
 * @param methodName Имя метода или "new" для constructor reference
 * @param isStatic true если это статический метод
 */
public record MethodRefExpr(
        Expr target,
        String targetClass,
        String methodName,
        boolean isStatic
) implements Expr {

    @NotNull
    @Override
    public String toString() {
        String prefix;
        if (target != null) {
            prefix = target.toString();
        } else if (targetClass != null) {
            prefix = targetClass;
        } else {
            prefix = "?";
        }

        return prefix + "::" + methodName;
    }
}
