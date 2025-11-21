package net.letsdank.jd.ast.fixture;

import net.letsdank.jd.ast.JavaPrettyPrinter;
import net.letsdank.jd.ast.MethodAst;
import net.letsdank.jd.ast.MethodDecompiler;
import net.letsdank.jd.fixtures.SimpleMethods;
import net.letsdank.jd.io.ClassFileReader;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.MethodInfo;
import net.letsdank.jd.utils.JDUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class MethodDecompilerFieldTest {

    @Test
    void setValueProducesFieldAssignment()throws IOException {
        InputStream in = SimpleMethods.class.getResourceAsStream("SimpleMethods.class");
        assertNotNull(in,"SimpleMethods.class resource must be available");

        ClassFileReader reader = new ClassFileReader();
        ClassFile cf = reader.read(in);
        ConstantPool cp = cf.constantPool();

        MethodInfo setValue = JDUtils.findMethod(cf,cp,"setValue","(I)V");

        MethodDecompiler decompiler=new MethodDecompiler();
        MethodAst ast = decompiler.decompile(setValue,cf);

        JavaPrettyPrinter printer = new JavaPrettyPrinter();
        String javaText = printer.printMethod(cf,setValue,ast);

        System.out.println("setValue decompiled:\n"+javaText);

        // оиждаем this.value = x;
        assertTrue(javaText.contains("this.value") && javaText.contains("="),
                "Decompiled setValue(I)V must assign to this.value");
    }
}
