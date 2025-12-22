package net.letsdank.jd.ast.stmt;

import org.jetbrains.annotations.NotNull;

/**
 * Узел try/catch(/finally).
 */
public record TryCatchStmt(BlockStmt tryBlock, String exceptionType,
                           String exceptionVarName, BlockStmt catchBlock, BlockStmt finallyBlock) implements Stmt {

    public TryCatchStmt(BlockStmt tryBlock, String exceptionType,
                        String exceptionVarName, BlockStmt catchBlock) {
        this(tryBlock, exceptionType, exceptionVarName, catchBlock, null);
    }

    @NotNull
    @Override
    public String toString() {
        return "try { ... } catch (" + exceptionType + " " + exceptionVarName + ") { ... }";
    }
}
