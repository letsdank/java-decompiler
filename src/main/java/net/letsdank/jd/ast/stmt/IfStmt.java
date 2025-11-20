package net.letsdank.jd.ast.stmt;

import net.letsdank.jd.ast.expr.Expr;

public record IfStmt(Expr condition, BlockStmt thenBlock, BlockStmt elseBlock) implements Stmt {
    @Override
    public String toString() {
        StringBuilder sb=  new StringBuilder();
        sb.append("if ").append(condition).append(" ").append(thenBlock);
        if(elseBlock!=null&&!elseBlock.statements().isEmpty()){
            sb.append(" else ").append(elseBlock);
        }
        return sb.toString();
    }
}
