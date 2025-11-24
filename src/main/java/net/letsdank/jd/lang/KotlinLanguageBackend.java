package net.letsdank.jd.lang;

import net.letsdank.jd.ast.DecompilerOptions;
import net.letsdank.jd.ast.KotlinPrettyPrinter;
import net.letsdank.jd.ast.MethodAst;
import net.letsdank.jd.ast.MethodDecompiler;
import net.letsdank.jd.kotlin.KotlinMetadataReader;
import net.letsdank.jd.kotlin.KotlinPropertyRegistry;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.MethodInfo;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Бэкенд, который печатает методы в виде Kotlin-кода,
 * используя существующий AST и KotlinPrettyPrinter.
 */
public final class KotlinLanguageBackend implements LanguageBackend {

    private final DecompilerOptions options;

    public KotlinLanguageBackend(DecompilerOptions options) {
        this.options = options;
    }

    @Override
    public Language language() {
        return Language.KOTLIN;
    }

    @Override
    public String decompileMethod(ClassFile cf, MethodInfo method, MethodAst ast) {
        KotlinMetadataReader.KotlinClassModel model = KotlinMetadataReader.readClassModel(cf, options);

        // Используем имена свойств для принтера (локально - для текущего класса/файла)
        Set<String> propertyNames = model.propertyNames();

        // Регистрируем эти свойства в глобальном реестре (ownerInternalName = internal name class-файла)
        String ownerInternal = cf.thisClassInternalName();
        KotlinPropertyRegistry.register(ownerInternal, propertyNames);

        KotlinPrettyPrinter printer = new KotlinPrettyPrinter(propertyNames);
        return printer.printMethod(cf, method, ast);
    }

    @Override
    public String decompileClass(ClassFile cf, MethodDecompiler methodDecompiler) {
        KotlinMetadataReader.KotlinClassModel model = KotlinMetadataReader.readClassModel(cf);

        if (model.kind() == KotlinMetadataReader.KotlinClassModel.Kind.FILE_FACADE) {
            // TODO: поменять на нашу модель
            return decompileFileFacade(cf, methodDecompiler, model);
        }

        // data class на основе модели
        String dataClassResult = tryDecompileDataClass(cf, methodDecompiler, model);
        if (dataClassResult != null) return dataClassResult;

        // Нет метаданных или какой-то другой вид - используем простой fallback
        return decompileKotlinClass(cf, methodDecompiler, model);
    }

    private String decompileFileFacade(ClassFile cf, MethodDecompiler decompiler,
                                       KotlinMetadataReader.KotlinClassModel model) {
        StringBuilder out = new StringBuilder();

        // Можно попытаться вывести package, если он есть (через ClassFile или через KmPackage)
        String fqn = cf.thisClassFqn();
        String pkg = null;
        int lastDot = fqn.lastIndexOf('.');
        if (lastDot >= 0) {
            pkg = fqn.substring(0, lastDot);
        }

        if (pkg != null && !pkg.isEmpty()) {
            out.append("package ").append(pkg).append("\n\n");
        }

        // Собираем свойства из модели; для file facade они должны быть top-level
        List<KotlinMetadataReader.KotlinPropertyModel> properties = model.properties();

        Set<String> propertyNames = new LinkedHashSet<>();
        for (KotlinMetadataReader.KotlinPropertyModel p : properties) {
            propertyNames.add(p.name());
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
        for (KotlinMetadataReader.KotlinPropertyModel p : properties) {
            if (!p.isTopLevel()) continue;

            String varOrVal = p.isVar() ? "var" : "val";
            out.append(varOrVal)
                    .append(" ")
                    .append(p.name())
                    .append(": ")
                    .append(p.type())
                    .append("\n");
        }

        if (!properties.isEmpty()) {
            out.append("\n");
        }

        // Печатаем top-level функции
        for (MethodInfo method : cf.methods()) {
            if (shouldSkipKotlinSynthMethod(cf, method)) continue;

            MethodAst ast = decompiler.decompile(method, cf);
            String methodText = printer.printMethod(cf, method, ast);
            out.append(methodText).append("\n\n");
        }

        return out.toString();
    }

    private String decompileKotlinClass(ClassFile cf, MethodDecompiler decompiler,
                                        KotlinMetadataReader.KotlinClassModel model) {
        StringBuilder out = new StringBuilder();

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

        boolean isEnum = model.isEnumClass();
        boolean isObject = model.isObjectClass();
        boolean isValue = model.isValueClass();
        boolean isSealed = model.isSealedClass();
        boolean isData = model.isDataClass();

        KotlinPrettyPrinter printer = new KotlinPrettyPrinter();

        if (isEnum) {
            // enum class
            out.append("enum class ").append(simpleName).append(" {\n");

            // enum entries (CONSTANT1, CONSTANT2, ...)
            // TODO: enum entries

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

        // свойства берем из model.properties()
        for (KotlinMetadataReader.KotlinPropertyModel p : model.properties()) {
            // пропускаем top-level, если вдруг туда попали
            if (p.isTopLevel()) continue;

            String varOrVal = p.isVar() ? "var" : "val";
            out.append(varOrVal)
                    .append(" ")
                    .append(p.name())
                    .append(": ")
                    .append(p.type())
                    .append("\n");
        }

        // Собираем имена свойств для фильтрации геттеров/сеттеров
        Set<String> propertyNames = new LinkedHashSet<>();
        for (KotlinMetadataReader.KotlinPropertyModel p : model.properties()) {
            if (p.isTopLevel()) continue;
            propertyNames.add(p.name());
        }

        PropertyAccessNames accessNames = buildPropertyAccessNames(propertyNames);

        if (!model.properties().isEmpty()) {
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

    private String tryDecompileDataClass(ClassFile cf, MethodDecompiler decompiler,
                                         KotlinMetadataReader.KotlinClassModel model) {
        // Без читаемой метадаты безопаснее НЕ пытаться собирать data class
        if (!model.hasMetadata() || !model.isDataClass()) {
            return null;
        }

        List<KotlinMetadataReader.KotlinPropertyModel> props = model.properties();
        if (props.isEmpty()) return null;

        // FQN -> package + simpleName
        String fqn = cf.thisClassFqn();
        String pkg = null;
        String simpleName = fqn;
        int lastDot = fqn.lastIndexOf('.');
        if (lastDot >= 0) {
            pkg = fqn.substring(0, lastDot);
            simpleName = fqn.substring(lastDot + 1);
        }

        StringBuilder out = new StringBuilder();

        if (pkg != null && !pkg.isEmpty()) {
            out.append("package ").append(pkg).append("\n\n");
        }

        out.append("data class ").append(simpleName).append("(\n");

        for (int i = 0; i < props.size(); i++) {
            KotlinMetadataReader.KotlinPropertyModel p = props.get(i);
            String varOrVal = p.isVar() ? "var" : "val";

            out.append("    ")
                    .append(varOrVal).append(" ")
                    .append(p.name()).append(": ")
                    .append(p.type());

            if (i + 1 < props.size()) {
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

    private static class PropertyAccessNames {
        final Set<String> getterNames = new HashSet<>();
        final Set<String> setterNames = new HashSet<>();
    }
}
