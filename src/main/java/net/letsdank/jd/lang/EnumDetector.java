package net.letsdank.jd.lang;

import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.FieldInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Детектор Enum классов.
 *
 * Java enum компилируется в класс, который наследует java.lang.Enum.
 * Enum константы это static final поля типа самого enum.
 */
public final class EnumDetector {

    /**
     * Проверяет, является ли класс enum.
     */
    public static boolean isEnum(ClassFile classFile) {
        String superClassName = classFile.superClassFqn();
        return "java.lang.Enum".equals(superClassName);
    }

    /**
     * Извлекает имена enum констант (static final поля типа enum).
     */
    public static List<String> extractEnumConstants(ClassFile classFile) {
        List<String> constants = new ArrayList<>();

        if (!isEnum(classFile)) {
            return constants;
        }

        String enumTypeName = classFile.thisClassFqn();
        ConstantPool cp = classFile.constantPool();

        for (FieldInfo field : classFile.fields()) {
            // Enum константы: static final
            if (!isStaticFinal(field.accessFlags())) {
                continue;
            }

            // Тип должен быть сам enum
            String fieldType = cp.getUtf8(field.descriptorIndex());
            String simpleType = fieldType.startsWith("L") && fieldType.endsWith(";")
                    ? fieldType.substring(1, fieldType.length() - 1).replace('/', '.')
                    : fieldType;

            if (!simpleType.equals(enumTypeName)) {
                continue;
            }

            // Это enum константа
            String fieldName = cp.getUtf8(field.nameIndex());
            constants.add(fieldName);
        }

        return constants;
    }

    /**
     * Извлекает поля enum (кроме констант и синтетических).
     */
    public static List<String> extractEnumFields(ClassFile classFile) {
        List<String> fields = new ArrayList<>();

        if (!isEnum(classFile)) {
            return fields;
        }

        ConstantPool cp = classFile.constantPool();
        String enumTypeName = classFile.thisClassFqn();

        for (FieldInfo field : classFile.fields()) {
            // Пропускаем static final поля (это константы)
            if (isStaticFinal(field.accessFlags())) {
                String fieldType = cp.getUtf8(field.descriptorIndex());
                String simpleType = fieldType.startsWith("L") && fieldType.endsWith(";")
                        ? fieldType.substring(1, fieldType.length() - 1).replace('/', '.')
                        : fieldType;
                if (simpleType.equals(enumTypeName)) {
                    continue; // это enum константа
                }
            }

            // Пропускаем синтетические поля
            if ((field.accessFlags() & 0x1000) != 0) { // synthetic flag
                continue;
            }

            String fieldName = cp.getUtf8(field.nameIndex());
            fields.add(fieldName);
        }

        return fields;
    }

    /**
     * Проверяет, является ли флаг static final.
     */
    private static boolean isStaticFinal(int accessFlags) {
        return (accessFlags & 0x0008) != 0 && (accessFlags & 0x0010) != 0; // STATIC | FINAL
    }
}
