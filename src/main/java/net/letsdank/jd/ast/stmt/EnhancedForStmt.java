package net.letsdank.jd.ast.stmt;

import net.letsdank.jd.ast.expr.Expr;

/**
 * Узел enhanced for (for-each) цикла: for (Type var : expr) { ... }
 */
public record EnhancedForStmt(String varType, String varName, Expr iterable, BlockStmt body) implements Stmt {
}
