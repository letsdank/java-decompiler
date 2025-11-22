package net.letsdank.jd.kotlin;

import net.letsdank.jd.ast.MethodDecompiler;
import net.letsdank.jd.fixtures.SampleService;
import net.letsdank.jd.io.ClassFileReader;
import net.letsdank.jd.lang.LanguageBackend;
import net.letsdank.jd.lang.LanguageBackends;
import net.letsdank.jd.model.ClassFile;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class KotlinDecompileClassTest {

    @Test
    void sampleService_basicStructureIsCorrect() throws Exception {
        // берем .class у уже скомпилированной фикстуры
        Class<?> clazz = SampleService.class;
        String resourcePath = clazz.getName().replace('.', '/') + ".class";

        ClassLoader cl = clazz.getClassLoader();
        try (InputStream in = cl.getResourceAsStream(resourcePath)) {
            assertNotNull(in, "Can't find resource: " + resourcePath);

            ClassFileReader reader = new ClassFileReader();
            ClassFile cf = reader.read(in);

            LanguageBackend backend = LanguageBackends.autoDetect(cf);
            MethodDecompiler decompiler = new MethodDecompiler();

            String src = backend.decompileClass(cf, decompiler);
            System.out.println("=== Decompiled SampleService ===\n" + src);

            // Проверяем общую форму
            assertTrue(src.contains("package net.letsdank.jd.fixtures"), "package should be present");
            assertTrue(src.contains("class SampleService"), "class name should be present");
            // greeting как property (в идеале)
            assertTrue(src.contains("greeting"), "greeting property should be present");

            // greet(User): String - хотя бы сигнатура
            assertTrue(src.contains("fun greet("), "greet() function should be present");
            assertTrue(src.contains("User"), "User parameter type should be present");
            assertTrue(src.contains(": String"), "greet() return type should be String");
        }
    }
}
