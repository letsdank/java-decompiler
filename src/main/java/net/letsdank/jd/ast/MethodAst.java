package net.letsdank.jd.ast;

import net.letsdank.jd.ast.stmt.BlockStmt;

public record MethodAst(String name, String descriptor, BlockStmt body) {
    @Override
    public String toString() {
        // очень примитивный вид, без типов/модификаторов
        return name + descriptor + " " + body;
    }
}
