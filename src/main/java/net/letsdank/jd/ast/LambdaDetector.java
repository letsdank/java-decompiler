package net.letsdank.jd.ast;

import net.letsdank.jd.ast.expr.CallExpr;
import net.letsdank.jd.ast.expr.Expr;
import net.letsdank.jd.ast.expr.LambdaExpr;
import net.letsdank.jd.ast.expr.MethodRefExpr;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.attribute.BootstrapMethodsAttribute;
import net.letsdank.jd.model.cp.CpInvokeDynamic;
import net.letsdank.jd.model.cp.CpMethodHandle;
import net.letsdank.jd.model.cp.CpMethodref;
import net.letsdank.jd.model.cp.CpNameAndType;

import java.util.ArrayList;
import java.util.List;

/**
 * Распознает lambda expressions и method references из invokedynamic.
 */
public final class LambdaDetector {

    private final ConstantPool cp;
    private final BootstrapMethodsAttribute bootstrapMethods;

    public LambdaDetector(ConstantPool cp, BootstrapMethodsAttribute bootstrapMethods) {
        this.cp = cp;
        this.bootstrapMethods = bootstrapMethods;
    }

    /**
     * Пытается распознать lambda/method reference из invokedynamic.
     *
     * @param indy invokedynamic constant pool entry
     * @param args аргументы лямбды (captured variables)
     * @return Lambda или MethodRef выражение, либо null если не удалось распознать
     */
    public Expr detectLambda(CpInvokeDynamic indy, List<Expr> args) {
        if (bootstrapMethods == null || bootstrapMethods.methods().length == 0) {
            return null;
        }

        int bmIndex = indy.bootstrapMethodAttrIndex();
        if (bmIndex < 0 || bmIndex >= bootstrapMethods.methods().length) {
            return null;
        }

        BootstrapMethodsAttribute.BootstrapMethod bm = bootstrapMethods.methods()[bmIndex];
        CpMethodHandle mh = (CpMethodHandle) cp.entry(bm.bootstrapMethodRef());

        // Проверяем bootstrap method
        CpMethodref targetRef = (CpMethodref) cp.entry(mh.referenceIndex());
        String ownerInternal = cp.getClassName(targetRef.classIndex());
        String owner = ownerInternal.replace('/', '.');

        CpNameAndType targetNt = (CpNameAndType) cp.entry(targetRef.nameAndTypeIndex());
        String targetName = cp.getUtf8(targetNt.nameIndex());

        // java.lang.invoke.LambdaMetafactory - стандартная фабрика для лямбд
        boolean isLambdaMetafactory = "java.lang.invoke.LambdaMetafactory".equals(owner) &&
                ("metafactory".equals(targetName) ||
                        "altMetafactory".equals(targetName));

        if (!isLambdaMetafactory) {
            return null;
        }

        // Достаем информацию о лямбде из bootstrap arguments
        if (bm.bootstrapArguments().length < 3) {
            return null;
        }

        // arg[0] - invokedType (MethodType) - сигнатура функционального интерфейса
        // arg[1] - implMethod (MethodHandle) - что вызывается (метод или лямбда)
        // arg[2] - instantiatedMethodType (MethodType) - сигнатура после erasure

        CpMethodHandle implMethod = (CpMethodHandle) cp.entry(bm.bootstrapArguments()[1]);

        // Проверяем вид Methodhandle
        int refKind = implMethod.referenceKind();

        // REF_invokeStatic (6) - обычная лямбда компилируется в статический метод
        // REF_invokeSpecial (7) - конструктор
        // REF_invokeVirtual (5) - метод экземпляра
        // REF_invokeInterface (9) - метод интерфейса

        if (refKind == 6) {
            // Статический метод - это compiled lambda
            return detectCompiledLambda(implMethod, args);
        } else if (refKind == 5 || refKind == 7 || refKind == 9) {
            // Method reference
            return detectMethodReference(implMethod, args, refKind);
        }

        return null;
    }

    /**
     * Распознает compiled lambda (компилятор создал синтетический метод).
     */
    private Expr detectCompiledLambda(CpMethodHandle implMethod, List<Expr> args) {
        CpMethodref methodRef = (CpMethodref) cp.entry(implMethod.referenceIndex());
        CpNameAndType nt = (CpNameAndType) cp.entry(methodRef.nameAndTypeIndex());

        String methodName = cp.getUtf8(nt.nameIndex());
        String descriptor = cp.getUtf8(nt.descriptorIndex());

        // Синтетические lambda методы обычно называются lambda$...
        if (!methodName.startsWith("lambda$")) {
            return null;
        }

        // Парсим параметры из дескриптора метода
        List<String> paramTypes = parseParameterTypes(descriptor);

        // Количество параметров лямбды = общее количество - captured variables
        int lambdaParamCount = paramTypes.size() - args.size();
        if (lambdaParamCount < 0) lambdaParamCount = 0;

        List<String> lambdaParamTypes = lambdaParamCount > 0
                ? paramTypes.subList(args.size(), paramTypes.size())
                : List.of();
        List<String> paramNames = generateParamNames(lambdaParamCount);

        // Тело лямбды - это вызов синтетического метода
        // В реальной реализации нужно было бы декомпилировать этот метод
        // Пока что создаем placeholder
        Expr body = new CallExpr(null, null, methodName, args);

        return new LambdaExpr(paramNames, lambdaParamTypes, body, false);
    }

    /**
     * Распознает method reference
     */
    private Expr detectMethodReference(CpMethodHandle implMethod, List<Expr> args, int refKind) {
        CpMethodref methodRef = (CpMethodref) cp.entry(implMethod.referenceIndex());

        String className = cp.getClassName(methodRef.classIndex());
        CpNameAndType nt = (CpNameAndType) cp.entry(methodRef.nameAndTypeIndex());
        String methodName = cp.getUtf8(nt.nameIndex());

        // Определяем target
        Expr target = null;
        String targetClass = null;
        boolean isStatic = (refKind == 6);

        if (!args.isEmpty() && refKind != 6) {
            // Первый аргумент - это объект для instance method reference
            target = args.getFirst();
        } else {
            // Статический метод или constructor reference
            targetClass = className.replace('/', '.');
        }

        // "<init>" означает constuctor reference
        if ("<init>".equals(methodName)) {
            methodName = "new";
        }

        return new MethodRefExpr(target, targetClass, methodName, isStatic);
    }

    /**
     * Парсит типы параметров из дескриптора метода.
     */
    private List<String> parseParameterTypes(String descriptor) {
        List<String> types = new ArrayList<>();

        int start = descriptor.indexOf('(');
        int end = descriptor.indexOf(')');

        if (start == -1 || end == -1) {
            return types;
        }

        String params = descriptor.substring(start + 1, end);
        int i = 0;

        while (i < params.length()) {
            char c = params.charAt(i);

            if (c == 'L') {
                // Object type
                int semi = params.indexOf(';', i);
                String type = params.substring(i + 1, semi).replace('/', '.');
                types.add(type);
                i = semi + 1;
            } else if (c == '[') {
                // Array
                int j = i;
                while (j < params.length() && params.charAt(j) == '[') {
                    j++;
                }
                if (j < params.length()) {
                    String baseType = parseBaseType(params.charAt(j));
                    String arrayType = baseType + "[]".repeat(j - i);
                    types.add(arrayType);
                    i = j + 1;
                }
            } else {
                // Primitive type
                types.add(parseBaseType(c));
                i++;
            }
        }

        return types;
    }

    /**
     * Парсит примитивный тип из дескриптора.
     */
    private String parseBaseType(char c) {
        return switch (c) {
            case 'Z' -> "boolean";
            case 'B' -> "byte";
            case 'C' -> "char";
            case 'S' -> "short";
            case 'I' -> "int";
            case 'J' -> "long";
            case 'F' -> "float";
            case 'D' -> "double";
            case 'V' -> "void";
            default -> "Object";
        };
    }

    /**
     * Генерирует имена параметров.
     */
    private List<String> generateParamNames(int count) {
        List<String> names = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            if (count == 1) {
                names.add("x");
            } else {
                names.add("x" + i);
            }
        }
        return names;
    }
}
