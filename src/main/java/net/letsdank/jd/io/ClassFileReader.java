package net.letsdank.jd.io;

import net.letsdank.jd.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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

            return new ClassFile(minor, major, cp, accessFlags, thisClassIndex, superClassIndex, interfaces);
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
            entries[i]=entry;
            i++;
        }

        return new ConstantPool(entries);
    }
}
