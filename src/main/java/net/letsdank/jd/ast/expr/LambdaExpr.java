package net.letsdank.jd.ast.expr;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Lambda выражение.
 *
 * Примеры:
 * - () -> 42
 * x -> x * 2
 * (x, y) -> x + y
 * (x, y) -> { return x + y; }
 *
 * @param parameters Имена параметров
 * @param paramTypes Типы параметров (может быть пустым для type inference)
 * @param body Тело lambda (если это выражение)
 * @param isBlock true если это block lambda { ... }
 */
public record LambdaExpr(
        List<String> parameters,
        List<String> paramTypes,
        Expr body,
        boolean isBlock
) implements Expr {

    @NotNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Параметры
        if (parameters.isEmpty()) {
            sb.append("()");
        } else if (parameters.size() == 1 && (paramTypes == null || paramTypes.isEmpty())) {
            // Одиночный параметр без типа: x -> ...
            sb.append(parameters.getFirst());
        } else {
            sb.append("(");
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) sb.append(", ");

                // Тип параметра (если есть)
                if (paramTypes != null && i < paramTypes.size() && paramTypes.get(i) != null) {
                    sb.append(paramTypes.get(i)).append(" ");
                }

                sb.append(parameters.get(i));
            }
            sb.append(")");
        }

        sb.append(" -> ");

        // Тело
        if (isBlock) {
            sb.append("{ /* block */ }");
        } else {
            sb.append(body);
        }

        return sb.toString();
    }
}
