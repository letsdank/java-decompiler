package net.letsdank.jd.ast.stmt;

import net.letsdank.jd.ast.expr.Expr;

public record ReturnStmt(Expr value) implements Stmt {
    @Override
    public String toString() {
        // void return
        if (value == null)
            return "return;";

        // обычный return expr;
        return "return " + value + ";";
    }
}
