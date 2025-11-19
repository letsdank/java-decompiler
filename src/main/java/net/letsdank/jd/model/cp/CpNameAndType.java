package net.letsdank.jd.model.cp;

public record CpNameAndType(int tag, int nameIndex, int descriptorIndex) implements CpInfo {
}
