package net.letsdank.jd.model.annotation;

/**
 * Базовый интерфейс для аннотаций.
 * Для большинства аннотаций нам хватает только дескриптора,
 * но для @kotlin.Metadata делаем спец-класс.
 */
public sealed interface AnnotationInfo
        permits SimpleAnnotationInfo, KotlinMetadataAnnotationInfo {

    String descriptor();
}
