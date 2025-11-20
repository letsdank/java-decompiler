package net.letsdank.jd.ast.stmt;

import net.letsdank.jd.ast.expr.Expr;
import net.letsdank.jd.ast.expr.VarExpr;

public record AssignStmt(VarExpr target, Expr value) implements Stmt {
}
