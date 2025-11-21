package net.letsdank.jd.model.attribute;

import net.letsdank.jd.model.annotation.AnnotationInfo;

/**
 * Атрибут RuntimeVisibleAnnotations.
 * Пока храним только список аннотаций с их дескрипторами.
 */
public record RuntimeVisibleAnnotationsAttribute(String name, AnnotationInfo[] annotations)
        implements AttributeInfo {
}
