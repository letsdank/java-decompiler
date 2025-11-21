package net.letsdank.jd.model;

/**
 * Атрибут RuntimeVisibleAnnotations.
 * Пока храним только список аннотаций с их дескрипторами.
 */
public record RuntimeVisibleAnnotationsAttribute(String name, AnnotationInfo[] annotations)
        implements AttributeInfo {
}
