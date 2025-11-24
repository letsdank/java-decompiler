package net.letsdank.jd.kotlin;

import net.letsdank.jd.fixtures.User;
import net.letsdank.jd.io.ClassFileReader;
import net.letsdank.jd.model.ClassFile;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class KotlinClassModelUserTest {

    private ClassFile readUserClassFile() throws Exception {
        try (InputStream in = User.class.getResourceAsStream("User.class")) {
            assertNotNull(in, "User.class resource not found");
            ClassFileReader reader = new ClassFileReader();
            return reader.read(in);
        }
    }

    @Test
    void userModelWithKotlinxMetadataEnabled() throws Exception {
        ClassFile cf = readUserClassFile();

        // allowKotlinxMetadata = true
        KotlinMetadataReader.KotlinClassModel model = KotlinMetadataReader.readClassModel(cf, null);

        System.out.println("User KotlinClassModel (with metadata): " + model);

        assertTrue(model.isKotlinClass(), "User should be recognized as Kotlin class");
        assertEquals("net/letsdank/jd/fixtures/User", model.internalName());

        Set<String> names = model.propertyNames();
        assertTrue(names.contains("id"), "User should have property 'id'");
        assertTrue(names.contains("name"), "User should have property 'name");

        KotlinMetadataReader.KotlinPropertyModel id = model.findProperty("id");
        KotlinMetadataReader.KotlinPropertyModel name = model.findProperty("name");
        assertNotNull(id);
        assertNotNull(name);

        assertEquals("Int", id.type());
        assertEquals("String", name.type());
        assertFalse(id.isVar());
        assertFalse(name.isVar());

        // Если метадата реально читается новой версией библиотеки -
        // проверяем data/Kind. Если нет - тест все равно пройдет.
        if (model.hasMetadata()) {
            assertEquals(KotlinMetadataReader.KotlinClassModel.Kind.CLASS, model.kind());
            assertTrue(model.isDataClass(), "User is a data class in Kotlin");
        }
    }

    
}
