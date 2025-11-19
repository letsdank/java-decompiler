package net.letsdank.jd.model;

public record FieldInfo(int accessFlags,int nameIndex,int descriptorIndex, AttributeInfo[] attributes) {
}
