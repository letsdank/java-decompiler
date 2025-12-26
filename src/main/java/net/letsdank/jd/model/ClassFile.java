package net.letsdank.jd.model;

import net.letsdank.jd.model.attribute.AttributeInfo;
import net.letsdank.jd.model.attribute.RecordAttribute;

/**
 * Минимальное представление class-файла.
 */
public record ClassFile(int minorVersion, int majorVersion, ConstantPool constantPool,
                        int accessFlags, int thisClassIndex, int superClassIndex, int[] interfaceIndices,
                        FieldInfo[] fields, MethodInfo[] methods, AttributeInfo[] attributes) {
    /**
     * Имя класса в виде "com/example/Foo" из constant pool
     */
    public String thisClassInternalName() {
        return constantPool.getClassName(thisClassIndex);
    }

    /**
     * Имя класса в виде "com.example.Foo".
     */
    public String thisClassFqn() {
        return thisClassInternalName().replace('/', '.');
    }

    /**
     * Простое имя класса без пакета.
     */
    public String thisClassSimpleName() {
        String fqn = thisClassFqn();
        int dot = fqn.lastIndexOf('.');
        return dot >= 0 ? fqn.substring(dot + 1) : fqn;
    }

    public String superClassInternalName() {
        if (superClassIndex == 0) {
            // Для java/lang/Object super_class = 0
            return null;
        }
        return constantPool.getClassName(superClassIndex);
    }

    public String superClassFqn() {
        String internal = superClassInternalName();
        return internal == null ? null : internal.replace('/', '.');
    }

    /**
     * Извлекает record components attribute, если это record.
     */
    public RecordAttribute.RecordComponent[] recordComponents() {
        for (AttributeInfo attr : attributes) {
            if (attr instanceof RecordAttribute ra) {
                return ra.components();
            }
        }
        return null;
    }
}
