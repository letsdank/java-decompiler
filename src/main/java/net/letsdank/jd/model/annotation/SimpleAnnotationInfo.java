package net.letsdank.jd.model.annotation;

/**
 * Обычная аннотация: храним только дескриптор вида "Ljavax/annotation/Nullable;"
 */
public record SimpleAnnotationInfo(String descriptor) implements AnnotationInfo {
}
