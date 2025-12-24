package net.letsdank.jd.ast.stmt;

import org.jetbrains.annotations.NotNull;

/**
 * Break statement для выхода из loop/switch.
 */
public record BreakStmt(String label) implements Stmt {

    /**
     * Break без метки.
     */
    public BreakStmt() {
        this(null);
    }

    @NotNull
    @Override
    public String toString() {
        return label != null ? "break " + label + ";" : "break;";
    }
}
