package net.letsdank.jd.kotlin;

import net.letsdank.jd.fixtures.User;
import net.letsdank.jd.io.ClassFileReader;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.annotation.AnnotationInfo;
import net.letsdank.jd.model.attribute.AttributeInfo;
import net.letsdank.jd.model.attribute.RuntimeVisibleAnnotationsAttribute;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class KotlinAnnotationsIntrospectionTest {

    @Test
    void userClassHasRuntimeVisibleKotlinMetadataAnnotation() throws Exception {
        try (InputStream in = User.class.getResourceAsStream("User.class")) {
            assertNotNull(in, "User.class resource not found");

            ClassFileReader reader = new ClassFileReader();
            ClassFile cf = reader.read(in);

            AttributeInfo[] attrs = cf.attributes();
            assertNotNull(attrs, "Class attributes must not be null");

            System.out.println("Class attributes for " + cf.thisClassInternalName() + ":");
            boolean foundRuntimeVisible = false;
            boolean foundKotlinMetadata = false;

            for (AttributeInfo attr : attrs) {
                System.out.println("  attr type = " + attr.getClass().getName());
                if(attr instanceof RuntimeVisibleAnnotationsAttribute rva) {
                    foundRuntimeVisible = true;
                    for(AnnotationInfo ann : rva.annotations()) {
                        System.out.println("    ann = " + ann.descriptor() +
                                " (" + ann.getClass().getSimpleName() + ")");
                        if("Lkotlin/Metadata;".equals(ann.descriptor())) {
                            foundKotlinMetadata = true;
                        }
                    }
                }
            }

            assertTrue(foundRuntimeVisible, "Class should have RuntimeVisibleAnnotations attribute");
            assertTrue(foundKotlinMetadata, "Class should have Lkotlin/Metadata; annotation");
        }
    }
}
