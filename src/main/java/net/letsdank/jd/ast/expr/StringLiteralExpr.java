package net.letsdank.jd.ast.expr;

public record StringLiteralExpr(String value) implements Expr {
    @Override
    public String toString() {
        // Минимальный экранинг кавычек
        String escaped = value.replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }
}
