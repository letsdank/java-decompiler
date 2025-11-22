package net.letsdank.jd.lang;

import kotlin.metadata.KmClass;
import kotlin.metadata.KmPackage;
import kotlin.metadata.jvm.KotlinClassMetadata;
import net.letsdank.jd.ast.KotlinPrettyPrinter;
import net.letsdank.jd.ast.MethodAst;
import net.letsdank.jd.ast.MethodDecompiler;
import net.letsdank.jd.kotlin.KotlinMetadataExtractor;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.MethodInfo;

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

        // Вычисляем package по FQN class-файла
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

        // TODO: позже можно использовать KmClass.isData (Attributes API) из Kotlin-кода.
        //  Пока не добавляем 'data' автоматически, чтобы не тянуться к extension-свойства из Java.
        boolean isData = false;

        if (isData) {
            out.append("data ");
        }
        out.append("class ").append(simpleName).append(" {\n\n");

        for (MethodInfo method : cf.methods()) {
            MethodAst ast = decompiler.decompile(method, cf);
            String methodText = printer.printMethod(cf, method, ast);
            out.append(indent(methodText)).append("\n\n");
        }

        out.append("}\n");
        return out.toString();
    }

    /**
     * Fallback: если метаданных нет, просто печатаем набор функций,
     * как и раньше (старое поведение).
     */
    private String decompileAsPlainKotlin(ClassFile cf, MethodDecompiler decompiler) {
        StringBuilder out = new StringBuilder();
        KotlinPrettyPrinter printer = new KotlinPrettyPrinter();

        out.append("// decompiled Kotlin class (experimental)\n");
        out.append("// ").append(cf.thisClassFqn()).append("\n\n");

        for (MethodInfo method : cf.methods()) {
            MethodAst ast = decompiler.decompile(method, cf);
            String methodText = printer.printMethod(cf, method, ast);
            out.append(methodText).append("\n\n");
        }

        return out.toString();
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
