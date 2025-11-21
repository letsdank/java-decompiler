package net.letsdank.jd.ast.stmt;

import net.letsdank.jd.ast.expr.Expr;

public record AssignStmt(Expr target, Expr value) implements Stmt {
}
