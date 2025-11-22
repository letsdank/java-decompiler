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
import net.letsdank.jd.kotlin.MetadataFlagsKt;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.MethodInfo;

import java.util.List;

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
        KotlinPrettyPrinter printer = new KotlinPrettyPrinter();
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
        KotlinPrettyPrinter printer = new KotlinPrettyPrinter();

        // Можно попытаться вывести package, если он есть (через ClassFile или через KmPackage)
        KmPackage kmPackage = meta.getKmPackage();
        String pkg = cf.thisClassFqn();
        int lastDot = pkg.lastIndexOf('.');
        if (lastDot >= 0) {
            pkg = pkg.substring(0, lastDot);
            out.append("package ").append(pkg).append("\n\n");
        }

        out.append("// Kotlin file facade for ")
                .append(cf.thisClassFqn())
                .append("\n\n");

        for (MethodInfo method : cf.methods()) {
            MethodAst ast = decompiler.decompile(method, cf);
            String methodText = printer.printMethod(cf, method, ast);
            out.append(methodText).append("\n\n");
        }

        return out.toString();
    }

    private String decompileKotlinClass(ClassFile cf, MethodDecompiler decompiler, KotlinClassMetadata.Class classMeta) {
        StringBuilder out = new StringBuilder();
        KotlinPrettyPrinter printer = new KotlinPrettyPrinter();

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

        for (MethodInfo method : cf.methods()) {
            if (shouldSkipKotlinSynthMethod(cf, method)) continue;
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
}
