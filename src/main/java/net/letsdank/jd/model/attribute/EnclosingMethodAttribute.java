package net.letsdank.jd.model.attribute;

/**
 * Атрибут EnclosingMethod указывает внешние класс и метод для локальных/анонимных классов.
 */
public record EnclosingMethodAttribute(String name, int classIndex,int methodIndex)implements AttributeInfo {
}
