package net.letsdank.jd.model;

/**
 * Минимальное представление class-файла.
 */
public record ClassFile(int minorVersion, int majorVersion, ConstantPool constantPool, int accessFlags,
                        int thisClassIndex, int superClassIndex, int[] interfaceIndices) {
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
}
