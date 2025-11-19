package net.letsdank.jd.io;

import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.ConstantPool;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassFileReaderTest {
    @Test
    void canReadOwnClassFile() throws IOException {
        // Берем .class этого же теста
        InputStream in = ClassFileReaderTest.class
                .getResourceAsStream("ClassFileReaderTest.class");
        assertNotNull(in, "Failed to load own .class as resource");

        ClassFileReader reader = new ClassFileReader();
        ClassFile cf = reader.read(in);

        // Версия должна быть разумной
        assertTrue(cf.majorVersion() >= 45, "Unexpected major version: " + cf.majorVersion());
        assertTrue(cf.minorVersion() >= 0);

        ConstantPool cp = cf.constantPool();
        assertNotNull(cp, "Constant pool must not be null");
        assertTrue(cp.size() > 1, "Constant pool must have at least one entry");

        // Имя класса в виде net.letsdank.jd.io.ClassFileReaderTest
        String fqn = cf.thisClassFqn();
        assertTrue(fqn.endsWith("ClassFileReaderTest"), "Unexpected thisClassFqn: " + fqn);

        // В пуле должен быть internal-name вида net/letsdank/jd/io/ClassFileReaderTest
        String internal = fqn.replace('.', '/');
        assertTrue(cp.containsUtf8(internal), "Constant pool must contain Utf8 for internal name: " + internal);
    }
}