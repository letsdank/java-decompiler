package net.letsdank.jd.ast.stmt;

import net.letsdank.jd.ast.expr.Expr;

/**
 * Узел synchronized блока.
 */
public record SynchronizedStmt(Expr monitor, BlockStmt body) implements Stmt {
}
