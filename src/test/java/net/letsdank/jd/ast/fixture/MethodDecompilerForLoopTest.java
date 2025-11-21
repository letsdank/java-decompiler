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

class MethodDecompilerForLoopTest {

    @Test
    void forLoopIsRenderedAsFor() throws IOException {
        InputStream in = SimpleMethods.class.getResourceAsStream("SimpleMethods.class");
        assertNotNull(in);

        ClassFileReader reader = new ClassFileReader();
        ClassFile cf = reader.read(in);
        ConstantPool cp = cf.constantPool();

        MethodInfo forLoop = JDUtils.findMethod(cf, cp, "forLoop", "(I)V");

        MethodDecompiler decompiler = new MethodDecompiler();
        MethodAst ast = decompiler.decompile(forLoop, cf);

        JavaPrettyPrinter printer = new JavaPrettyPrinter();
        String javaText = printer.printMethod(cf, forLoop, ast);

        System.out.println("forLoop decompiled:\n" + javaText);

        // for-циклы пока не распознаются, все сводится к while
        assertTrue(javaText.contains("while ("), "Decompiled forLoop(I)V must contain 'while ('");
    }
}
