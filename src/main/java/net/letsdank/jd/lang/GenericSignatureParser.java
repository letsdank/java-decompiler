package net.letsdank.jd.lang;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Парсит и форматирует generic type signatures.
 *
 * Примеры:
 * - "List<String>" -> List с параметром String
 * - "Map<String, Integer>" -> Map с String и Integer
 * - "<T extends Number> T" -> метод с bounded type variable
 */
public final class GenericSignatureParser {

    /**
     * Парсит class signature и извлекает type parameters.
     *
     * Signature: "<T:Ljava/lang/Number;>LLjava/lang/Object;"
     * -> имя="T", bounds=["Number"]
     */
    public static List<TypeVariable> parseClassSignature(String signature) {
        List<TypeVariable> vars = new ArrayList<>();

        if (signature == null || !signature.startsWith("<")) {
            return vars;
        }

        int idx = 1;
        while (idx < signature.length()) {
            if (signature.charAt(idx) == '>') {
                break; // конец type parameters
            }

            // Парсим NAME:bound1:bound2...
            int colonIdx = signature.indexOf(':', idx);
            if (colonIdx == -1) break;

            String name = signature.substring(idx, colonIdx);
            List<String> bounds = new ArrayList<>();

            int nextIdx = colonIdx + 1;
            while (nextIdx < signature.length()) {
                char c = signature.charAt(nextIdx);
                if (c == '>') {
                    break;
                }
                if (c == ':') {
                    nextIdx++;
                    continue;
                }

                // Парсим type reference
                int typeEnd = findTypeEnd(signature, nextIdx);
                String bound = signature.substring(nextIdx, typeEnd);
                bounds.add(formatType(bound));
                nextIdx = typeEnd;
            }

            vars.add(new TypeVariable(name, bounds));
            idx = nextIdx;
        }

        return vars;
    }

    /**
     * Парсит method signature и извлекает return type с generics.
     *
     * "(Ljava/lang/String;)Ljava/util/List<Ljava/lang/String;>;"
     */
    public static MethodGenericSignature parseMethodSignature(String signature) {
        if (signature == null) {
            return new MethodGenericSignature(new ArrayList<>(), List.of(), "void");
        }

        // Type parameters
        List<TypeVariable> typeVars = new ArrayList<>();
        int idx = 0;
        if (signature.startsWith("<")) {
            idx = parseTypeParameters(signature, 0, typeVars);
        }

        // Parameters in (...)
        int parenOpen = signature.indexOf('(', idx);
        int parenClose = signature.indexOf(')', idx);
        if (parenOpen == -1 || parenClose == -1 || parenClose < parenOpen) {
            return new MethodGenericSignature(typeVars, List.of(), "?");
        }

        String paramsPart = signature.substring(parenOpen + 1, parenClose);
        List<String> paramTypes = new ArrayList<>();
        int pIdx = 0;
        while (pIdx < paramsPart.length()) {
            int typeEnd = findTypeEnd(paramsPart, pIdx);
            String pDesc = paramsPart.substring(pIdx, typeEnd);
            paramTypes.add(formatType(pDesc));
            pIdx = typeEnd;
        }

        // Return type после )
        String returnTypeStr = signature.substring(parenClose + 1);
        String returnType = formatType(returnTypeStr);

        return new MethodGenericSignature(typeVars, paramTypes, returnType);
    }

    /**
     * Форматирует type descriptor в читаемый вид.
     *
     * Ljava/util/List<Ljava/lang/String;>; -> List<String>
     * Ljava/lang/String; -> String
     * [Ljava/lang/String; -> String[]
     */
    public static String formatType(String descriptor) {
        if (descriptor == null || descriptor.isEmpty()) {
            return "Object";
        }

        return switch (descriptor.charAt(0)) {
            case 'Z' -> "boolean";
            case 'B' -> "byte";
            case 'C' -> "char";
            case 'S' -> "Short";
            case 'I' -> "int";
            case 'J' -> "long";
            case 'F' -> "float";
            case 'D' -> "double";
            case 'V' -> "void";
            case 'L' -> parseObjectType(descriptor);
            case '[' -> formatType(descriptor.substring(1)) + "[]";
            case 'T' -> formatTypeVariable(descriptor);
            case '*' -> "?"; // wildcard
            case '+' -> parseWildcardBound(descriptor, "extends");
            case '-' -> parseWildcardBound(descriptor, "super");
            default -> "Object";
        };
    }

    /**
     * Парсит object type с возможными generics.
     *
     * Ljava/util/List<Ljava/lang/String;>; -> List<String>
     */
    private static String parseObjectType(String desc) {
        // Найдем конец simple type name
        int semiIdx = desc.indexOf(';');
        if (semiIdx == -1) semiIdx = desc.length();

        int genericStart = desc.indexOf('<');
        int typeEnd = genericStart == -1 ? semiIdx : genericStart;

        String baseName = desc.substring(1, typeEnd).replace('/', '.');

        if (genericStart != -1) {
            int depth = 1;
            int idx = genericStart + 1;
            while (idx < desc.length() && depth > 0) {
                char c = desc.charAt(idx);
                if (c == '<') depth++;
                else if (c == '>') depth--;
                idx++;
            }

            int genericEnd = depth == 0 ? idx - 1 : desc.length();
            String genericPart = desc.substring(genericStart + 1, genericEnd);
            String formattedGenerics = formatGenericParameters(genericPart);
            return baseName + "<" + formattedGenerics + ">";
        }

        return baseName;
    }

    /**
     * Форматирует generic parameters.
     */
    private static String formatGenericParameters(String genPart) {
        List<String> params = new ArrayList<>();
        int idx = 0;

        while (idx < genPart.length()) {
            int typeEnd = findTypeEnd(genPart, idx);
            String param = genPart.substring(idx, typeEnd);
            params.add(formatType(param));
            idx = typeEnd;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(params.get(i));
        }
        return sb.toString();
    }

    /**
     * Находит конец type descriptor, учитывая вложенные generics.
     */
    private static int findTypeEnd(String str, int start) {
        if (start >= str.length()) return start;

        char startChar = str.charAt(start);
        if ("ZBCSIFDJV".indexOf(startChar) >= 0) {
            return start + 1; // primitive or void descriptor
        }
        if (startChar == '[') {
            return findTypeEnd(str, start + 1); // array element type
        }

        int idx = start;
        int depth = 0;

        while (idx < str.length()) {
            char c = str.charAt(idx);

            if (c == '<') depth++;
            else if (c == '>') depth--;
            else if (c == ':' && depth == 0) return idx + 1;
            else if (c == ';' && depth == 0) return idx;

            idx++;
        }

        return idx;
    }

    /**
     * Парсит type parameters.
     */
    private static int parseTypeParameters(String sig, int start, List<TypeVariable> out) {
        int idx = start + 1; // пропускаем <

        while (idx < sig.length()) {
            if (sig.charAt(idx) == '>') {
                return idx + 1;
            }

            int colorIdx = sig.indexOf(':', idx);
            if (colorIdx == -1) break;

            String name = sig.substring(idx, colorIdx);
            List<String> bounds = new ArrayList<>();

            idx = colorIdx + 1;
            while (idx < sig.length()) {
                char c = sig.charAt(idx);
                if (c == '>') {
                    out.add(new TypeVariable(name, bounds));
                    return idx + 1;
                }
                if (c == ':') { // пустой class bound, двигаемся к interface bound
                    idx++;
                    continue;
                }

                int typeEnd = findTypeEnd(sig, idx);
                String bound = sig.substring(idx, typeEnd);
                bounds.add(formatType(bound));
                idx = typeEnd;

                if (idx < sig.length() && sig.charAt(idx) == ':') {
                    idx++; // еще одна bound
                }
            }

            out.add(new TypeVariable(name, bounds));
        }

        return idx;
    }

    private static String parseWildcardBound(String desc, String keyword) {
        int end = findTypeEnd(desc, 1);
        String bound = desc.substring(1, end);
        return "? " + keyword + " " + formatType(bound);
    }

    /**
     * Преобразует дескриптор type variable вида TName; в строку Name
     */
    private static String formatTypeVariable(String descriptor) {
        if (descriptor == null) {
            return null;
        }
        int start = descriptor.indexOf('T');
        int end = descriptor.indexOf(';', start + 1);
        if (start < 0 || end < 0 || end <= start + 1) {
            return descriptor;
        }
        return descriptor.substring(start + 1, end);
    }

    /**
     * Type variable с bounds.
     */
    public record TypeVariable(String name, List<String> bounds) {
        @NotNull
        @Override
        public String toString() {
            if (bounds.isEmpty()) return name;
            if (bounds.size() == 1) return name + " extends " + bounds.getFirst();
            return name + " extends " + String.join(" & ", bounds);
        }
    }

    /**
     * Method generic signature.
     */
    public record MethodGenericSignature(List<TypeVariable> typeVariables, List<String> parameterTypes,
                                         String returnType) {
    }
}
