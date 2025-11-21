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

class MethodDecompilerInvokeVirtualTest {

    @Test
    void printHelloProducesSystemOutPrintlnCall() throws IOException {
        InputStream in = SimpleMethods.class.getResourceAsStream("SimpleMethods.class");
        assertNotNull(in);

        ClassFileReader reader = new ClassFileReader();
        ClassFile cf = reader.read(in);
        ConstantPool cp = cf.constantPool();

        MethodInfo printHello = JDUtils.findMethod(cf, cp, "printHello", "()V");

        MethodDecompiler decompiler = new MethodDecompiler();
        MethodAst ast = decompiler.decompile(printHello, cf);

        JavaPrettyPrinter printer = new JavaPrettyPrinter();
        String javaText = printer.printMethod(cf, printHello, ast);

        System.out.println("printHello decompiled:\n" + javaText);

        assertTrue(javaText.contains("System.out.println"),
                "Decompiled printHello()V must contain System.out.println");
    }
}
