package net.letsdank.jd.model;

import net.letsdank.jd.model.attribute.AttributeInfo;

public record FieldInfo(int accessFlags, int nameIndex, int descriptorIndex, AttributeInfo[] attributes) {
}
