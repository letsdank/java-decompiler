package net.letsdank.jd.model.attribute;

public record BootstrapMethodsAttribute(String name, BootstrapMethod[] methods)
        implements AttributeInfo {

    public record BootstrapMethod(int bootstrapMethodRef, int[] bootstrapArguments) {
    }
}
