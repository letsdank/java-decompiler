package net.letsdank.jd.lang;

import net.letsdank.jd.ast.KotlinPrettyPrinter;
import net.letsdank.jd.ast.MethodAst;
import net.letsdank.jd.ast.MethodDecompiler;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.MethodInfo;

/**
 * Бэкенд, который печатает методы в виде Kotlin-кода,
 * используя существующий AST и KotlinPrettyPrinter.
 */
public final class KotlinLanguageBackend implements LanguageBackend {
    @Override
    public Language language() {
        return Language.KOTLIN;
    }

    @Override
    public String decompileMethod(ClassFile cf, MethodInfo method, MethodAst ast) {
        KotlinPrettyPrinter printer = new KotlinPrettyPrinter();
        return printer.printMethod(cf, method, ast);
    }

    @Override
    public String decompileClass(ClassFile cf, MethodDecompiler methodDecompiler) {
        StringBuilder out = new StringBuilder();

        // Пока тоже делаем простой заголовок/комментарий
        out.append("// decompiled Kotlin class (experimental)\n");
        out.append("// ").append(cf.thisClassFqn()).append("\n\n");

        for (MethodInfo method : cf.methods()) {
            MethodAst ast = methodDecompiler.decompile(method, cf);
            String methodText = decompileMethod(cf, method, ast);
            out.append(methodText).append("\n\n");
        }

        return out.toString();
    }
}
