package net.letsdank.jd.lang;

import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.MethodInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Детектор Record классов (Java 16+).
 *
 * Record компилируется в класс с:
 * - Аннотацией RecordComponent
 * - private final полями для компонентов
 * Методом java.lang.Class#recordComponents()
 * Сгенерированными equals, hashCode, toString
 */
public final class RecordDetector {

    /**
     * Проверяет, является ли класс record.
     */
    public static boolean isRecord(ClassFile classFile) {
        // Records содержат RecordComponent attribute
        return classFile.recordComponents() != null && classFile.recordComponents().length > 0;
    }

    /**
     * Извлекает компоненты record.
     */
    public static List<RecordComponent> extractRecordComponents(ClassFile classFile) {
        List<RecordComponent> components = new ArrayList<>();

        if (!isRecord(classFile)) {
            return components;
        }

        var recordComps = classFile.recordComponents();
        if (recordComps == null) {
            return components;
        }

        ConstantPool cp = classFile.constantPool();

        for (var rc : recordComps) {
            String name = cp.getUtf8(rc.nameIndex());
            String descriptor = cp.getUtf8(rc.descriptorIndex());

            // Преобразуем descriptor в читаемый тип
            String typeName = descriptorToTypeName(descriptor, cp);

            components.add(new RecordComponent(name, typeName, descriptor));
        }

        return components;
    }

    /**
     * Проверяет, имеет ли класс сгенерированные методы record.
     */
    public static boolean hasGeneratedRecordMethods(ClassFile classFile) {
        ConstantPool cp = classFile.constantPool();
        int generatedCount = 0;

        for (MethodInfo method : classFile.methods()) {
            String methodName = cp.getUtf8(method.nameIndex());

            if ("equals".equals(methodName) ||
                    "hashCode".equals(methodName) ||
                    "toString".equals(methodName)) {
                // Проверяем, что это синтетический метод
                if ((method.accessFlags() & 0x1000) != 0) { // synthetic
                    generatedCount++;
                }
            }
        }

        return generatedCount >= 2; // обычно все три сгенерированы
    }

    /**
     * Преобразует descriptor в читаемое имя типа.
     */
    private static String descriptorToTypeName(String descriptor, ConstantPool cp) {
        if (descriptor == null || descriptor.isEmpty()) {
            return "Object";
        }

        return switch (descriptor.charAt(0)) {
            case 'Z' -> "boolean";
            case 'B' -> "byte";
            case 'C' -> "char";
            case 'S' -> "short";
            case 'I' -> "int";
            case 'J' -> "long";
            case 'F' -> "float";
            case 'D' -> "double";
            case 'V' -> "void";
            case 'L' -> {
                // Object type
                String internal = descriptor.substring(1, descriptor.length() - 1);
                yield internal.replace('/', '.');
            }
            case '[' -> {
                // Array type
                String baseDesc = descriptor.substring(1);
                String baseType = descriptorToTypeName(baseDesc, cp);
                int arrayDims = 0;
                for (char c : descriptor.toCharArray()) {
                    if (c == '[') arrayDims++;
                }
                yield baseType + "[]".repeat(arrayDims);
            }
            default -> "Object";
        };
    }

    /**
     * Компонент record.
     */
    public record RecordComponent(String name, String typeName, String descriptor) {
        @NotNull
        @Override
        public String toString() {
            return typeName + " " + name;
        }
    }
}
