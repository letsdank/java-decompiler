package net.letsdank.jd.kotlin;

import kotlin.metadata.KmClass;
import kotlin.metadata.KmPackage;
import kotlin.metadata.KmProperty;
import kotlin.metadata.jvm.KotlinClassMetadata;
import net.letsdank.jd.lang.KotlinClassDetector;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.FieldInfo;
import net.letsdank.jd.model.MethodInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Обертка над @kotlin.Metadata + fallback-эвристики.
 * <p>
 * Задача:
 * - попробовать вытащить имена свойств через kotlinx-metadata-jvm;
 * - если метадата битая / несовместимая -> использовать эвристику по полям+геттерам;
 * - выдать единый Set<String> имен свойств.
 */
public final class KotlinMetadataReader {

    private KotlinMetadataReader() {
    }

    /**
     * Главный метод: возвращает множество имен свойств Kotlin-класса.
     * <p>
     * Использует несколько источников:
     * 1. KotlinClassMetadata.Class / .FileFacade + KmProperty (если удалось прочитать);
     * 2. Эвристика по полям + getX()/isX() (если метадата недоступна).
     */
    public static Set<String> readEffectivePropertyNames(ClassFile cf) {
        Set<String> result = new HashSet<>();

        // 1. Попробуем пройти "официальным" путем через KotlinClassMetadata
        KotlinClassMetadata metadata = KotlinMetadataExtractor.extractFromClassFile(cf);
        if (metadata instanceof KotlinClassMetadata.Class classMeta) {
            KmClass kmClass = classMeta.getKmClass();
            List<KmProperty> props = kmClass.getProperties();
            for (KmProperty p : props) {
                result.add(p.getName());
            }
        } else if (metadata instanceof KotlinClassMetadata.FileFacade fileFacadeMeta) {
            KmPackage kmPackage = fileFacadeMeta.getKmPackage();
            List<KmProperty> props = kmPackage.getProperties();
            for (KmProperty p : props) {
                result.add(p.getName());
            }
        }

        // 2. Если метадата не прочиталась или не дала ничего - включаем эвристику.
        if (result.isEmpty() && KotlinClassDetector.isKotlinClass(cf)) {
            Set<String> inferred = inferKotlinPropertiesFromClassFile(cf);
            result.addAll(inferred);
        }

        return result;
    }

    /**
     * Эвристика: находит Kotlin-подобные свойства по полям и их getX()/isX() геттерам.
     * <p>
     * Ничего не знает про kotlinx-metadata, работает по чистому байткоду:
     * - собирает имена полей;
     * - ищет методы без аргументов вида getX / isX;
     * - если X с decap совпадает с полем -> считаем это "свойство".
     */
    public static Set<String> inferKotlinPropertiesFromClassFile(ClassFile cf) {
        Set<String> result = new HashSet<>();
        ConstantPool cp = cf.constantPool();

        // 1. Собираем имена полей
        Set<String> fieldNames = new HashSet<>();
        for (FieldInfo f : cf.fields()) {
            String fieldName = cp.getUtf8(f.nameIndex());
            fieldNames.add(fieldName);
        }

        // 2. Ищем геттеры / isX и смотрим, есть ли для них поле
        for (MethodInfo m : cf.methods()) {
            String name = cp.getUtf8(m.nameIndex());
            String desc = cp.getUtf8(m.descriptorIndex());

            // Нас интересуют только методы без параметров: ()T
            if (!desc.startsWith("()")) continue;

            String propName = null;
            if (name.startsWith("get") && name.length() > 3) {
                String base = name.substring(3);
                String decap = Character.toLowerCase(base.charAt(0)) + base.substring(1);
                propName = decap;
            } else if (name.startsWith("is") && name.length() > 2) {
                String base = name.substring(2);
                String decap = Character.toLowerCase(base.charAt(0)) + base.substring(1);
                propName = decap;
            }

            if (propName != null && fieldNames.contains(propName)) {
                result.add(propName);
            }
        }

        return result;
    }
}
