package net.letsdank.jd.ast.stmt;

public sealed interface Stmt
        permits AssignStmt, BlockStmt, BreakStmt, CatchClause,
        ContinueStmt, ExprStmt, FinallyClause, ForStmt, IfStmt,
        LoopStmt, ReturnStmt, TryCatchStmt, CommentStmt, SwitchStmt,
        EnhancedForStmt, SynchronizedStmt {
}
