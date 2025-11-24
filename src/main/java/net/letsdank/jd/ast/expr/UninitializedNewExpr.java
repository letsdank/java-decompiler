package net.letsdank.jd.ast.expr;

import org.jetbrains.annotations.NotNull;

/**
 * Внутренний маркер для результата NEW до вызова конструктора.
 * В финальный вывод попадать не должен.
 */
public record UninitializedNewExpr(String internalName) implements Expr {
    @NotNull
    @Override
    public String toString() {
        // если внезапно дойдет до pretty-printer'а - хотя бы не сломается
        return "/*new " + internalName + "*/";
    }
}
