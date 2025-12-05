package net.letsdank.jd.ast.stmt;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Узел try/catch(/finally).
 */
public record TryCatchStmt(BlockStmt tryBlock, String exceptionType,
                           String exceptionVarName, BlockStmt catchBlock) implements Stmt {

    @NotNull
    @Override
    public String toString() {
        return "try { ... } catch (" + exceptionType + " " + exceptionVarName + ") { ... }";
    }
}
