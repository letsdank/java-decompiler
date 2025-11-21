package net.letsdank.jd.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Очень простой маппер типов в Kotlin-представление.
 * Основан на JavaTypeUtils, поэтому покрытие то же самое.
 */
public final class KotlinTypeUtils {
    private KotlinTypeUtils() {
    }

    public static String methodReturnType(String methodDescriptor) {
        String javaType = JavaTypeUtils.methodReturnType(methodDescriptor);
        return toKotlinType(javaType);
    }

    public static List<String> methodParameterTypes(String methodDescriptor) {
        List<String> javaTypes = JavaTypeUtils.methodParameterTypes(methodDescriptor);
        List<String> result = new ArrayList<>(javaTypes.size());
        for (String jt : javaTypes) {
            result.add(toKotlinType(jt));
        }
        return result;
    }

    /**
     * Очень грубый маппинг:
     * int -> Int, boolean -> Boolean, java.lang.String -> String и т.д.
     * Остальные классы -> simpleName (List вместо java.util.List).
     */
    private static String toKotlinType(String javaType) {
        // массивы: int[] -> IntArray, String[] -> Array<String> (для простоты оставим как есть)
        // Чтобы сильно не усложнять, оставим "T[]" как есть - Kotlin это проглотит.
        String t = javaType;

        switch (t) {
            case "void":
                return "Unit";
            case "int":
                return "Int";
            case "long":
                return "Long";
            case "short":
                return "Short";
            case "byte":
                return "Byte";
            case "boolean":
                return "Boolean";
            case "float":
                return "Float";
            case "double":
                return "Double";
            case "char":
                return "Char";
        }

        // java.lang.String -> String
        if ("java.lang.String".equals(t)) {
            return "String";
        }

        // java.util.List -> List и т.п.
        int lastDot = t.lastIndexOf('.');
        if (lastDot >= 0 && lastDot + 1 < t.length()) {
            return t.substring(lastDot + 1);
        }

        return t;
    }
}
