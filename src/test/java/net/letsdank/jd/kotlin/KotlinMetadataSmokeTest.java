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

import static org.junit.jupiter.api.Assertions.*;

class KotlinMetadataSmokeTest {

    @Test
    void userMetadataContainsProperties() throws Exception {
        try (InputStream in = User.class.getResourceAsStream("User.class")) {
            assertNotNull(in, "User.class resource not found");

            ClassFileReader reader = new ClassFileReader();
            ClassFile cf = reader.read(in);

            KotlinClassMetadata metadata = KotlinMetadataExtractor.extractFromClassFile(cf);
            assertNotNull(metadata, "Kotlin metadata for User should not be null");
            assertInstanceOf(KotlinClassMetadata.Class.class, metadata, "User metadata should be KotlinClassMetadata.Class");

            KmClass kmClass = ((KotlinClassMetadata.Class) metadata).getKmClass();
            List<KmProperty> props = kmClass.getProperties();

            System.out.println("KmClass properties for User: " + props.size());
            for (KmProperty p : props) {
                System.out.println("  prop: " + p.getName());
            }

            assertFalse(props.isEmpty(), "KmClass.getProperties() for User should not be empty");
            assertTrue(props.stream().anyMatch(p -> p.getName().equals("id")),
                    "KmClass should contain property 'id'");
            assertTrue(props.stream().anyMatch(p -> p.getName().equals("name")),
                    "KmClass should contain property 'name'");
        }
    }
}
