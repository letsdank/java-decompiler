package net.letsdank.jd.ast.stmt;

import net.letsdank.jd.ast.expr.Expr;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @param cases  case value -> block
 * @param defaultBlock  may be null */
public record SwitchStmt(Expr selector, Map<Integer, BlockStmt> cases, BlockStmt defaultBlock) implements Stmt {
    public SwitchStmt(Expr selector, Map<Integer, BlockStmt> cases, BlockStmt defaultBlock) {
        this.selector = selector;
        this.cases = new LinkedHashMap<>(cases);
        this.defaultBlock = defaultBlock;
    }
}
