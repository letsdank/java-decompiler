package net.letsdank.jd.io;

import net.letsdank.jd.model.ClassFile;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

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
    }
}