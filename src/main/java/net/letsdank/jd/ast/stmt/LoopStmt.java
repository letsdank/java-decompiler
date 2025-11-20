package net.letsdank.jd.ast.stmt;

import net.letsdank.jd.ast.expr.Expr;

public record LoopStmt(Expr condition, BlockStmt body) implements Stmt {
    @Override
    public String toString() {
        return "while (" + condition + ") " + body;
    }
}
