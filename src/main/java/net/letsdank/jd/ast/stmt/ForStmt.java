package net.letsdank.jd.ast.stmt;

import net.letsdank.jd.ast.expr.Expr;

public record ForStmt(Stmt init, Expr condition, Stmt update, BlockStmt body) implements Stmt {
    @Override
    public String toString() {
        return "for( " + init + "; " + condition + "; " + update + ") " + body;
    }
}
