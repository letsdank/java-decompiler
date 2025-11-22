package net.letsdank.jd.ast;

import kotlin.metadata.KmClassifier;
import kotlin.metadata.KmType;
import kotlin.metadata.KmTypeProjection;
import kotlin.metadata.KmVariance;
import net.letsdank.jd.kotlin.MetadataFlagsKt;

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

    /**
     * Преобразование KmType (из kotlin-metadata) в строку Kotlin-типа.
     * <p>
     * Поддерживает:
     * - обычные классы (kotlin.Int -> Int, java.lang.String -> String и т.п.)
     * - дженерики: List<String>, Map<String, Int>
     * - nullable: T?
     * - проекции: in / out / *
     * <p>
     * Это отдельный слой над уже существующим toKotlinType(String),
     * который мапит FQN вида "kotlin.Int" -> "Int".
     */
    public static String kmTypeToKotlin(KmType type) {
        if (type == null) {
            return "Unit"; // безопасный fallback
        }

        KmType actual = type;

        // Если есть сокращение типа (typealias) - работаем с аббревиатурой
        if (actual.getAbbreviatedType() != null) {
            actual = actual.getAbbreviatedType();
        }

        // Базовое имя (класс/alias/параметр типа)
        String base = "Any";

        KmClassifier classifier = actual.getClassifier();
        boolean isFunctionType = false;
        List<KmTypeProjection> functionArgs = null;
        String functionInternalName = null;

        // 1. Определяем базовое имя + проверяем, не function ли это
        if (classifier instanceof KmClassifier.Class cls) {
            functionInternalName = cls.getName(); // internal name, например "kotlin/Function2"
            String fqn = functionInternalName.replace('/', '.');

            // Проверяем, что это функциональный тип
            if (functionInternalName.startsWith("kotlin/Function") ||
                    functionInternalName.startsWith("kotlin/reflect/KFunction") ||
                    functionInternalName.startsWith("kotlin/reflect/KSuspendFunction")) {
                isFunctionType = true;
                functionArgs = actual.getArguments();
                // base пока не заполняем, для этого логика вынесена ниже отдельно
            } else {
                base = toKotlinType(fqn);
            }
        } else if (classifier instanceof KmClassifier.TypeAlias alias) {
            String internalName = alias.getName();
            String fqn = internalName.replace('/', '.');
            base = toKotlinType(fqn);
        } else if (classifier instanceof KmClassifier.TypeParameter tp) {
            // Для простоты: T0, T1, ...
            base = "T" + tp.getId();
        }

        // 2. Если это function type - печатаем как (A, B) -> R
        if (isFunctionType && functionArgs != null && !functionArgs.isEmpty()) {
            List<String> argTypes = new ArrayList<>();

            for (KmTypeProjection proj : functionArgs) {
                if (proj.getType() == null) {
                    argTypes.add("*");
                } else {
                    argTypes.add(kmTypeToKotlin(proj.getType()));
                }
            }

            String functionText;
            if (argTypes.isEmpty()) {
                functionText = "() -> Unit";
            } else if (argTypes.size() == 1) {
                // () -> R
                functionText = "() -> " + argTypes.getFirst();
            } else {
                String returnType = argTypes.getLast();
                List<String> params = argTypes.subList(0, argTypes.size() - 1);

                StringBuilder paramsSb = new StringBuilder();
                paramsSb.append("(");
                for (int i = 0; i < params.size(); i++) {
                    if (i > 0) paramsSb.append(", ");
                    paramsSb.append(params.get(i));
                }
                paramsSb.append(")");

                functionText = paramsSb + " -> " + returnType;
            }

            base = functionText;
        } else if (!actual.getArguments().isEmpty()) {
            // 3. Обычный generic-класс: List<out String?>, Map<in K, out V>, ...
            StringBuilder sb = new StringBuilder();
            sb.append(base).append("<");
            List<KmTypeProjection> args = actual.getArguments();
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) sb.append(", ");
                KmTypeProjection proj = args.get(i);
                if (proj.getType() == null) {
                    sb.append("*");
                } else {
                    KmVariance variance = proj.getVariance();
                    if (variance == KmVariance.IN) {
                        sb.append("in ");
                    } else if (variance == KmVariance.OUT) {
                        sb.append("out ");
                    }
                    sb.append(kmTypeToKotlin(proj.getType()));
                }
            }
            sb.append(">");
            base = sb.toString();
        }

        // Nullable / definitely-non-null
        if (MetadataFlagsKt.isNullable(actual) && !MetadataFlagsKt.isDefinitelyNotNull(actual)) {
            base = base + "?";
        }

        // Suspend-тип
        if (MetadataFlagsKt.isSuspend(actual)) {
            base = "suspend " + base;
        }

        return base;
    }
}
