package net.letsdank.jd.model.attribute;

/**
 * Атрибут Exceptions перечисляет проверяемые исключения метода (индексы в constant pool).
 */
public record ExceptionsAttribute(String name, int[]exceptionIndexTable) implements AttributeInfo {
}
