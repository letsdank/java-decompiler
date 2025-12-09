package net.letsdank.jd.ast.stmt;

import org.jetbrains.annotations.NotNull;

public record CommentStmt(String text) implements Stmt {
    @NotNull
    @Override
    public String toString() {
        return text;
    }
}
