package net.letsdank.jd.kotlin;

import kotlin.metadata.KmClass;
import kotlin.metadata.KmProperty;
import kotlin.metadata.jvm.KotlinClassMetadata;
import net.letsdank.jd.fixtures.User;
import net.letsdank.jd.io.ClassFileReader;
import net.letsdank.jd.model.ClassFile;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class KotlinMetadataSmokeTest {

    @Test
    void userMetadataContainsProperties() throws Exception {
        try (InputStream in = User.class.getResourceAsStream("User.class")) {
            assertNotNull(in, "User.class resource not found");

            ClassFileReader reader = new ClassFileReader();
            ClassFile cf = reader.read(in);

            // Используем нашу обертку над метаданными + fallback
            Set<String> props = KotlinMetadataReader.readEffectivePropertyNames(cf);

            System.out.println("Effective Kotlin properties for User: " + props);

            assertFalse(props.isEmpty(), "User should have at least one effective property");
            assertTrue(props.contains("id"), "User should have property 'id'");
            assertTrue(props.contains("name"), "User should have property 'name'");
        }
    }
}
