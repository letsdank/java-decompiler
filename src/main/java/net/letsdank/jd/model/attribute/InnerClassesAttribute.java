package net.letsdank.jd.model.attribute;

import java.util.List;

/**
 * Атрибут InnerClasses перечисляет вложенные/внутренние классы.
 */
public record InnerClassesAttribute(String name, List<Entry> classes) implements AttributeInfo {
    public record Entry(int innerClassInfoIndex,
                        int outerClassInfoIndex,
                        int innerNameIndex,
                        int innerClassAccessFlags) {
    }
}
