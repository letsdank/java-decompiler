package net.letsdank.jd.lang;

import kotlin.metadata.KmClass;
import kotlin.metadata.KmPackage;
import kotlin.metadata.KmProperty;
import kotlin.metadata.jvm.KotlinClassMetadata;
import net.letsdank.jd.ast.KotlinPrettyPrinter;
import net.letsdank.jd.ast.KotlinTypeUtils;
import net.letsdank.jd.ast.MethodAst;
import net.letsdank.jd.ast.MethodDecompiler;
import net.letsdank.jd.kotlin.KotlinMetadataExtractor;
import net.letsdank.jd.kotlin.KotlinPropertyRegistry;
import net.letsdank.jd.kotlin.MetadataFlagsKt;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.FieldInfo;
import net.letsdank.jd.model.MethodInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Бэкенд, который печатает методы в виде Kotlin-кода,
 * используя существующий AST и KotlinPrettyPrinter.
 */
public final class KotlinLanguageBackend implements LanguageBackend {
    @Override
    public Language language() {
        return Language.KOTLIN;
    }

    @Override
    public String decompileMethod(ClassFile cf, MethodInfo method, MethodAst ast) {
        // Пытаемся вытащить Kotlin-метаданные для этого class-файла
        KotlinClassMetadata metadata = KotlinMetadataExtractor.extractFromClassFile(cf);

        Set<String> propertyNames = new HashSet<>();

        if (metadata instanceof KotlinClassMetadata.Class classMeta) {
            KmClass kmClass = classMeta.getKmClass();
            for (KmProperty p : kmClass.getProperties()) {
                propertyNames.add(p.getName());
            }

            // Регистрируем свойства этого класса в глобальном реестре
            String ownerInternal = cf.thisClassInternalName();
            KotlinPropertyRegistry.register(ownerInternal, propertyNames);
        } else if (metadata instanceof KotlinClassMetadata.FileFacade fileFacadeMeta) {
            KmPackage kmPackage = fileFacadeMeta.getKmPackage();
            for (KmProperty p : kmPackage.getProperties()) {
                propertyNames.add(p.getName());
            }

            String ownerInternal = cf.thisClassInternalName();
            KotlinPropertyRegistry.register(ownerInternal, propertyNames);
        } else {
            // Fallback: метадата не прочиталась, но класс-то Kotlin'овский.
            if (KotlinClassDetector.isKotlinClass(cf)) {
                Set<String> inferred = inferKotlinPropertiesFromClassFile(cf);
                propertyNames.addAll(inferred);

                String ownerInternal = cf.thisClassInternalName();
                KotlinPropertyRegistry.register(ownerInternal, propertyNames);
            }
        }

        KotlinPrettyPrinter printer = new KotlinPrettyPrinter(propertyNames);
        return printer.printMethod(cf, method, ast);
    }

    @Override
    public String decompileClass(ClassFile cf, MethodDecompiler methodDecompiler) {
        KotlinClassMetadata metadata = KotlinMetadataExtractor.extractFromClassFile(cf);

        if (metadata instanceof KotlinClassMetadata.FileFacade fileFacadeMeta) {
            // Это file facade: SampleKotlinKt - печатаем top-level функции
            return decompileFileFacade(cf, methodDecompiler, fileFacadeMeta);
        } else if (metadata instanceof KotlinClassMetadata.Class classMeta) {
            // Обычный Kotlin-класс (в том числе data class)
            String dataClassResult = tryDecompileDataClass(cf, methodDecompiler, classMeta);
            if (dataClassResult != null) return dataClassResult;
            return decompileKotlinClass(cf, methodDecompiler, classMeta);
        } else {
            // Нет метаданных или какой-то другой вид - используем простой fallback
            return decompileAsPlainKotlin(cf, methodDecompiler);
        }
    }

    private String decompileFileFacade(ClassFile cf, MethodDecompiler decompiler,
                                       KotlinClassMetadata.FileFacade meta) {
        StringBuilder out = new StringBuilder();

        // Можно попытаться вывести package, если он есть (через ClassFile или через KmPackage)
        KmPackage kmPackage = meta.getKmPackage();
        String pkg = cf.thisClassFqn();
        int lastDot = pkg.lastIndexOf('.');
        if (lastDot >= 0) {
            pkg = pkg.substring(0, lastDot);
            out.append("package ").append(pkg).append("\n\n");
        }

        // Собираем свойства из KmPackage
        List<KmProperty> kmProps = kmPackage.getProperties();
        List<KotlinPropertyModel> properties = new ArrayList<>();
        for (KmProperty p : kmProps) {
            String name = p.getName();
            String type = KotlinTypeUtils.kmTypeToKotlin(p.getReturnType());
            boolean isVar = MetadataFlagsKt.isVarProperty(p);
            properties.add(new KotlinPropertyModel(name, type, isVar, true));
        }

        // Собираем список имен свойств для принтера, чтобы он мог знать,
        // какие getX()/isX() относятся к настоящим Kotlin-свойствам из метаданных
        Set<String> propertyNames = new HashSet<>();
        for (KotlinPropertyModel p : properties) {
            propertyNames.add(p.name);
        }
        String ownerInternal = cf.thisClassInternalName();

        // Регистрируем свойства файла в глобальном реестре
        KotlinPropertyRegistry.register(ownerInternal, propertyNames);

        // Создаем принтер с информацией о свойствах
        KotlinPrettyPrinter printer = new KotlinPrettyPrinter(propertyNames);

        out.append("// Kotlin file facade for ")
                .append(cf.thisClassFqn())
                .append("\n\n");

        // Печатаем top-level свойства
        for (KotlinPropertyModel p : properties) {
            String varOrVal = p.isVar ? "var" : "val";
            out.append(varOrVal)
                    .append(" ")
                    .append(p.name)
                    .append(": ")
                    .append(p.type)
                    .append("\n");
        }

        if (!properties.isEmpty()) {
            out.append("\n");
        }

        for (MethodInfo method : cf.methods()) {
            MethodAst ast = decompiler.decompile(method, cf);
            String methodText = printer.printMethod(cf, method, ast);
            out.append(methodText).append("\n\n");
        }

        return out.toString();
    }

    private String decompileKotlinClass(ClassFile cf, MethodDecompiler decompiler, KotlinClassMetadata.Class classMeta) {
        StringBuilder out = new StringBuilder();
        KmClass kmClass = classMeta.getKmClass();

        // Вычисляем package и простое имя по FQN class-файла
        String fqn = cf.thisClassFqn();
        String pkg = null;
        String simpleName = fqn;
        int lastDot = fqn.lastIndexOf('.');
        if (lastDot >= 0) {
            pkg = fqn.substring(0, lastDot);
            simpleName = fqn.substring(lastDot + 1);
        }

        if (pkg != null && !pkg.isEmpty()) {
            out.append("package ").append(pkg).append("\n\n");
        }

        // Свойства класса из KmClass
        List<KmProperty> kmProps = kmClass.getProperties();
        List<KotlinPropertyModel> properties = new ArrayList<>();
        for (KmProperty p : kmProps) {
            String name = p.getName();
            String type = KotlinTypeUtils.kmTypeToKotlin(p.getReturnType());
            boolean isVar = MetadataFlagsKt.isVarProperty(p);
            properties.add(new KotlinPropertyModel(name, type, isVar, false));
        }

        // Имена свойств для принтера (используем только имена, без эвристики на getX)
        Set<String> propertyNames = new HashSet<>();
        for (KotlinPropertyModel p : properties) {
            propertyNames.add(p.name);
        }

        // Регистрируем свойства этого класса в глобальном реестре
        String ownerInternal = cf.thisClassInternalName();
        KotlinPropertyRegistry.register(ownerInternal, propertyNames);

        // AccessNames - для фильтрации getter/setter в этом же классе
        PropertyAccessNames accessNames = buildPropertyAccessNames(propertyNames);

        // Принтер с набором имен свойств
        KotlinPrettyPrinter printer = new KotlinPrettyPrinter(propertyNames);

        // Companion: SampleService$Companion -> class SampleService { companion object { ... } }
        boolean isCompanionLike = simpleName.endsWith("$Companion");
        if (isCompanionLike) {
            String owner = simpleName.substring(0, simpleName.length() - "$Companion".length());

            out.append("class ").append(owner).append(" {\n\n");
            out.append("    companion owner {\n\n");

            for (MethodInfo method : cf.methods()) {
                if (shouldSkipKotlinSynthMethod(cf, method)) continue;
                MethodAst ast = decompiler.decompile(method, cf);
                String methodText = printer.printMethod(cf, method, ast);
                // методы компаньона - на один уровень глубже
                out.append(indent(indent(methodText))).append("\n\n");
            }

            out.append("    }\n");
            out.append("}\n");
            return out.toString();
        }

        // Не companion: определяем вид класса
        boolean isEnum = MetadataFlagsKt.isEnumClass(kmClass);
        boolean isObject = MetadataFlagsKt.isObjectClass(kmClass);
        boolean isValue = MetadataFlagsKt.isValueClass(kmClass);
        boolean isData = MetadataFlagsKt.isDataClass(kmClass);
        boolean isSealed = MetadataFlagsKt.isSealedClass(kmClass);

        if (isEnum) {
            // enum class
            out.append("enum class ").append(simpleName).append(" {\n");

            // enum entries (CONSTANT1, CONSTANT2, ...)
            List<String> entries = MetadataFlagsKt.enumEntries(kmClass);
            if (!entries.isEmpty()) {
                for (int i = 0; i < entries.size(); i++) {
                    out.append("    ").append(entries.get(i));
                    if (i + 1 < entries.size()) {
                        out.append(",");
                    }
                    out.append("\n");
                }
                out.append("\n");
            }

            // методы enum-а
            for (MethodInfo method : cf.methods()) {
                if (shouldSkipKotlinSynthMethod(cf, method)) continue;
                MethodAst ast = decompiler.decompile(method, cf);
                String methodText = printer.printMethod(cf, method, ast);
                out.append(indent(methodText)).append("\n\n");
            }

            out.append("}\n");
            return out.toString();
        }

        if (isObject) {
            // object Foo { ... }
            out.append("object ").append(simpleName).append(" {\n\n");

            for (MethodInfo method : cf.methods()) {
                if (shouldSkipKotlinSynthMethod(cf, method)) continue;
                MethodAst ast = decompiler.decompile(method, cf);
                String methodText = printer.printMethod(cf, method, ast);
                out.append(indent(methodText)).append("\n\n");
            }

            out.append("}\n");
            return out.toString();
        }

        // Обычный class / data / sealed / value

        StringBuilder header = new StringBuilder();

        if (isSealed) {
            header.append("sealed ");
        }
        if (isData) {
            header.append("data ");
        }
        if (isValue) {
            header.append("value ");
        }

        header.append("class ");

        out.append(header).append(simpleName).append(" {\n\n");

        // Печатаем top-level свойства
        for (KotlinPropertyModel p : properties) {
            String varOrVal = p.isVar ? "var" : "val";
            out.append(varOrVal)
                    .append(" ")
                    .append(p.name)
                    .append(": ")
                    .append(p.type)
                    .append("\n");
        }

        if (!properties.isEmpty()) {
            out.append("\n");
        }

        for (MethodInfo method : cf.methods()) {
            if (shouldSkipKotlinSynthMethod(cf, method)) continue;

            ConstantPool cp = cf.constantPool();
            String methodName = cp.getUtf8(method.nameIndex());

            // Если это геттер/сеттер свойства, не выводим его как обычную функцию
            if (accessNames.getterNames.contains(methodName) ||
                    accessNames.setterNames.contains(methodName)) {
                continue;
            }

            MethodAst ast = decompiler.decompile(method, cf);
            String methodText = printer.printMethod(cf, method, ast);
            out.append(indent(methodText)).append("\n\n");
        }

        out.append("}\n");
        return out.toString();
    }

    private String tryDecompileDataClass(ClassFile cf, MethodDecompiler decompiler, KotlinClassMetadata.Class classMeta) {
        KmClass km = classMeta.getKmClass();

        // 1. Проверяем, что это действительно data class
        if (!MetadataFlagsKt.isDataClass(km)) {
            return null;
        }

        // 2. Собираем свойства primary-конструктора
        List<KmProperty> properties = km.getProperties();
        if (properties.isEmpty()) {
            // На всякий случай: странный "data class" без свойств - отдадим обработку дальше
            return null;
        }

        // Регистрируем свойства этого data class в глобальном реестре
        String ownerInternal = cf.thisClassInternalName();

        Set<String> propertyNames = new HashSet<>();
        for (KmProperty p : properties) {
            propertyNames.add(p.getName()); // "id", "name"
        }
        KotlinPropertyRegistry.register(ownerInternal, propertyNames);

        // 3. Вычисляем имя класса
        String fqn = cf.thisClassFqn();
        String pkg = null;
        String simpleName = fqn;
        int lastDot = fqn.lastIndexOf('.');
        if (lastDot >= 0) {
            pkg = fqn.substring(0, lastDot);
            simpleName = fqn.substring(lastDot + 1);
        }

        StringBuilder out = new StringBuilder();

        // Пакет
        if (pkg != null && !pkg.isEmpty()) {
            out.append("package ").append(pkg).append("\n\n");
        }

        // Заголовок data class
        out.append("data class ").append(simpleName).append("(\n");

        for (int i = 0; i < properties.size(); i++) {
            KmProperty p = properties.get(i);
            String name = p.getName();

            // val / var - по metadata
            boolean isVar = MetadataFlagsKt.isVarProperty(p);
            String varOrVal = isVar ? "var" : "val";

            // Красивый Kotlin-тип из KmType
            String type = KotlinTypeUtils.kmTypeToKotlin(p.getReturnType());

            out.append("    ")
                    .append(varOrVal).append(" ")
                    .append(name).append(": ")
                    .append(type);

            if (i + 1 < properties.size()) {
                out.append(",");
            }
            out.append("\n");
        }

        out.append(")\n");

        return out.toString();
    }

    /**
     * Fallback: если метаданных нет, просто печатаем набор функций,
     * как и раньше (старое поведение).
     */
    private String decompileAsPlainKotlin(ClassFile cf, MethodDecompiler decompiler) {
        StringBuilder out = new StringBuilder();
        KotlinPrettyPrinter printer = new KotlinPrettyPrinter();

        String fqn = cf.thisClassFqn();
        String pkg = null;
        String simpleName = fqn;
        int lastDot = fqn.lastIndexOf('.');
        if (lastDot >= 0) {
            pkg = fqn.substring(0, lastDot);
            simpleName = fqn.substring(lastDot + 1);
        }

        if (pkg != null && !pkg.isEmpty()) {
            out.append("package ").append(pkg).append("\n\n");
        }

        boolean isCompanion = simpleName.endsWith("$Companion");
        boolean isFileFacadeLike = simpleName.endsWith("Kt");

        if (isCompanion) {
            String owner = simpleName.substring(0, simpleName.length() - "$Companion".length());
            out.append("object ").append(owner).append(".Companion").append(" {\n\n");
        } else if (!isFileFacadeLike) {
            out.append("class ").append(simpleName).append(" {\n\n");
        }

        for (MethodInfo method : cf.methods()) {
            if (shouldSkipLowLevelMethod(cf, method)) continue;

            MethodAst ast = decompiler.decompile(method, cf);
            String methodText = printer.printMethod(cf, method, ast);
            if (!isFileFacadeLike) {
                out.append(indent(methodText)).append("\n\n");
            } else {
                out.append(methodText).append("\n\n");
            }
        }

        if (!isFileFacadeLike) {
            out.append("}\n");
        }

        return out.toString();
    }

    private static Set<String> inferKotlinPropertiesFromClassFile(ClassFile cf) {
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

            String propName = null;
            if (name.startsWith("get") && name.length() > 3) {
                String base = name.substring(3);
                String decap = Character.toLowerCase(base.charAt(0)) + base.substring(1);
                propName = decap;
            } else if (name.startsWith("is") && name.length() > 2) {
                String base = name.substring(2);
                String decap = Character.toLowerCase(base.charAt(0)) + base.substring(1);
            }

            if (propName != null && fieldNames.contains(propName)) {
                result.add(propName);
            }
        }

        return result;
    }

    private boolean shouldSkipLowLevelMethod(ClassFile cf, MethodInfo method) {
        ConstantPool cp = cf.constantPool();
        String name = cp.getUtf8(method.nameIndex());

        // Статический инициализатор - не показываем как функцию
        if ("<clinit>".equals(name)) {
            return true;
        }

        // Временный хак: не показываем copy$default - это чисто служебный метод data-классов
        if ("copy$default".equals(name)) {
            return true;
        }

        return false;
    }

    private boolean shouldSkipKotlinSynthMethod(ClassFile cf, MethodInfo method) {
        ConstantPool cp = cf.constantPool();
        String name = cp.getUtf8(method.nameIndex());

        if ("<init>".equals(name)) return true;
        if (name.startsWith("component")) return true;
        if (name.equals("copy")) return true;
        if (name.equals("copy$default")) return true;
        if (name.equals("equals")) return true;
        if (name.equals("hashCode")) return true;
        if (name.equals("toString")) return true;
        return false;
    }

    private static String indent(String text) {
        // Простейшая индентация: добавляем 4 пробела перед каждой непустой строкой
        String[] lines = text.split("\\R", -1);
        StringBuilder sb = new StringBuilder(text.length() + 16 * lines.length);
        for (String line : lines) {
            if (!line.isEmpty()) {
                sb.append("    ").append(line);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static PropertyAccessNames buildPropertyAccessNames(Set<String> propertyNames) {
        PropertyAccessNames pan = new PropertyAccessNames();
        for (String prop : propertyNames) {
            String cap = capitalize(prop);
            // стандартные соглашения
            pan.getterNames.add("get" + cap);
            pan.getterNames.add("is" + cap); // для Boolean-свойств
            pan.setterNames.add("set" + cap);
        }
        return pan;
    }

    private static final class KotlinPropertyModel {
        final String name;          // имя свойства в Kotlin (user, greeting, count)
        final String type;          // отрендеренный тип (String, Int, User, List<String>...)
        final boolean isVar;        // var или val
        final boolean isTopLevel;   // top-level (file facade) или член класса

        KotlinPropertyModel(String name, String type, boolean isVar, boolean isTopLevel) {
            this.name = name;
            this.type = type;
            this.isVar = isVar;
            this.isTopLevel = isTopLevel;
        }
    }

    private static class PropertyAccessNames {
        final Set<String> getterNames = new HashSet<>();
        final Set<String> setterNames = new HashSet<>();
    }
}
