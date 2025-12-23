package net.letsdank.jd.ast;

import net.letsdank.jd.ast.expr.BinaryExpr;
import net.letsdank.jd.ast.expr.Expr;
import net.letsdank.jd.ast.expr.IntConstExpr;
import net.letsdank.jd.ast.expr.VarExpr;
import net.letsdank.jd.ast.stmt.*;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.MethodInfo;
import net.letsdank.jd.model.attribute.AttributeInfo;
import net.letsdank.jd.model.attribute.ExceptionsAttribute;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public final class JavaPrettyPrinter {
    private final StringBuilder sb = new StringBuilder();
    private int indent = 0;
    private final ExpressionSimplifier simplifier = new ExpressionSimplifier();

    public String printMethod(ClassFile cf, MethodInfo method, MethodAst ast) {
        // сброс
        sb.setLength(0);
        indent = 0;

        ConstantPool cp = cf.constantPool();
        String desc = cp.getUtf8(method.descriptorIndex());

        MethodLocalNameProvider nameProvider = new MethodLocalNameProvider(method.accessFlags(), desc);

        String header = buildMethodHeader(cf, method, desc, nameProvider);
        sb.append("// decompiled (experimental)\n");
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

    private String buildMethodHeader(ClassFile cf, MethodInfo method,
                                     String desc, MethodLocalNameProvider names) {
        ConstantPool cp = cf.constantPool();
        String rawName = cp.getUtf8(method.nameIndex());

        String methodName;
        String returnType;
        boolean isConstructor = "<init>".equals(rawName);
        boolean isClinit = "<clinit>".equals(rawName);

        if (isConstructor) {
            String fqn = cf.thisClassFqn();
            int dot = fqn.lastIndexOf('.');
            methodName = (dot >= 0) ? fqn.substring(dot + 1) : fqn;
            returnType = ""; // у конструкторов нет типа
        } else if (isClinit) {
            methodName = ""; // static init-блок
            returnType = "";
        } else {
            methodName = rawName;
            returnType = JavaTypeUtils.methodReturnType(desc);
        }

        StringBuilder header = new StringBuilder();

        int acc = method.accessFlags();
        if (Modifier.isPublic(acc)) header.append("public ");
        else if (Modifier.isProtected(acc)) header.append("protected ");
        else if (Modifier.isPrivate(acc)) header.append("private ");

        if (Modifier.isStatic(acc) && !isClinit) header.append("static ");
        if (Modifier.isFinal(acc)) header.append("final ");

        if (!isConstructor && !isClinit) {
            header.append(returnType).append(" ");
        }

        if (isClinit) {
            // static init: "static"
            header.append("static");
            return header.toString();
        }

        header.append(methodName).append("(");

        List<String> paramTypes = JavaTypeUtils.methodParameterTypes(desc);
        List<String> paramNames = names.parameterNames();
        for (int i = 0; i < paramTypes.size(); i++) {
            if (i > 0) header.append(", ");
            String type = paramTypes.get(i);
            String name = (i < paramNames.size())
                    ? paramNames.get(i)
                    : ("arg" + i);
            header.append(type).append(" ").append(name);
        }
        header.append(")");

        List<String> thrown = collectThrownExceptions(method, cp);
        if (!thrown.isEmpty()) {
            header.append(" throws ");
            for (int i = 0; i < thrown.size(); i++) {
                if (i > 0) header.append(", ");
                header.append(thrown.get(i));
            }
        }

        return header.toString();
    }

    private List<String> collectThrownExceptions(MethodInfo method, ConstantPool cp) {
        for (AttributeInfo attr : method.attributes()) {
            if (attr instanceof ExceptionsAttribute ex) {
                List<String> names = new ArrayList<>(ex.exceptionIndexTable().length);
                for (int idx : ex.exceptionIndexTable()) {
                    try {
                        names.add(cp.getClassName(idx).replace('/', '.'));
                    } catch (Exception e) {
                        names.add("<bad-exception>");
                    }
                }
                return names;
            }
        }
        return List.of();
    }

    private void printStmt(Stmt stmt) {
        if (stmt instanceof IfStmt ifs) {
            printIf(ifs);
        } else if (stmt instanceof LoopStmt loop) {
            printLoop(loop);
        } else if (stmt instanceof ForStmt forStmt) {
            printFor(forStmt);
        } else if (stmt instanceof EnhancedForStmt efStmt) {
            printEnhancedFor(efStmt);
        } else if (stmt instanceof SwitchStmt sw) {
            printSwitch(sw);
        } else if (stmt instanceof AssignStmt as) {
            printAssign(as);
        } else if (stmt instanceof ReturnStmt rs) {
            printReturn(rs);
        } else if (stmt instanceof ExprStmt es) {
            printExprStmt(es);
        } else if (stmt instanceof TryCatchStmt tcs) {
            printTryCatchStmt(tcs);
        } else if (stmt instanceof SynchronizedStmt sync) {
            printSynchronized(sync);
        } else if (stmt instanceof CommentStmt cs) {
            appendLine(cs.text());
        } else {
            // временный fallback, чтобы видеть неожиданные типы
            appendLine("// TODO: " + stmt.getClass().getSimpleName() + " -> " + stmt);
        }
    }

    private void printIf(IfStmt ifs) {
        Expr cond = simplifier.simplify(ifs.condition());

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

        appendLine("for (" + initStr + " " + simplifier.simplify(fs.condition()) + "; " + updateStr + ") {");
        indent++;
        for (Stmt s : fs.body().statements()) {
            printStmt(s);
        }
        indent--;
        appendLine("}");
    }

    private void printEnhancedFor(EnhancedForStmt efs) {
        appendLine("for (" + efs.varType() + " " + efs.varName() + " : " + efs.iterable() + ") {");
        indent++;
        for (Stmt s : efs.body().statements()) {
            printStmt(s);
        }
        indent--;
        appendLine("}");
    }

    private void printSwitch(SwitchStmt sw) {
        appendLine("switch (" + sw.selector() + ") {");
        indent++;
        for (var entry : sw.cases().entrySet()) {
            appendLine("case " + entry.getKey() + ":");
            indent++;
            for (Stmt s : entry.getValue().statements()) {
                printStmt(s);
            }
            appendLine("break;");
            indent--;
        }
        if (sw.defaultBlock() != null && !sw.defaultBlock().statements().isEmpty()) {
            appendLine("default:");
            indent++;
            for (Stmt s : sw.defaultBlock().statements()) {
                printStmt(s);
            }
            appendLine("break;");
            indent--;
        }
        indent--;
        appendLine("}");
    }

    private void printLoop(LoopStmt loop) {
        appendLine("while (" + simplifier.simplify(loop.condition()) + ") {");
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
            appendLine(tv.name() + "++;");
            return;
        }

        Expr simplified = simplifier.simplify(value);
        appendLine(target.toString() + " = " + simplified.toString() + ";");
    }

    private void printReturn(ReturnStmt rs) {
        if (rs.value() == null) {
            appendLine("return;");
        } else {
            appendLine("return " + simplifier.simplify(rs.value()) + ";");
        }
    }

    private void printExprStmt(ExprStmt es) {
        appendLine(simplifier.simplify(es.expr()).toString() + ";");
    }

    private void printTryCatchStmt(TryCatchStmt tcs) {
        // try {
        appendLine("try {");
        indent++;
        for (Stmt s : tcs.tryBlock().statements()) {
            printStmt(s);
        }
        indent--;
        appendLine("} catch (" + tcs.exceptionType() + " " + tcs.exceptionVarName() + ") {");
        indent++;
        if (tcs.catchBlock() != null) {
            for (Stmt s : tcs.catchBlock().statements()) {
                printStmt(s);
            }
        }
        indent--;

        // Опциональный finally блок
        if (tcs.finallyBlock() != null && !tcs.finallyBlock().statements().isEmpty()) {
            appendLine("} finally {");
            indent++;
            for (Stmt s : tcs.finallyBlock().statements()) {
                printStmt(s);
            }
            indent--;
        }

        appendLine("}");
    }

    private void printSynchronized(SynchronizedStmt sync) {
        appendLine("synchronized (" + sync.monitor() + ") {");
        indent++;
        for (Stmt s : sync.body().statements()) {
            printStmt(s);
        }
        indent--;
        appendLine("}");
    }

    /**
     * Превращаем простой Stmt вроде AssignStmt/ExprStmt в однострочное "i = 0;" или "i++;".
     */
    private String stmtToInline(Stmt s) {
        if (s instanceof AssignStmt as) {
            Expr simplified = simplifier.simplify(as.value());
            return as.target().toString() + " = " + simplified.toString() + ";";
        }
        if (s instanceof ExprStmt es) {
            return simplifier.simplify(es.expr()).toString() + ";";
        }
        // fallback
        return s.toString();
    }

    private void appendLine(String line) {
        sb.append(" ".repeat(indent * 2)).append(line).append("\n");
    }
}
