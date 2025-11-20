package net.letsdank.jd.ast;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LocalNameProvider, который знает про this и параметры метода.
 */
public final class MethodLocalNameProvider implements LocalNameProvider {
    private final boolean isStatic;
    private final Map<Integer, String> indexToName = new HashMap<>();

    public MethodLocalNameProvider(int accessFlags, String methodDescriptor) {
        this.isStatic = Modifier.isStatic(accessFlags);
        List<String> paramTypes = DescriptorUtils.parseParameterTypes(methodDescriptor);

        int slot = 0;
        if (!isStatic) {
            // локал 0 - это this
            indexToName.put(0, "this");
            slot = 1;
        }

        // Простенький генератор имен параметров: a, b, c, d, e, f..., потом argN
        String[] baseNames = {"a", "b", "c", "d", "e", "f"};
        int paramIndex = 0;

        for (String type : paramTypes) {
            String name;
            if (paramIndex < baseNames.length) {
                name = baseNames[paramIndex];
            } else {
                name = "arg" + paramIndex;
            }

            indexToName.put(slot, name);
            slot += DescriptorUtils.slotsForType(type);
            paramIndex++;
        }
    }

    @Override
    public String nameForLocal(int index) {
        String name = indexToName.get(index);
        if (name != null) return name;
        // запасной вариант - vN
        return "v" + index;
    }
}
