package net.letsdank.jd.ast;

import java.util.ArrayList;
import java.util.List;

public final class JavaTypeUtils {
    private JavaTypeUtils() {
    }

    public static String methodReturnType(String methodDescriptor) {
        int idx = methodDescriptor.indexOf(')');
        if (idx < 0 || idx + 1 >= methodDescriptor.length()) {
            throw new IllegalArgumentException("Bad method descriptor: " + methodDescriptor);
        }
        String retDesc = methodDescriptor.substring(idx + 1);
        if ("V".equals(retDesc)) {
            return "void";
        }
        return toJavaType(retDesc);
    }

    public static List<String> methodParameterTypes(String methodDescriptor) {
        var paramDescs = DescriptorUtils.parseParameterTypes(methodDescriptor);
        List<String> result = new ArrayList<>(paramDescs.size());
        for (String d : paramDescs) {
            result.add(toJavaType(d));
        }
        return result;
    }

    /**
     * Один дескриптор типа -> строка Java-типа.
     */
    public static String toJavaType(String desc) {
        int arrayDims = 0;
        while (desc.charAt(arrayDims) == '[') {
            arrayDims++;
        }
        String elemDesc = desc.substring(arrayDims);
        String baseType = switch (elemDesc.charAt(0)) {
            case 'B' -> "byte";
            case 'C' -> "char";
            case 'D' -> "double";
            case 'F' -> "float";
            case 'I' -> "int";
            case 'J' -> "long";
            case 'S' -> "short";
            case 'Z' -> "boolean";
            case 'L' -> {
                // Ljava/lang/String;
                int semi = elemDesc.indexOf(';');
                String internal = elemDesc.substring(1, semi);
                yield simpleClassName(internal);
            }
            default -> "Object"; // fallback
        };

        StringBuilder sb = new StringBuilder(baseType);
        for (int i = 0; i < arrayDims; i++) {
            sb.append("[]");
        }
        return sb.toString();
    }

    private static String simpleClassName(String internalName) {
        // internalName: java/lang/System
        int slash = internalName.lastIndexOf('/');
        if (slash >= 0 && slash < internalName.length() - 1) {
            return internalName.substring(slash + 1);
        }
        return internalName.replace('/', '.');
    }
}
