package net.letsdank.jd.model.cp;

public record CpMethodref(int tag, int classIndex, int nameAndTypeIndex) implements CpInfo {
}
