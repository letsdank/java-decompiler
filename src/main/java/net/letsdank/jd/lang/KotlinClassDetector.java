package net.letsdank.jd.lang;

import net.letsdank.jd.model.ClassFile;

/**
 * Детектор Kotlin-классов.
 * <p>
 * Сейчас реализация очень простая и эвристическая:
 * - если имя класса оканчивается на "Kt" (типичный топ-левел Kotlin-файла),
 * считаем его Kotlin-классом.
 * <p>
 * Позже сюда можно добавить полноценную проверку по аннотации @kotlin.Metadata,
 * как только в ClassFile появятся атрибуты класса.
 */
public final class KotlinClassDetector {
    private KotlinClassDetector() {
    }

    public static boolean isKotlinClass(ClassFile cf) {
        String fqn = cf.thisClassFqn();
        if (fqn == null) return false;

        // Простейшая эвристика: FooKt -> Kotlin-генерация top-level функций.
        if (fqn.endsWith("Kt")) return true;

        // TODO: когда появятся атрибуты у ClassFile:
        //  1) найти RuntimeVisibleAnnotations
        //  2) проверить наличие аннотации "Lkotlin/Metadata;"

        return false;
    }
}
