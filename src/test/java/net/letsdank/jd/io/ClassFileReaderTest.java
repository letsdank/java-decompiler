package net.letsdank.jd.io;

import net.letsdank.jd.fixtures.SimpleMethods;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.attribute.CodeAttribute;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.MethodInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassFileReaderTest {
    @Test
    void canReadOwnClassFile() throws IOException {
        // Берем .class фикстуры
        InputStream in = SimpleMethods.class.getResourceAsStream("SimpleMethods.class");
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
        assertTrue(fqn.endsWith("SimpleMethods"), "Unexpected thisClassFqn: " + fqn);

        // В пуле должен быть internal-name вида net/letsdank/jd/io/ClassFileReaderTest
        String internal = fqn.replace('.', '/');
        assertTrue(cp.containsUtf8(internal), "Constant pool must contain Utf8 for internal name: " + internal);
    }

    @Test
    void hasMethodWithCodeAttribute() throws IOException {
        // Берем .class фикстуры
        InputStream in = SimpleMethods.class.getResourceAsStream("SimpleMethods.class");
        assertNotNull(in, "Failed to load own .class as resource");

        ClassFileReader reader = new ClassFileReader();
        ClassFile cf = reader.read(in);

        ConstantPool cp = cf.constantPool();
        MethodInfo[] methods = cf.methods();
        assertTrue(methods.length > 0, "Class must have methods");

        // Ищем метод "hasMethodWithCodeAttribute"
        MethodInfo target = Arrays.stream(methods)
                .filter(m -> "add".equals(cp.getUtf8(m.nameIndex())))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Method not found"));

        CodeAttribute code = target.findCodeAttribute();
        assertNotNull(code, "Method must have Code attribute");
        assertTrue(code.code().length > 0, "Bytecode length must be > 0");
        assertTrue(code.maxStack() > 0, "maxStack should be > 0");
    }

    @Test
    void methodsAndFieldsArePresent() throws IOException {
        InputStream in = SimpleMethods.class.getResourceAsStream("SimpleMethods.class");
        assertNotNull(in, "Failed to load own SimpleMethods.class");

        ClassFileReader reader = new ClassFileReader();
        ClassFile cf = reader.read(in);
        ConstantPool cp = cf.constantPool();

        for (var m : cf.methods()) {
            String name = cp.getUtf8(m.nameIndex());
            // Конструктор инициализации тоже имеет Code
            if (!"<init>".equals(name) && !"printHello".equals(name)
                    && !"add".equals(name) && !"abs".equals(name)) {
                continue; // Другие методы фикстуры можно игнорировать
            }

            assertNotNull(m.findCodeAttribute(), "Method " + name + " must have Code attribute");
        }
    }
}