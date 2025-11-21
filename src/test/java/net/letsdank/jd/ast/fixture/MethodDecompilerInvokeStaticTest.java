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

class MethodDecompilerInvokeStaticTest {

    @Test
    void staticCallProducesMathAbsCall() throws IOException {
        InputStream in = SimpleMethods.class.getResourceAsStream("SimpleMethods.class");
        assertNotNull(in);

        ClassFileReader reader = new ClassFileReader();
        ClassFile cf = reader.read(in);
        ConstantPool cp = cf.constantPool();

        MethodInfo staticCall = JDUtils.findMethod(cf, cp, "staticCall", "()V");

        MethodDecompiler decompiler = new MethodDecompiler();
        MethodAst ast = decompiler.decompile(staticCall, cf);

        JavaPrettyPrinter printer = new JavaPrettyPrinter();
        String javaText = printer.printMethod(cf, staticCall, ast);

        System.out.println("staticCall decompiled:\n" + javaText);

        // ожидаем Math.abs(...) в исходнике
        assertTrue(javaText.contains("Math.abs"), "Decompiled staticCall()V must contain Math.abs");
    }
}
