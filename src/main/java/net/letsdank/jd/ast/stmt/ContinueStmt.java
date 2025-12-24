package net.letsdank.jd.ast.stmt;

import org.jetbrains.annotations.NotNull;

/**
 * Continue statement для перехода к следующей итерации цикла.
 */
public record ContinueStmt(String label) implements Stmt {

    /**
     * Continue без метки.
     */
    public ContinueStmt() {
        this(null);
    }

    @NotNull
    @Override
    public String toString() {
        return label != null ? "continue " + label + ";" : "continue;";
    }
}
