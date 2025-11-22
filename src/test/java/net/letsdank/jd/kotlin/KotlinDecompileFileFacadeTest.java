package net.letsdank.jd.kotlin;

import net.letsdank.jd.ast.MethodDecompiler;
import net.letsdank.jd.io.ClassFileReader;
import net.letsdank.jd.lang.Language;
import net.letsdank.jd.lang.LanguageBackend;
import net.letsdank.jd.lang.LanguageBackends;
import net.letsdank.jd.model.ClassFile;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тест декомпиляции Kotlin file facade (SampleKotlinKt):
 * - ожидаем, что бэкенд будет Kotlin
 * - что в выводе есть пакет и top-level функция topLevelSum
 */
class KotlinDecompileFileFacadeTest {
    @Test
    void topLevelSum_isDecompiledAsKotlin() throws Exception {
        String className = "net.letsdank.jd.fixtures.SampleKotlinKt";
        String resourcePath = className.replace('.', '/') + ".class";

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream in = cl.getResourceAsStream(resourcePath)) {
            assertNotNull(in, "Can't find resource: " + resourcePath);

            ClassFileReader reader = new ClassFileReader();
            ClassFile cf = reader.read(in);

            LanguageBackend backend = LanguageBackends.autoDetect(cf);
            assertEquals(Language.KOTLIN, backend.language(),
                    "File facade should be decompiled with Kotlin backend");

            MethodDecompiler decompiler = new MethodDecompiler();
            String src = backend.decompileClass(cf, decompiler);

            System.out.println("=== Decompiled SampleKotlinKt ===");
            System.out.println(src);

            // sanity-check по структуре
            assertTrue(src.contains("package net.letsdank.jd.fixtures"), "package should be present");
            assertTrue(src.contains("fun topLevelSum("), "topLevelSum should be present");
            assertTrue(src.contains("a: Int"), "parameter a type should be Int");
            assertTrue(src.contains("b: Int"), "parameter b type should be Int");
            assertTrue(src.contains(": Int"), "return type Int should be present");
        }
    }
}
