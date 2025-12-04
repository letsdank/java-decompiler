package net.letsdank.jd.ast.stmt;

import java.util.List;

/**
 * finally-блок.
 */
public record FinallyClause(List<Stmt> body) implements Stmt {
}
