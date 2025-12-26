package net.letsdank.jd.ast;

import net.letsdank.jd.ast.stmt.BlockStmt;

/**
 * Абстрактное синтаксическое дерево для декомпилированного метода.
 *
 * Содержит основную информацию о методе:
 * - {@link #name()} - имя метода (e.g., "processData", "toString")
 * - {@link #descriptor()} - дескриптор метода в байткоде (e.g., "(I)V")
 * - {@link #body()} - тело метода как {@link BlockStmt}
 *
 * <p>Дескриптор метода следует формату JVM:
 * <pre>
 *     (ParameterTypes)ReturnType
 *
 *     Примеры:
 *     - "(I)V"                     -> метод принимает int, возвращает void
 *     - "(Ljava/lang/String;)Z"    -> метод принимает String, возвращает boolean
 *     - "()Ljava/lang/Object;"     -> метод без параметров, возвращает Object
 * </pre>
 * </p>
 *
 * @param name имя метода (никогда null)
 * @param descriptor полный дескриптор метода (может быть null для synthetic методов)
 * @param body блок операторов, составляющих тело метода (никогда null)
 *
 * @see BlockStmt
 */
public record MethodAst(String name, String descriptor, BlockStmt body) {
    @Override
    public String toString() {
        // очень примитивный вид, без типов/модификаторов
        return name + descriptor + " " + body;
    }
}
