package net.letsdank.jd.ast.stmt;

public sealed interface Stmt
        permits ReturnStmt, ExprStmt, IfStmt, BlockStmt,
        AssignStmt, LoopStmt, ForStmt {
}
