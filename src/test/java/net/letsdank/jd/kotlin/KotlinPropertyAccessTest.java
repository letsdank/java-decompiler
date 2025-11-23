package net.letsdank.jd.kotlin;

import net.letsdank.jd.ast.MethodAst;
import net.letsdank.jd.ast.MethodDecompiler;
import net.letsdank.jd.fixtures.SampleService;
import net.letsdank.jd.fixtures.User;
import net.letsdank.jd.io.ClassFileReader;
import net.letsdank.jd.lang.Language;
import net.letsdank.jd.lang.LanguageBackend;
import net.letsdank.jd.lang.LanguageBackends;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.MethodInfo;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для проверки, что:
 * - свойства Kotlin-классов попадают в KotlinPropertyRegistry;
 * - вызов User.getName() в SampleService.greet печатается как user.name.
 */
class KotlinPropertyAccessTest {

    /**
     * Проверяем, что при декомпиляции Kotlin data class User
     * его свойства (id, name) попадают в KotlinPropertyRegistry
     * под ключом internal name "net/letsdank/jd/fixtures/User".
     */
    @Test
    void userPropertiesAreRegisteredInRegistry() throws Exception {
        try (InputStream in = User.class.getResourceAsStream("User.class")) {
            assertNotNull(in, "User.class resource not found");

            ClassFileReader reader = new ClassFileReader();
            ClassFile cf = reader.read(in);

            // Берем Kotlin-бэкенд напрямую
            LanguageBackend backend = LanguageBackends.forLanguage(Language.KOTLIN);
            MethodDecompiler decompiler = new MethodDecompiler();

            // Декомпилируем любой метод, чтобы сработал decompileMethod(...)
            MethodInfo[] methods = cf.methods();
            assertTrue(methods.length > 0, "User should have at least one method");

            MethodInfo m = methods[0];
            MethodAst ast = decompiler.decompile(m, cf);
            backend.decompileMethod(cf, m, ast);
        }

        // внутреннее имя берем такое же, как в javap: net/letsdank/jd/fixtures/User
        String ownerInternal = "net/letsdank/jd/fixtures/User";

        System.out.println("Registry for " + ownerInternal + " = " +
                KotlinPropertyRegistry.getProperties(ownerInternal));

        assertTrue(KotlinPropertyRegistry.hasProperty(ownerInternal, "id"),
                "User.id should be registered in KotlinPropertyRegistry");
        assertTrue(KotlinPropertyRegistry.hasProperty(ownerInternal, "name"),
                "User.name should be registered in KotlinPropertyRegistry");
    }

    /**
     * Проверяем, что метод SampleService.greet(User): String
     * печатается с использованием property-доступа user.name, а не user.getName().
     * <p>
     * Это интеграционный тест всей цепочки:
     * - ExpressionBuilder проставляет ownerInternalName = "net/letsdank/jd/fixtures/User"
     * - KotlinPropertyRegistry знает, что у User есть property "name"
     * - KotlinPrettyPrinter.tryPrintPropertyAccess превращает getName() -> .name
     * - buildKotlinStringTemplate печатает "$greeting, ${user.name}!"
     */
    @Test
    void sampleServiceGreetUsesPropertyAccessForUserName() throws Exception {
        // Сначала убеждаемся, что User зарегистрирован в реестре
        // (дублируем логику из предыдущего теста, чтобы не зависеть от порядка выполнения тестов)
        try (InputStream in = User.class.getResourceAsStream("User.class")) {
            assertNotNull(in, "User.class resource not found");

            ClassFileReader reader = new ClassFileReader();
            ClassFile cf = reader.read(in);

            // Берем Kotlin-бэкенд напрямую
            LanguageBackend backend = LanguageBackends.forLanguage(Language.KOTLIN);
            MethodDecompiler decompiler = new MethodDecompiler();

            // Декомпилируем любой метод, чтобы сработал decompileMethod(...)
            MethodInfo[] methods = cf.methods();
            assertTrue(methods.length > 0, "User should have at least one method");

            MethodInfo m = methods[0];
            MethodAst ast = decompiler.decompile(m, cf);
            backend.decompileMethod(cf, m, ast);
        }

        String ownerInternal = "net/letsdank/jd/fixtures/User";
        System.out.println("After decompile(User.*): registry = " +
                KotlinPropertyRegistry.getProperties(ownerInternal));

        // Теперь декомпилируем SampleService целиком
        try (InputStream in = SampleService.class.getResourceAsStream("SampleService.class")) {
            assertNotNull(in, "SampleService.class resource not found");

            ClassFileReader reader = new ClassFileReader();
            ClassFile cf = reader.read(in);

            MethodDecompiler decompiler = new MethodDecompiler();
            LanguageBackend backend = LanguageBackends.forLanguage(Language.KOTLIN);

            String src = backend.decompileClass(cf, decompiler);

            System.out.println("=== Decompiled SampleService ===");
            System.out.println(src);

            // sanity: класс вообще декомпилировался
            assertTrue(src.contains("class SampleService"), "class SampleService should be present");
            assertTrue(src.contains("fun greet("), "greet() function should be present");

            // Проверяем, что где-то внутри класса есть обращение к ".name"
            // и при этом нет вызова ".getName(" - т.е. getName() был заменен на свойство.
            assertTrue(src.contains(".name"), "decompiled SampleService source should contain property access '.name'");
            assertFalse(src.contains(".getName("), "decompiled SampleService source should not contain '.getName(' (should use property access instead)");
        }
    }
}
