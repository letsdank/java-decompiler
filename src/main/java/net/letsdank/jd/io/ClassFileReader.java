package net.letsdank.jd.io;

import net.letsdank.jd.model.*;
import net.letsdank.jd.model.annotation.AnnotationInfo;
import net.letsdank.jd.model.annotation.KotlinMetadataAnnotationInfo;
import net.letsdank.jd.model.annotation.SimpleAnnotationInfo;
import net.letsdank.jd.model.attribute.*;
import net.letsdank.jd.model.cp.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Читает минимальный заголовок .class-файла.
 */
public final class ClassFileReader {
    public ClassFile read(InputStream rawInput) throws IOException {
        try (ClassFileInput in = new ClassFileInput(rawInput)) {
            long magic = in.readU4();
            if (magic != 0xCAFEBABEL) {
                throw new IOException(String.format("Invalid class file magic: 0x%08X", magic));
            }

            int minor = in.readU2();
            int major = in.readU2();

            ConstantPool cp = readConstantPool(in);

            int accessFlags = in.readU2();
            int thisClassIndex = in.readU2();
            int superClassIndex = in.readU2();

            int interfacesCount = in.readU2();
            int[] interfaces = new int[interfacesCount];
            for (int i = 0; i < interfacesCount; i++) {
                interfaces[i] = in.readU2();
            }

            FieldInfo[] fields = readFields(in, cp);
            MethodInfo[] methods = readMethods(in, cp);

            // Атрибуты класса
            AttributeInfo[] classAttributes = readAttributes(in, cp);

            return new ClassFile(
                    minor,
                    major,
                    cp,
                    accessFlags,
                    thisClassIndex,
                    superClassIndex,
                    interfaces,
                    fields,
                    methods,
                    classAttributes
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ConstantPool readConstantPool(ClassFileInput in) throws IOException {
        int cpCount = in.readU2();
        CpInfo[] entries = new CpInfo[cpCount];

        int i = 1;
        while (i < cpCount) {
            int tag = in.readU1();
            CpInfo entry;
            switch (tag) {
                case 1 -> { // CONSTANT_Utf8
                    int length = in.readU2();
                    byte[] bytes = new byte[length];
                    for (int j = 0; j < length; j++) {
                        bytes[j] = (byte) in.readU1();
                    }
                    String value = new String(bytes, StandardCharsets.UTF_8);
                    entry = new CpUtf8(tag, value);
                }
                case 3 -> { // Integer
                    long raw = in.readU4();
                    entry = new CpInteger(tag, (int) raw);
                }
                case 4 -> { // Float
                    long raw = in.readU4();
                    entry = new CpFloat(tag, Float.intBitsToFloat((int) raw));
                }
                case 5 -> { // Long (занимает два слота)
                    long high = in.readU4();
                    long low = in.readU4();
                    long value = (high << 32) | (low & 0xFFFFFFFFL);
                    entry = new CpLong(tag, value);
                    entries[i] = entry;
                    i++; // следующий индекс пропускаем
                    if (i < cpCount) {
                        entries[i] = null;
                    }
                    i++;
                    continue;
                }
                case 6 -> { // Double (занимает два слота)
                    long high = in.readU4();
                    long low = in.readU4();
                    long bits = (high << 32) | (low & 0xFFFFFFFFL);
                    double value = Double.longBitsToDouble(bits);
                    entry = new CpDouble(tag, value);
                    entries[i] = entry;
                    i++;
                    if (i < cpCount) {
                        entries[i] = null;
                    }
                    i++;
                    continue;
                }
                case 7 -> { // Class
                    int nameIndex = in.readU2();
                    entry = new CpClass(tag, nameIndex);
                }
                case 8 -> { // String
                    int stringIndex = in.readU2();
                    entry = new CpString(tag, stringIndex);
                }
                case 9 -> { // Fieldref
                    int classIndex = in.readU2();
                    int nameAndTypeIndex = in.readU2();
                    entry = new CpFieldref(tag, classIndex, nameAndTypeIndex);
                }
                case 10 -> { // Methodref
                    int classIndex = in.readU2();
                    int nameAndTypeIndex = in.readU2();
                    entry = new CpMethodref(tag, classIndex, nameAndTypeIndex);
                }
                case 11 -> { // InterfaceMethodref
                    int classIndex = in.readU2();
                    int nameAndTypeIndex = in.readU2();
                    entry = new CpInterfaceMethodref(tag, classIndex, nameAndTypeIndex);
                }
                case 12 -> { // NameAndType
                    int nameIndex = in.readU2();
                    int descriptorIndex = in.readU2();
                    entry = new CpNameAndType(tag, nameIndex, descriptorIndex);
                }
                case 15 -> { // MethodHandle
                    int refKind = in.readU1();
                    int refIndex = in.readU2();
                    entry = new CpMethodHandle(tag, refKind, refIndex);
                }
                case 16 -> { // MethodType
                    int descriptorIndex = in.readU2();
                    entry = new CpMethodType(tag, descriptorIndex);
                }
                case 17 -> { // Dynamic
                    int bootstrapIndex = in.readU2();
                    int nameAndTypeIndex = in.readU2();
                    entry = new CpDynamic(tag, bootstrapIndex, nameAndTypeIndex);
                }
                case 18 -> { // InvokeDynamic
                    int bootstrapIndex = in.readU2();
                    int nameAndTypeIndex = in.readU2();
                    entry = new CpInvokeDynamic(tag, bootstrapIndex, nameAndTypeIndex);
                }
                case 19 -> { // Module
                    int nameIndex = in.readU2();
                    entry = new CpModule(tag, nameIndex);
                }
                case 20 -> { // Package
                    int nameIndex = in.readU2();
                    entry = new CpPackage(tag, nameIndex);
                }
                default -> throw new IOException("Unknown constant pool tag: " + tag +
                        " at index " + i);
            }
            entries[i] = entry;
            i++;
        }

        return new ConstantPool(entries);
    }

    private FieldInfo[] readFields(ClassFileInput in, ConstantPool cp) throws IOException {
        int count = in.readU2();
        FieldInfo[] result = new FieldInfo[count];
        for (int i = 0; i < count; i++) {
            int accessFlags = in.readU2();
            int nameIndex = in.readU2();
            int descriptorIndex = in.readU2();
            AttributeInfo[] attrs = readAttributes(in, cp);
            result[i] = new FieldInfo(accessFlags, nameIndex, descriptorIndex, attrs);
        }
        return result;
    }

    private MethodInfo[] readMethods(ClassFileInput in, ConstantPool cp) throws IOException {
        int count = in.readU2();
        MethodInfo[] result = new MethodInfo[count];
        for (int i = 0; i < count; i++) {
            int accessFlags = in.readU2();
            int nameIndex = in.readU2();
            int descriptorIndex = in.readU2();
            AttributeInfo[] attrs = readAttributes(in, cp);
            result[i] = new MethodInfo(accessFlags, nameIndex, descriptorIndex, attrs);
        }
        return result;
    }

    private AttributeInfo[] readAttributes(ClassFileInput in, ConstantPool cp) throws IOException {
        int count = in.readU2();
        AttributeInfo[] attrs = new AttributeInfo[count];
        for (int i = 0; i < count; i++) {
            attrs[i] = readSingleAttribute(in, cp);
        }
        return attrs;
    }

    private AttributeInfo readSingleAttribute(ClassFileInput in, ConstantPool cp) throws IOException {
        int nameIndex = in.readU2();
        long length = in.readU4();
        String name = cp.getUtf8(nameIndex);

        if ("Code".equals(name)) {
            int maxStack = in.readU2();
            int maxLocals = in.readU2();
            long codeLength = in.readU4();
            byte[] code = new byte[(int) codeLength];
            for (int i = 0; i < codeLength; i++) {
                code[i] = (byte) in.readU1();
            }

            // exception_table
            int exceptionTableLength = in.readU2();
            // просто пролистываем
            for (int i = 0; i < exceptionTableLength; i++) {
                in.readU2(); // start_pc
                in.readU2(); // end_pc
                in.readU2(); // handler_pc
                in.readU2(); // catch_type
            }

            LineNumberTableAttribute lnt = null;
            LocalVariableTableAttribute lvt = null;

            // attributes внутри Code
            int codeAttrsCount = in.readU2();
            for (int i = 0; i < codeAttrsCount; i++) {
                int subNameIndex = in.readU2();
                long subLen = in.readU4();
                String subName = cp.getUtf8(subNameIndex);

                if ("LineNumberTable".equals(subName)) {
                    int len = in.readU2();
                    List<LineNumberTableAttribute.Entry> entries = new ArrayList<>();
                    for (int j = 0; j < len; j++) {
                        int startPc = in.readU2();
                        int line = in.readU2();
                        entries.add(new LineNumberTableAttribute.Entry(startPc, line));
                    }
                    lnt = new LineNumberTableAttribute(entries);
                } else if ("LocalVariableTable".equals(subName)) {
                    lvt = readLocalVariableTable(in, cp, subLen);
                } else {
                    // просто пропускаем неизвестные вложенные атрибуты
                    for (long k = 0; k < subLen; k++) {
                        in.readU1();
                    }
                }
            }

            return new CodeAttribute(name, maxStack, maxLocals, code, lnt, lvt);
        } else if ("RuntimeVisibleAnnotations".equals(name)) {
            // читаем bytes целиком и разбираем уже в отдельном потоке
            byte[] data = new byte[(int) length];
            for (int i = 0; i < length; i++) {
                data[i] = (byte) in.readU1();
            }
            return readRuntimeVisibleAnnotationsAttribute(name, data, cp);
        } else if ("BootstrapMethods".equals(name)) {
            byte[] data = new byte[(int) length];
            for (int i = 0; i < length; i++) {
                data[i] = (byte) in.readU1();
            }
            return readBootstrapMethodsAttribute(name, data, cp);
        } else {
            // Пропускаем тело атрибута
            byte[] data = new byte[(int) length];
            for (int i = 0; i < length; i++) {
                data[i] = (byte) in.readU1();
            }
            return new RawAttribute(name, data);
        }
    }

    private LocalVariableTableAttribute readLocalVariableTable(ClassFileInput in, ConstantPool cp, long attrLength)
            throws IOException {
        int len = in.readU2();
        List<LocalVariableTableAttribute.Entry> entries = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            int startPc = in.readU2();
            int length = in.readU2();
            int nameIndex = in.readU2();
            int descIndex = in.readU2();
            int index = in.readU2();
            entries.add(new LocalVariableTableAttribute.Entry(startPc, length, nameIndex, descIndex, index));
        }
        return new LocalVariableTableAttribute(entries);
    }

    private RuntimeVisibleAnnotationsAttribute readRuntimeVisibleAnnotationsAttribute(String name, byte[] data, ConstantPool cp)
            throws IOException {
        try (ClassFileInput in = new ClassFileInput(new ByteArrayInputStream(data))) {
            int numAnnotations = in.readU2();
            AnnotationInfo[] annotations = new AnnotationInfo[numAnnotations];
            for (int i = 0; i < numAnnotations; i++) {
                annotations[i] = readAnnotationInfo(in, cp);
            }
            return new RuntimeVisibleAnnotationsAttribute(name, annotations);
        } catch (Exception e) {
            throw new IOException("Failed to parse RuntimeVisibleAnnotations", e);
        }
    }

    private BootstrapMethodsAttribute readBootstrapMethodsAttribute(String name, byte[] data, ConstantPool cp)
            throws IOException {
        try (ClassFileInput in = new ClassFileInput(new ByteArrayInputStream(data))) {
            int numBootstrapMethods = in.readU2();
            BootstrapMethodsAttribute.BootstrapMethod[] methods
                    = new BootstrapMethodsAttribute.BootstrapMethod[numBootstrapMethods];

            for (int i = 0; i < numBootstrapMethods; i++) {
                int methodRef = in.readU2();        // CONSTANT_MethodHandle
                int numArgs = in.readU2();
                int[] args = new int[numArgs];
                for (int j = 0; j < numArgs; j++) {
                    args[j] = in.readU2();            // индексы в constant pool
                }
                methods[i] = new BootstrapMethodsAttribute.BootstrapMethod(methodRef, args);
            }

            return new BootstrapMethodsAttribute(name,methods);
        } catch (Exception e) {
            // fail-soft, чтобы не завалить все чтение class-файла
            return new BootstrapMethodsAttribute(name, new BootstrapMethodsAttribute.BootstrapMethod[0]);
        }
    }

    private AnnotationInfo readAnnotationInfo(ClassFileInput in, ConstantPool cp) throws IOException {
        int typeIndex = in.readU2();
        String descriptor = cp.getUtf8(typeIndex);
        int numElementValuePairs = in.readU2();

        // Специализированная обработка @kotlin.Metadata
        if ("Lkotlin/Metadata;".equals(descriptor)) {
            return readKotlinMetadataAnnotation(descriptor, numElementValuePairs, in, cp);
        }

        // Для всех остальных аннотаций просто пропускаем пары и возвращаем SimpleAnnotationInfo
        for (int i = 0; i < numElementValuePairs; i++) {
            int elementNameIndex = in.readU2(); // элемент нам не интересен
            skipElementValue(in);
        }

        return new SimpleAnnotationInfo(descriptor);
    }

    private KotlinMetadataAnnotationInfo readKotlinMetadataAnnotation(String descriptor, int numElementValuePairs, ClassFileInput in, ConstantPool cp)
            throws IOException {
        int kind = 1;
        int[] mv = null;
        int[] bv = null;
        String[] d1 = null;
        String[] d2 = null;
        String xs = null;
        int xi = 0;

        for (int i = 0; i < numElementValuePairs; i++) {
            int elementNameIndex = in.readU2();
            String name = cp.getUtf8(elementNameIndex);

            switch (name) {
                case "k" -> kind = readIntElementValue(in, cp);
                case "mv" -> mv = readIntArrayElementValue(in, cp);
                case "bv" -> bv = readIntArrayElementValue(in, cp);
                case "d1" -> d1 = readStringArrayElementValue(in, cp);
                case "d2" -> d2 = readStringArrayElementValue(in, cp);
                case "xs" -> xs = readStringElementValue(in, cp);
                case "pn" -> {
                    // packageName - можно сохранить в xs или просто пропустить; пока пропустим
                    skipElementValue(in);
                }
                default -> {
                    // неизвестное поле аннотации - аккуратно пропускаем
                    skipElementValue(in);
                }
            }
        }

        return new KotlinMetadataAnnotationInfo(descriptor, kind, mv, bv, d1, d2, xs, xi);
    }

    private int readIntElementValue(ClassFileInput in, ConstantPool cp) throws IOException {
        int tag = in.readU1();
        if (tag != 'I') {
            throw new IOException("Expected int element_value with tag 'I', got: " + (char) tag);
        }
        int constIndex = in.readU2();
        return cp.getInteger(constIndex);
    }

    private int[] readIntArrayElementValue(ClassFileInput in, ConstantPool cp) throws IOException {
        int tag = in.readU1();
        if (tag != '[') {
            throw new IOException("Expected array element_value with tag '[', got: " + (char) tag);
        }
        int numValues = in.readU2();
        int[] result = new int[numValues];
        for (int i = 0; i < numValues; i++) {
            result[i] = readIntElementValue(in, cp);
        }
        return result;
    }

    private String readStringElementValue(ClassFileInput in, ConstantPool cp) throws IOException {
        int tag = in.readU1();
        if (tag != 's') {
            throw new IOException("Expected string element_value with tag 's', got: " + (char) tag);
        }
        int constIndex = in.readU2();
        String s = cp.getUtf8(constIndex);
        if (s == null) {
            throw new IOException("UTF8 at index " + constIndex + " is null");
        }
        return s;
    }

    private String[] readStringArrayElementValue(ClassFileInput in, ConstantPool cp) throws IOException {
        int tag = in.readU1();
        if (tag != '[') {
            throw new IOException("Expected array element_value with tag '[', got: " + (char) tag);
        }
        int numValues = in.readU2();
        String[] result = new String[numValues];
        for (int i = 0; i < numValues; i++) {
            result[i] = readStringElementValue(in, cp);
        }
        return result;
    }

    private void skipElementValue(ClassFileInput in) throws IOException {
        int tag = in.readU1();
        switch (tag) {
            case 'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z', 's' -> {
                // const value_index
                in.readU2();
            }
            case 'e' -> {
                // enum_const_value: type_name_index, const_name_index
                in.readU2();
                in.readU2();
            }
            case 'c' -> {
                // class_info_index
                in.readU2();
            }
            case '@' -> {
                // nested annotation
                skipAnnotation(in);
            }
            case '[' -> {
                int numValues = in.readU2();
                for (int i = 0; i < numValues; i++) {
                    skipElementValue(in);
                }
            }
            default -> throw new IOException("Unknown element_value tag: " + (char) tag);
        }
    }

    private void skipAnnotation(ClassFileInput in) throws IOException {
        // type_index уже будет прочитан в readAnnotationInfo, но здесь
        // это используется только для вложенных аннотаций.
        int typeIndex = in.readU2();
        int numElementValuePairs = in.readU2();
        for (int i = 0; i < numElementValuePairs; i++) {
            int elementNameIndex = in.readU2();
            skipElementValue(in);
        }
    }
}
