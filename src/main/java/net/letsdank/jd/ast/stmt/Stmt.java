package net.letsdank.jd.ast.stmt;

public sealed interface Stmt
        permits AssignStmt, BlockStmt, CatchClause, ExprStmt,
        FinallyClause, ForStmt, IfStmt, LoopStmt, ReturnStmt,
        TryCatchStmt, CommentStmt {
}
