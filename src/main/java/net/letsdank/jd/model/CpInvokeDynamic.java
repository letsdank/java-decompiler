package net.letsdank.jd.model;

public record CpInvokeDynamic(int tag, int bootstrapMethodAttrIndex, int nameAndTypeIndex) implements CpInfo {
}
