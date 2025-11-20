package net.letsdank.jd.ast.stmt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BlockStmt implements Stmt {
    private final List<Stmt>statements=new ArrayList<>();

    public BlockStmt() {}

    public BlockStmt(List<Stmt>stmts){
        statements.addAll(stmts);
    }

    public List<Stmt>statements(){
        return Collections.unmodifiableList(statements);
    }

    public void add(Stmt stmt){
        statements.add(stmt);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{\n");
        for(Stmt s : statements){
            sb.append(" ").append(s).append('\n');
        }
        sb.append("}");
        return sb.toString();
    }
}
