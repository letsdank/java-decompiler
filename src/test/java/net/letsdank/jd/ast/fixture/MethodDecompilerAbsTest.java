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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodDecompilerAbsTest {
    @Test
    void decompileAbsProducesNonInvertedCondition() throws IOException {
        InputStream in = SimpleMethods.class.getResourceAsStream("SimpleMethods.class");
        assertNotNull(in);

        ClassFileReader reader = new ClassFileReader();
        ClassFile cf = reader.read(in);
        ConstantPool cp = cf.constantPool();

        MethodInfo abs = JDUtils.findMethod(cf, cp, "abs", "(I)I");

        MethodDecompiler decompiler = new MethodDecompiler();
        MethodAst ast = decompiler.decompile(abs, cf);

        JavaPrettyPrinter printer = new JavaPrettyPrinter();
        String javaText = printer.printMethod(cf, abs, ast);

        System.out.println("abs decompiled:\n" + javaText);

        // 1. Есть if
        assertTrue(javaText.contains("if ("), "abs must contain if");

        // 2. Условие выглядит как a >= 0 (или x >= 0)
        assertTrue(javaText.contains(">= 0"), "abs condition should be >= 0, got: " + javaText);

        // 3. then-ветка возвращает положительное значение, else - отрицательное
        // упрощенная проверка: порядок return'ов
        int idxIf = javaText.indexOf("if (");
        int idxThenReturn = javaText.indexOf("return", idxIf);
        int idxElseReturn = javaText.indexOf("return", idxThenReturn + 1);

        assertTrue(idxThenReturn != -1 && idxElseReturn != -1, "expected two return statements in if/else");

        String thenLine = extractLine(javaText, idxThenReturn);
        String elseLine = extractLine(javaText, idxElseReturn);

        assertTrue(thenLine.contains("return a") || thenLine.contains("return x"),
                "then-branch should return x/a: " + thenLine);
        assertTrue(elseLine.contains("return -"), "else-branch should return negative: " + elseLine);
    }

    private static String extractLine(String text, int pos) {
        int start = text.lastIndexOf('\n', pos);
        int end = text.indexOf('\n', pos);
        if (start < 0) start = 0;
        if (end < 0) end = text.length();
        return text.substring(start, end).trim();
    }
}
