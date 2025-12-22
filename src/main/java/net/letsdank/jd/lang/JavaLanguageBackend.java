package net.letsdank.jd.lang;

import net.letsdank.jd.ast.JavaPrettyPrinter;
import net.letsdank.jd.ast.MethodAst;
import net.letsdank.jd.ast.MethodDecompiler;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.MethodInfo;
import net.letsdank.jd.model.attribute.AttributeInfo;
import net.letsdank.jd.model.attribute.SourceFileAttribute;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;

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

        // Комментарий с информацией о компиляции
        out.append("// decompiled class (experimental)\n");
        String sourceFile = findSourceFile(cf);
        if (sourceFile != null) {
            out.append("// from ").append(sourceFile).append("\n");
        }
        out.append("\n");

        // Объявление класса с модификаторами, extends, implements
        out.append(buildClassDeclaration(cf)).append(" {\n\n");

        for (MethodInfo method : cf.methods()) {
            MethodAst ast = methodDecompiler.decompile(method, cf);
            String methodText = decompileMethod(cf, method, ast);
            // Отступаем методы внутри класса
            String indented = indent(methodText);
            out.append(indented).append("\n\n");
        }

        out.append("}\n");
        return out.toString();
    }

    private String buildClassDeclaration(ClassFile cf) {
        StringBuilder sb = new StringBuilder();
        int flags = cf.accessFlags();

        if (Modifier.isPublic(flags)) sb.append("public ");
        if (Modifier.isAbstract(flags)) sb.append("abstract ");
        if (Modifier.isFinal(flags)) sb.append("final ");

        String fqn = cf.thisClassFqn();
        String simpleName = fqn.contains(".") ? fqn.substring(fqn.lastIndexOf('.') + 1) : fqn;

        sb.append("class ").append(simpleName);

        String superFqn = cf.superClassFqn();
        if (superFqn != null && !"java.lang.Object".equals(superFqn)) {
            sb.append(" extends ").append(superFqn);
        }

        if (cf.interfaceIndices().length > 0) {
            sb.append(" implements ");
            ConstantPool cp = cf.constantPool();
            String interfaces = Arrays.stream(cf.interfaceIndices())
                    .mapToObj(idx -> {
                        try {
                            return cp.getClassName(idx).replace('/', '.');
                        } catch (Exception e) {
                            return "<bad-interface>";
                        }
                    })
                    .collect(Collectors.joining(", "));
            sb.append(interfaces);
        }

        return sb.toString();
    }

    private String findSourceFile(ClassFile cf) {
        for (AttributeInfo attr : cf.attributes()) {
            if (attr instanceof SourceFileAttribute sf) {
                try {
                    return cf.constantPool().getUtf8(sf.sourceFileIndex());
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    private String indent(String text) {
        return Arrays.stream(text.split("\n"))
                .map(line -> line.isEmpty() ? "" : "    " + line)
                .collect(Collectors.joining("\n"));
    }
}
