package net.letsdank.jd.ast;

import net.letsdank.jd.ast.expr.BinaryExpr;
import net.letsdank.jd.ast.expr.Expr;
import net.letsdank.jd.ast.expr.IntConstExpr;
import net.letsdank.jd.ast.expr.VarExpr;
import net.letsdank.jd.ast.stmt.*;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.MethodInfo;

import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Простейший pretty-printer в Kotlin.
 * Логика по stmt/expr почти совпадает с JavaPrettyPrinter,
 * отличается только заголовок метода и типы.
 */
public final class KotlinPrettyPrinter {
    private final StringBuilder sb = new StringBuilder();
    private int indent = 0;

    public String printMethod(ClassFile cf, MethodInfo method, MethodAst ast) {
        sb.setLength(0);
        indent = 0;

        ConstantPool cp = cf.constantPool();
        String desc = cp.getUtf8(method.descriptorIndex());

        MethodLocalNameProvider nameProvider = new MethodLocalNameProvider(method.accessFlags(), desc);

        String header = buildMethodHeader(cf, method, desc, nameProvider);
        appendLine("// decompiled to Kotlin (experimental)");
        appendLine(header + " {");
        indent++;

        BlockStmt body = ast.body();
        for (Stmt stmt : body.statements()) {
            printStmt(stmt);
        }

        indent--;
        appendLine("}");

        return sb.toString();
    }

    private String buildMethodHeader(ClassFile cf, MethodInfo method, String desc, MethodLocalNameProvider names) {
        ConstantPool cp = cf.constantPool();
        String rawName = cp.getUtf8(method.nameIndex());

        String methodName;
        String returnType;
        boolean isConstructor = "<init>".equals(rawName);
        boolean isClinit = "<clinit>".equals(rawName);

        if (isConstructor || isClinit) {
            // Пока конструкторы/статические инициализаторы опустим,
            // чтобы не городить сложный синтаксис.
            methodName = rawName; // будет "init"/"<clinit>" в комментариях
            returnType = "Unit";
        } else {
            methodName = rawName;
            returnType = KotlinTypeUtils.methodReturnType(desc);
        }

        StringBuilder header = new StringBuilder();

        int acc = method.accessFlags();
        if (Modifier.isPublic(acc)) header.append("public ");
        else if (Modifier.isProtected(acc)) header.append("protected ");
        else if (Modifier.isPrivate(acc)) header.append("private ");

        // В Kotlin static нет, но чтобы не терять информацию, можно добавить комментарий
        if (Modifier.isStatic(acc) && !isClinit) {
            header.append("// static ").append(' ');
        }

        // fun name(params): ReturnType
        header.append("fun ").append(methodName).append("(");

        List<String> paramTypes = KotlinTypeUtils.methodParameterTypes(desc);
        List<String> paramNames = names.parameterNames();
        for (int i = 0; i < paramTypes.size(); i++) {
            if (i > 0) header.append(", ");
            String type = paramTypes.get(i);
            String name = (i < paramNames.size())
                    ? paramNames.get(i)
                    : ("arg" + i);
            header.append(name).append(": ").append(type);
        }
        header.append(")");

        // В Kotlin тип после параметров: ": Type"
        header.append(": ").append(returnType);

        return header.toString();
    }

    private void printStmt(Stmt stmt) {
        if (stmt instanceof IfStmt ifs) {
            printIf(ifs);
        } else if (stmt instanceof LoopStmt loop) {
            printLoop(loop);
        } else if (stmt instanceof ForStmt forStmt) {
            printFor(forStmt);
        } else if (stmt instanceof AssignStmt as) {
            printAssign(as);
        } else if (stmt instanceof ReturnStmt rs) {
            printReturn(rs);
        } else if (stmt instanceof ExprStmt es) {
            printExprStmt(es);
        } else {
            appendLine("// TODO: " + stmt.getClass().getSimpleName() + " -> " + stmt);
        }
    }

    private void printIf(IfStmt ifs) {
        Expr cond = ifs.condition();

        appendLine("if (" + cond + ") {");
        indent++;
        for (Stmt s : ifs.thenBlock().statements()) {
            printStmt(s);
        }
        indent--;

        BlockStmt elseBlock = ifs.elseBlock();
        if (elseBlock != null && !elseBlock.statements().isEmpty()) {
            appendLine("} else {");
            indent++;
            for (Stmt s : elseBlock.statements()) {
                printStmt(s);
            }
            indent--;
        }

        appendLine("}");
    }

    private void printFor(ForStmt fs) {
        String initStr = fs.init() != null ? stmtToInline(fs.init()) : "";
        String updateStr = fs.update() != null ? stmtToInline(fs.update()) : "";

        appendLine("for (" + initStr + "; " + fs.condition() + "; " + updateStr + ") {");
        indent++;
        for (Stmt s : fs.body().statements()) {
            printStmt(s);
        }
        indent--;
        appendLine("}");
    }

    private void printLoop(LoopStmt loop) {
        appendLine("while (" + loop.condition() + ") {");
        indent++;
        for (Stmt s : loop.body().statements()) {
            printStmt(s);
        }
        indent--;
        appendLine("}");
    }

    private void printAssign(AssignStmt as) {
        Expr target = as.target();
        Expr value = as.value();

        // v2 = v2 + 1 -> v2++;
        if (target instanceof VarExpr tv && value instanceof BinaryExpr be && "+".equals(be.op()) &&
                be.left() instanceof VarExpr lv && be.right() instanceof IntConstExpr c &&
                c.value() == 1 && tv.name().equals(lv.name())) {
            appendLine(tv.name() + "++");
            return;
        }

        appendLine(target.toString() + " = " + value.toString());
    }

    private void printReturn(ReturnStmt rs) {
        if (rs.value() == null) appendLine("return");
        else appendLine("return " + rs.value().toString());
    }

    private void printExprStmt(ExprStmt es) {
        appendLine(es.expr().toString());
    }

    private String stmtToInline(Stmt s) {
        if (s instanceof AssignStmt as) {
            return as.target().toString() + " = " + as.value().toString();
        }
        if (s instanceof ExprStmt es) {
            return es.expr().toString();
        }
        return s.toString();
    }

    private void appendLine(String line) {
        sb.append(" ".repeat(indent * 2)).append(line).append("\n");
    }
}
