package net.letsdank.jd.model;

import net.letsdank.jd.model.attribute.AttributeInfo;
import net.letsdank.jd.model.attribute.CodeAttribute;

import java.util.Arrays;

public record MethodInfo(int accessFlags, int nameIndex, int descriptorIndex, AttributeInfo[] attributes) {
    public CodeAttribute findCodeAttribute() {
        return Arrays.stream(attributes)
                .filter(a -> a instanceof CodeAttribute)
                .map(a -> (CodeAttribute) a)
                .findFirst()
                .orElse(null);
    }
}
