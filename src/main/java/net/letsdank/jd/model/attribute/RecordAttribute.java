package net.letsdank.jd.model.attribute;

import org.jetbrains.annotations.NotNull;

/**
 * Record Components attribute (Java 16+).
 *
 * Содержит информацию о компонентах record класса.
 * Каждый компонент это фактически компонент record, который становится final полем
 * и параметром канонического конструктора.
 */
public record RecordAttribute(RecordComponent[] components) implements AttributeInfo {
    public RecordAttribute(RecordComponent[] components) {
        this.components = components != null ? components : new RecordComponent[0];
    }

    @Override
    public String name() {
        return "Record";
    }

    @NotNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Record{");
        for (int i = 0; i < components.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(components[i]);
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Record component descriptor.
     *
     * Содержит информацию об одном компоненте record:
     * - name_index указывает на UTF8 с именем компонента
     * - descriptor_index указывает на UTF8 с дескриптором типа
     */
    public record RecordComponent(int nameIndex, int descriptorIndex) {
        @NotNull
        @Override
        public String toString() {
            return "RecordComponent(name_idx=" + nameIndex + ", descriptor_idx=" + descriptorIndex + ")";
        }
    }
}
