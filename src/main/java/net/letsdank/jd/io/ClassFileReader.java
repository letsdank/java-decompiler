package net.letsdank.jd.io;

import net.letsdank.jd.model.ClassFile;

import java.io.IOException;
import java.io.InputStream;

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

            // Здесь позже будет чтение constant pool и остального
            return new ClassFile(minor, major);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
