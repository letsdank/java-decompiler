package net.letsdank.jd.lang;

import net.letsdank.jd.ast.JavaPrettyPrinter;
import net.letsdank.jd.ast.MethodAst;
import net.letsdank.jd.ast.MethodDecompiler;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.MethodInfo;

public final class JavaLanguageBackend implements LanguageBackend {
    @Override
    public Language language() {
        return Language.JAVA;
    }

    @Override
    public String decompileMethod(ClassFile cf, MethodInfo method, MethodAst ast) {
        JavaPrettyPrinter printer = new JavaPrettyPrinter();
        return printer.printMethod(cf, method, ast);
    }

    @Override
    public String decompileClass(ClassFile cf, MethodDecompiler methodDecompiler) {
        StringBuilder out = new StringBuilder();

        // Простейший заголовок класса (пока без модификаторов)
        out.append("// decompiled class (experimental)\n");
        out.append("// ").append(cf.thisClassFqn()).append("\n\n");

        for (MethodInfo method : cf.methods()) {
            MethodAst ast = methodDecompiler.decompile(method, cf);
            String methodText = decompileMethod(cf, method, ast);
            out.append(methodText).append("\n\n");
        }

        return out.toString();
    }
}
