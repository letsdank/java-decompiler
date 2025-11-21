package net.letsdank.jd.model;

/**
 * Представление атрибута Code метода.
 * Пока: только "сырые" данные байткода и базовая информация.
 *
 * @param name Всегда "Code"
 * @param maxStack
 * @param maxLocals
 * @param code Байткод
 */
public record CodeAttribute(String name, int maxStack, int maxLocals, byte[] code,
                            LineNumberTableAttribute lineNumberTable,
                            LocalVariableTableAttribute localVariableAttribute) implements AttributeInfo {
}
