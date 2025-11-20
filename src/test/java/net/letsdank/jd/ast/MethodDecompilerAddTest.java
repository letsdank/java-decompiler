package net.letsdank.jd.ast;

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

class MethodDecompilerAddTest {
    @Test
    void decompileAdd() throws IOException {
        // Загружаем .class SimpleMethods
        InputStream in = SimpleMethods.class.getResourceAsStream("SimpleMethods.class");
        assertNotNull(in, "Failed to load SimpleMethods.class");

        ClassFileReader reader = new ClassFileReader();
        ClassFile cf = reader.read(in);
        ConstantPool cp = cf.constantPool();

        MethodInfo add = JDUtils.findMethod(cf, cp, "add", "(II)I");

        MethodDecompiler decompiler = new MethodDecompiler();
        MethodAst addAst = decompiler.decompile(add, cf);

        System.out.println("add AST: " + addAst);

        // Простейшие sanity-проверки
        assertTrue(addAst.toString().contains("+"), "add AST should contain '+'");
    }
}