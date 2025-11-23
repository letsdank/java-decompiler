package net.letsdank.jd.kotlin;

import net.letsdank.jd.fixtures.User;
import net.letsdank.jd.io.ClassFileReader;
import net.letsdank.jd.model.ClassFile;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class KotlinMetadataSmokeTest {

    @Test
    void userMetadataContainsProperties() throws Exception {
        try (InputStream in = User.class.getResourceAsStream("User.class")) {
            assertNotNull(in, "User.class resource not found");

            ClassFileReader reader = new ClassFileReader();
            ClassFile cf = reader.read(in);

            KotlinMetadataReader.KotlinClassModel model = KotlinMetadataReader.readClassModel(cf);

            System.out.println("User KotlinClassModel: " + model);

            assertFalse(model.isKotlinClass(), "User should be recognized as Kotlin class");
            assertTrue(model.propertyNames().contains("id"), "User should have property 'id'");
            assertTrue(model.propertyNames().contains("name"), "User should have property 'name'");
        }
    }
}
