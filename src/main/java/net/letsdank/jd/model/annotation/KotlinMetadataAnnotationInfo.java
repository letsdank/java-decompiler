package net.letsdank.jd.model.annotation;

/**
 * Специализированное представление аннотации @kotlin.Metadata.
 * <p>
 * Поля соответствуют параметрам конструктора kotlin.Metadata:
 * - k: kind
 * - mv: metadataVersion
 * - bv: bytecodeVersion
 * - d1, d2: data (строковые массивы)
 * - xs: extraString
 * - xi: extraInt
 */
public record KotlinMetadataAnnotationInfo(
        String descriptor,
        int kind,
        int[] metadataVersion,
        int[] bytecodeVersion,
        String[] data1,
        String[] data2,
        String extraString,
        int extraInt
) implements AnnotationInfo {
}
