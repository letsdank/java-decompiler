package net.letsdank.jd.ast.stmt;

import java.util.List;

/**
 * Узел try/catch(/finally).
 */
public record TryCatchStmt(List<Stmt> tryBlock, List<CatchClause> catches,
                           FinallyClause finallyClause) {
    public boolean hasFinally() {
        return finallyClause != null;
    }
}
