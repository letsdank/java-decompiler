package net.letsdank.jd.ast.stmt;

import net.letsdank.jd.ast.expr.Expr;

import java.util.List;

/**
 * Один catch-блок.
 * <p>
 * Пример: catch (IOException e) { ... }
 */
public record CatchClause(String exceptionType, String varName,
                          List<Stmt> body, Expr filterExpr)
        implements Stmt {
    // TODO: exceptionType пока строкой, позже можно завязать на модель типа
}
