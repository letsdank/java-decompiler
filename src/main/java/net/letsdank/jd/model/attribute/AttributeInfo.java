package net.letsdank.jd.model.attribute;

public sealed interface AttributeInfo
        permits CodeAttribute, RawAttribute, RuntimeVisibleAnnotationsAttribute,
        BootstrapMethodsAttribute, SignatureAttribute, ExceptionsAttribute {
    String name();
}
