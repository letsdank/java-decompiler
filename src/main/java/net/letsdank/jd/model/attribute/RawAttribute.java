package net.letsdank.jd.model.attribute;

/**
 * Атрибут, который мы пока никак не интерпретируем.
 */
public record RawAttribute(String name,byte[] data) implements AttributeInfo {
}
