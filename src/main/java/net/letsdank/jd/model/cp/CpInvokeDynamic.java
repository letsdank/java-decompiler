package net.letsdank.jd.model.cp;

public record CpInvokeDynamic(int tag, int bootstrapMethodAttrIndex, int nameAndTypeIndex) implements CpInfo {
}
