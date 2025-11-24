package net.letsdank.jd.ast.fixture;

import net.letsdank.jd.ast.JavaPrettyPrinter;
import net.letsdank.jd.ast.MethodAst;
import net.letsdank.jd.ast.MethodDecompiler;
import net.letsdank.jd.fixtures.TypeFixtures;
import net.letsdank.jd.io.ClassFileReader;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.MethodInfo;
import net.letsdank.jd.utils.JDUtils;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class MethodDecompilerTypeTest {

    @Test
    void testInstanceOfAndCast() throws Exception {
        try (InputStream in = TypeFixtures.class.getResourceAsStream("TypeFixtures.class")) {
            ClassFileReader reader = new ClassFileReader();
            ClassFile cf = reader.read(in);
            ConstantPool cp = cf.constantPool();

            MethodInfo isString = JDUtils.findMethod(cf, cp, "isString", "(Ljava/lang/Object;)Z");
            MethodInfo castToNumber = JDUtils.findMethod(cf, cp, "castToNumber",
                    "(Ljava/lang/Object;)Ljava/lang/Number;");

            MethodDecompiler decompiler = new MethodDecompiler();
            JavaPrettyPrinter printer = new JavaPrettyPrinter();

            MethodAst astIsString = decompiler.decompile(isString, cf);
            String sourceIsString = printer.printMethod(cf, isString, astIsString);
            System.out.println("isString decompiled:\n" + sourceIsString);

            assertTrue(sourceIsString.contains("instanceof String"),
                    "isString(Object) must contain 'instanceof String'");

            MethodAst astCast = decompiler.decompile(castToNumber,cf);
            String sourceCast = printer.printMethod(cf,castToNumber,astCast);
            System.out.println("castToNumber decompiled:\n"+sourceCast);

            assertTrue(sourceCast.contains("(Number)"),
                    "castToNumber(Object) must contain '(Number)' cast");
        }
    }
}
