package net.letsdank.jd.model.attribute;

/**
 * Атрибут SourceFile содержит имя исходного файла, скомпилировавшего этот класс.
 */
public record SourceFileAttribute(String name, int sourceFileIndex) implements AttributeInfo {
}
