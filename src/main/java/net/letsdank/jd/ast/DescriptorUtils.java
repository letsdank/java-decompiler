package net.letsdank.jd.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Примитивный парсер дескрипторов JVM.
 * Поддерживает только то, что нужно для имен параметров.
 */
public final class DescriptorUtils {

    private DescriptorUtils() {
    }

    /**
     * Возвращает список типов параметров в виде дескрипторов:
     * "I", "J", "D", "Ljava.lang/String;" и т.п.
     */
    public static List<String> parseParameterTypes(String methodDescriptor) {
        // Пример: (ILjava/lang/String;J)V
        int pos = 0;
        if (methodDescriptor.charAt(pos) != '(') {
            throw new IllegalArgumentException("Not a method descriptor: " + methodDescriptor);
        }
        pos++;

        List<String> params = new ArrayList<>();
        while (methodDescriptor.charAt(pos) != ')') {
            char c = methodDescriptor.charAt(pos);
            if (c == 'L') {
                int semicolon = methodDescriptor.indexOf(';', pos);
                if (semicolon < 0) {
                    throw new IllegalArgumentException("Bad descriptor: " + methodDescriptor);
                }
                params.add(methodDescriptor.substring(pos, semicolon + 1));
                pos = semicolon + 1;
            } else if (c == '[') {
                // Массивы: считаем подряд [ и затем тип
                int start = pos;
                pos++;
                while (methodDescriptor.charAt(pos) == '[') {
                    pos++;
                }
                char elem = methodDescriptor.charAt(pos);
                if (elem == 'L') {
                    int semicolon = methodDescriptor.indexOf(';', pos);
                    if (semicolon < 0) {
                        throw new IllegalArgumentException("Bad descriptor: " + methodDescriptor);
                    }
                    pos = semicolon + 1;
                } else {
                    pos++;
                }
                params.add(methodDescriptor.substring(start, pos));
            } else {
                // Примитив: B C D F I J S Z
                params.add(String.valueOf(c));
                pos++;
            }
        }

        return params;
    }

    /**
     * Возвращает количество локальных слотов, которые занимает тип
     * (J и D занимают 2).
     */
    public static int slotsForType(String typeDescriptor) {
        char c = typeDescriptor.charAt(0);
        return switch (c) {
            case 'J', 'D' -> 2;     // long, double
            default -> 1;           // все остальное
        };
    }
}
