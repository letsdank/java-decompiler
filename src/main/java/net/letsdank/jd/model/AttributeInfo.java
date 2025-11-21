package net.letsdank.jd.model;

public sealed interface AttributeInfo
        permits CodeAttribute, RawAttribute, RuntimeVisibleAnnotationsAttribute {
    String name();
}
