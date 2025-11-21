package net.letsdank.jd.lang;

import net.letsdank.jd.model.*;
import net.letsdank.jd.model.annotation.AnnotationInfo;
import net.letsdank.jd.model.annotation.SimpleAnnotationInfo;
import net.letsdank.jd.model.attribute.AttributeInfo;
import net.letsdank.jd.model.attribute.RuntimeVisibleAnnotationsAttribute;

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
        // 1. Строгий способ: ищем @kotlin.Metadata в аннотациях класса
        AttributeInfo[] attrs = cf.attributes();
        if (attrs != null) {
            for (AttributeInfo attr : attrs) {
                if (attr instanceof RuntimeVisibleAnnotationsAttribute annsAttr) {
                    for (AnnotationInfo ann : annsAttr.annotations()) {
                        if ("Lkotlin/Metadata;".equals(ann.descriptor())) {
                            return true;
                        }
                    }
                }
            }
        }

        // 2. Бэкап: по пулу констант (на случай, если кто-то создает ClassFile вручную без атрибутов)
        ConstantPool cp = cf.constantPool();
        if (cp.containsUtf8("Lkotlin/Metadata;") || cp.containsUtf8("kotlin/Metadata")) {
            return true;
        }

        // 3. Самая грубая эвристика: имя класса оканчивается на Kt
        String fqn = cf.thisClassFqn();
        return fqn != null && fqn.endsWith("Kt");
    }
}
