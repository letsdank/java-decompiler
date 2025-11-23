package net.letsdank.jd.kotlin;

import kotlin.metadata.KmClass;
import kotlin.metadata.KmPackage;
import kotlin.metadata.KmProperty;
import kotlin.metadata.jvm.KotlinClassMetadata;
import net.letsdank.jd.ast.DecompilerOptions;
import net.letsdank.jd.ast.KotlinTypeUtils;
import net.letsdank.jd.lang.KotlinClassDetector;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.FieldInfo;
import net.letsdank.jd.model.MethodInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Высокоуровневая "читалка" @kotlin.Metadata + эвристик по байткоду.
 * <p>
 * Здесь мы:
 * - пробуем прочитать KotlinClassMetadata и KmClass/KmPackage;
 * - вынимаем флаги класса (data, sealed, enum, object, value);
 * - вынимаем KmProperty (имя, тип, val/var, top-level или другие);
 * - если метадата недоступна или битая - включаем эвристику по полям + геттерам.
 * <p>
 * Все остальное (бэкенды, принтеры, реестр) должны опираться на эту модель,
 * а не напрямую на kotlinx-metadata.
 */
public final class KotlinMetadataReader {

    private KotlinMetadataReader() {
    }

    /**
     * @param name       имя свойства в Kotlin (user, greeting, count)
     * @param type       отрендеренный тип (String, Int, User, List<String>...)
     * @param isVar      var или val
     * @param isTopLevel top-level (file facade) или член класса
     */
    public record KotlinPropertyModel(String name, String type, boolean isVar, boolean isTopLevel) {
        @NotNull
        @Override
        public String toString() {
            return (isTopLevel ? "top " : "") +
                    (isVar ? "var " : "val ") +
                    name + ": " + type;
        }
    }

    public record KotlinClassModel(String internalName, Kind kind, boolean isKotlinClass,
                                   boolean hasMetadata, boolean isDataClass, boolean isSealedClass,
                                   boolean isEnumClass, boolean isObjectClass, boolean isValueClass,
                                   List<KotlinPropertyModel> properties) {
        public enum Kind {
            CLASS,
            FILE_FACADE,
            UNKNOWN
        }

        public Set<String> propertyNames() {
            Set<String> names = new HashSet<>();
            for (KotlinPropertyModel p : properties) {
                names.add(p.name());
            }
            return names;
        }

        public KotlinPropertyModel findProperty(String name) {
            for (KotlinPropertyModel p : properties) {
                if (p.name.equals(name)) return p;
            }
            return null;
        }
    }

    public static KotlinClassModel readClassModel(ClassFile cf) {
        return readClassModel(cf, null);
    }

    /**
     * Главный метод: читает модель Kotlin-класса (или file-facade) по ClassFile.
     * <p>
     * - Если есть @Metadata и ее удается прочитать через kotlinx-metadata-jvm,
     * используются KmClass/KmPackage + MetadataFlagsKt.
     * - Если метадата битая/несовместимая, но класс распознан как Kotlin,
     * используются эвристики по полям+геттерам.
     * - Если класс не Kotlin - isKotlinClass=false, свойства пустые.
     */
    public static KotlinClassModel readClassModel(ClassFile cf, DecompilerOptions options) {
        String internalName = cf.thisClassInternalName();
        boolean isKotlin = KotlinClassDetector.isKotlinClass(cf);

        KotlinClassMetadata metadata = null;
        boolean hasMetadata = false;

        if (options == null || options.useKotlinxMetadata()) {
            try {
                metadata = KotlinMetadataExtractor.extractFromClassFile(cf);
                hasMetadata = metadata != null;
            } catch (Throwable ignored) {
                // на всякий случай не даем упасть
            }
        }

        List<KotlinPropertyModel> props = new ArrayList<>();
        KotlinClassModel.Kind kind = KotlinClassModel.Kind.UNKNOWN;
        boolean isData = false;
        boolean isSealed = false;
        boolean isEnum = false;
        boolean isObject = false;
        boolean isValue = false;

        // 1. Метадата прочиталась -> используем ее
        if (metadata instanceof KotlinClassMetadata.Class classMeta) {
            kind = KotlinClassModel.Kind.CLASS;

            KmClass kmClass = classMeta.getKmClass();

            isData = MetadataFlagsKt.isDataClass(kmClass);
            isSealed = MetadataFlagsKt.isSealedClass(kmClass);
            isEnum = MetadataFlagsKt.isEnumClass(kmClass);
            isObject = MetadataFlagsKt.isObjectClass(kmClass);
            isValue = MetadataFlagsKt.isValueClass(kmClass);

            List<KmProperty> kmProps = kmClass.getProperties();
            for (KmProperty p : kmProps) {
                String name = p.getName();
                String type = KotlinTypeUtils.kmTypeToKotlin(p.getReturnType());
                boolean isVar = MetadataFlagsKt.isVarProperty(p);
                props.add(new KotlinPropertyModel(name, type, isVar, false));
            }
        } else if (metadata instanceof KotlinClassMetadata.FileFacade fileFacadeMeta) {
            kind = KotlinClassModel.Kind.FILE_FACADE;

            KmPackage kmPackage = fileFacadeMeta.getKmPackage();
            List<KmProperty> kmProps = kmPackage.getProperties();
            for (KmProperty p : kmProps) {
                String name = p.getName();
                String type = KotlinTypeUtils.kmTypeToKotlin(p.getReturnType());
                boolean isVar = MetadataFlagsKt.isVarProperty(p);
                props.add(new KotlinPropertyModel(name, type, isVar, true));
            }
        } else if (isKotlin) {
            // 2. Метадаты нет или она не читается - но класс Kotlin'овский:
            //    включаем эвристики по полям+геттерам.
            Set<KotlinPropertyModel> inferred = inferKotlinPropertiesFromClassFile(cf);
            props.addAll(inferred);
        }

        return new KotlinClassModel(internalName, kind, isKotlin, hasMetadata,
                isData, isSealed, isEnum, isObject, isValue, props);
    }

    /**
     * Упрощенный API: только имена свойств.
     * Используется там, где не нужны типы и флаги.
     */
    public static Set<String> readEffectivePropertyNames(ClassFile cf) {
        return readClassModel(cf).propertyNames();
    }

    /**
     * Эвристика: находит Kotlin-подобные свойства по полям и их getX()/isX() геттерам.
     * <p>
     * Ничего не знает про kotlinx-metadata, работает по чистому байткоду:
     * - собирает имена полей;
     * - ищет методы без аргументов вида getX / isX;
     * - если X с decap совпадает с полем -> считаем это "свойство".
     */
    public static Set<KotlinPropertyModel> inferKotlinPropertiesFromClassFile(ClassFile cf) {
        Set<KotlinPropertyModel> result = new HashSet<>();
        ConstantPool cp = cf.constantPool();

        // 1. Собираем имена полей + их дескрипторы
        record FieldInfoView(String name, String desc) {
        }
        List<FieldInfoView> fields = new ArrayList<>();
        Set<String> fieldNames = new HashSet<>();
        for (FieldInfo f : cf.fields()) {
            String fieldName = cp.getUtf8(f.nameIndex());
            String fieldDesc = cp.getUtf8(f.descriptorIndex());
            fields.add(new FieldInfoView(fieldName, fieldDesc));
            fieldNames.add(fieldName);
        }

        // 2. Ищем геттеры без параметров
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

            if (propName == null) continue;
            if (!fieldNames.contains(propName)) continue;

            // Подбираем дескриптор поля для этого свойства
            String fieldDesc = null;
            for (FieldInfoView f : fields) {
                if (f.name.equals(propName)) {
                    fieldDesc = f.desc;
                    break;
                }
            }

            String kotlinType = fallbackKotlinTypeFromFieldDescriptor(fieldDesc);
            // мы не знаем val или var - считаем val по умолчанию
            result.add(new KotlinPropertyModel(propName, kotlinType, false, false));
        }

        return result;
    }

    /**
     * Примитивное преобразование дескриптора поля в Kotlin-тип.
     * Используется только в fallback-режиме, поэтому можно не покрывать все кейсы.
     */
    private static String fallbackKotlinTypeFromFieldDescriptor(String desc) {
        if (desc == null || desc.isEmpty()) {
            return "Any /* unknown */";
        }
        // Примитивы
        return switch (desc.charAt(0)) {
            case 'I' -> "Int";
            case 'J' -> "Long";
            case 'Z' -> "Boolean";
            case 'D' -> "Double";
            case 'F' -> "Float";
            case 'B' -> "Byte";
            case 'C' -> "Char";
            case 'S' -> "Short";
            case 'L' -> {
                // Объектный тип: Ljava/lang/String;
                if (desc.startsWith("Ljava/lang/String;")) {
                    yield "String";
                }
                // Простейшее эвристическое FQN -> simpleName
                int slash = desc.lastIndexOf('/');
                int semi = desc.indexOf(';', slash + 1);
                if (slash >= 0 && semi > slash) {
                    String simple = desc.substring(slash + 1, semi);
                    yield simple;
                }
                yield "Any /* " + desc + " */";
            }
            case '[' -> "Array<" + fallbackKotlinTypeFromFieldDescriptor(desc.substring(1)) + ">";
            default -> "Any /* " + desc + " */";
        };
    }
}
