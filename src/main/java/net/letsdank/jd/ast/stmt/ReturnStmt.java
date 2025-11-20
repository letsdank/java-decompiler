package net.letsdank.jd.ast.stmt;

import net.letsdank.jd.ast.expr.Expr;

public record ReturnStmt(Expr value) implements Stmt {
    @Override
    public String toString() {
        return "return " + value + ";";
    }
}
