package net.letsdank.jd.ast;

import net.letsdank.jd.ast.expr.*;
import net.letsdank.jd.ast.stmt.*;
import net.letsdank.jd.kotlin.KotlinPropertyRegistry;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.MethodInfo;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Простейший pretty-printer в Kotlin.
 * Логика по stmt/expr почти совпадает с JavaPrettyPrinter,
 * отличается только заголовок метода и типы.
 */
public final class KotlinPrettyPrinter {
    private final StringBuilder sb = new StringBuilder();
    private int indent = 0;

    private final Set<String> knownProperties;

    public KotlinPrettyPrinter() {
        this(Set.of());
    }

    public KotlinPrettyPrinter(Set<String> knownProperties) {
        this.knownProperties = knownProperties;
    }

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

        appendLine("if (" + printExpr(cond) + ") {");
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

        appendLine("for (" + initStr + "; " + printExpr(fs.condition()) + "; " + updateStr + ") {");
        indent++;
        for (Stmt s : fs.body().statements()) {
            printStmt(s);
        }
        indent--;
        appendLine("}");
    }

    private void printLoop(LoopStmt loop) {
        appendLine("while (" + printExpr(loop.condition()) + ") {");
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

        appendLine(printExpr(target) + " = " + printExpr(value));
    }

    private void printReturn(ReturnStmt rs) {
        if (rs.value() == null) appendLine("return");
        else appendLine("return " + printExpr(rs.value()));
    }

    private void printExprStmt(ExprStmt es) {
        appendLine(printExpr(es.expr()));
    }

    private String stmtToInline(Stmt s) {
        if (s instanceof AssignStmt as) {
            return printExpr(as.target()) + " = " + printExpr(as.value());
        }
        if (s instanceof ExprStmt es) {
            return printExpr(es.expr());
        }
        return s.toString();
    }

    /**
     * Унифицированный принтер выражений для Kotlin.
     * Умеет:
     * - распознавать конкатенации строк и печатать их как строковые шаблоны;
     * - в остальных случаях делегирует в toString().
     */
    private String printExpr(Expr expr) {
        // 1. Строковые конкатенации -> Kotlin-шаблоны
        if (expr instanceof BinaryExpr bin && "+".equals(bin.op())) {
            List<Expr> chain = new ArrayList<>();
            flattenPlus(bin, chain);
            boolean hasStringLiteral = false;
            for (Expr e : chain) {
                if (e instanceof StringLiteralExpr) {
                    hasStringLiteral = true;
                    break;
                }
            }
            if (hasStringLiteral) {
                return buildKotlinStringTemplate(chain);
            }
        }

        // 2. Попытка интерпретировать вызов как доступ к свойству
        if (expr instanceof CallExpr call) {
            String propAccess = tryPrintPropertyAccess(call);
            if (propAccess != null) return propAccess;
        }

        // 3. Обычный случай
        return printSimpleExpr(expr);
    }

    private String tryPrintPropertyAccess(CallExpr call) {
        String methodName = call.methodName();

        // 1. Пробуем вытащить имя свойства из имени метода
        String propName = null;

        if (methodName.startsWith("get") && methodName.length() > 3) {
            String base = methodName.substring(3);
            propName = Character.toLowerCase(base.charAt(0)) + base.substring(1);
        } else if (methodName.startsWith("is") && methodName.length() > 2) {
            String base = methodName.substring(2);
            propName = Character.toLowerCase(base.charAt(0)) + base.substring(1);
        }

        if (propName == null) {
            return null; // это не property-getter, а обычный метод
        }

        // 2. Если известен ownerInternalName - проверяем глобальный реестр
        String ownerInternal = call.ownerInternalName();
        boolean isProperty;

        if (ownerInternal != null) {
            // основной путь: есть ownerInternalName -> смотрим в глобальный реестр
            isProperty = KotlinPropertyRegistry.hasProperty(ownerInternal, propName);
        } else {
            // fallback: если owner неизвестен (например, this/текущий файл) -
            // используем локальный набор свойств класса/файла
            isProperty = knownProperties.contains(propName);
        }

        if (!isProperty) {
            // метаданные говорят, что это не property, а обычный метод
            return null;
        }

        // 3. Печатаем доступ к свойству
        if (call.target() == null) {
            // top-level property (или статическое property без объекта)
            return propName;
        } else {
            // доступ через объект: a.name
            return printExpr(call.target()) + "." + propName;
        }
    }

    private String printSimpleExpr(Expr expr) {
        return expr.toString();
    }

    private void flattenPlus(Expr expr, List<Expr> out) {
        if (expr instanceof BinaryExpr bin && "+".equals(bin.op())) {
            flattenPlus(bin.left(), out);
            flattenPlus(bin.right(), out);
        } else {
            out.add(expr);
        }
    }

    private String buildKotlinStringTemplate(List<Expr> parts) {
        StringBuilder out = new StringBuilder();
        out.append("\"");
        for (Expr part : parts) {
            if (part instanceof StringLiteralExpr s) {
                out.append(escapeKotlinString(s.value()));
            } else if (part instanceof VarExpr v && isSimpleIdentifier(v.name())) {
                out.append("$").append(v.name());
            } else if (part instanceof FieldAccessExpr fa &&
                    fa.target() instanceof VarExpr v &&
                    "this".equals(v.name()) &&
                    isSimpleIdentifier(fa.fieldName())) {
                // this.field -> $field
                out.append("$").append(fa.fieldName());
            } else {
                out.append("${").append(printExpr(part)).append("}");
            }
        }
        out.append("\"");
        return out.toString();
    }

    private String escapeKotlinString(String raw) {
        String escaped = raw
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
        // Экранируем $ внутри литерала, чтобы он не стал началом шаблона
        escaped = escaped.replace("$", "\\$");
        return escaped;
    }

    private boolean isSimpleIdentifier(String name) {
        if (name == null || name.isEmpty()) return false;
        if (!Character.isJavaIdentifierPart(name.charAt(0))) return false;
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) return false;
        }
        return true;
    }

    private void appendLine(String line) {
        sb.append(" ".repeat(indent * 2)).append(line).append("\n");
    }
}
