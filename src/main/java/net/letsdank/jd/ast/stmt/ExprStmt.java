package net.letsdank.jd.ast.stmt;

import net.letsdank.jd.ast.expr.Expr;

public record ExprStmt(Expr expr) implements Stmt {
    @Override
    public String toString() {
        return expr + ";";
    }
}
