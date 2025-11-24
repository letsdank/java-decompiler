package net.letsdank.jd.ast.fixture;

import net.letsdank.jd.ast.JavaPrettyPrinter;
import net.letsdank.jd.ast.MethodAst;
import net.letsdank.jd.ast.MethodDecompiler;
import net.letsdank.jd.fixtures.ArrayFixtures;
import net.letsdank.jd.io.ClassFileReader;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.MethodInfo;
import net.letsdank.jd.utils.JDUtils;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class MethodDecompilerArrayTest {

    @Test
    void testSumArrayDecompilesToIndexing() throws Exception {
        try (InputStream in = ArrayFixtures.class.getResourceAsStream("ArrayFixtures.class")) {
            ClassFileReader reader = new ClassFileReader();
            ClassFile cf = reader.read(in);
            ConstantPool cp = cf.constantPool();

            MethodInfo sum = JDUtils.findMethod(cf,cp,"sum","([I)I");
            MethodDecompiler decompiler = new MethodDecompiler();
            MethodAst ast = decompiler.decompile(sum,cf);

            JavaPrettyPrinter printer = new JavaPrettyPrinter();
            String source = printer.printMethod(cf,sum,ast);

            System.out.println("sum([I)I decompiled:\n"+source);

            assertTrue(source.contains("arr[i]"), "sum([I)I must use arr[i]");
            assertTrue(source.contains("new int[") || source.contains("arr.length"),
                    "sum([I)I must use array length and/or array creation");
        }
    }
}
