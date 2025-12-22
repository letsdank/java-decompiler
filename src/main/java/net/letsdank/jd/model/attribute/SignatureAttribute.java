package net.letsdank.jd.model.attribute;

/**
 * Атрибут Signature хранит строку generic-сигнатуры для классов, полей или методов
 */
public record SignatureAttribute(String name, String signature) implements AttributeInfo {
}
