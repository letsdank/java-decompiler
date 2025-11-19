package net.letsdank.jd.model;

public record CpNameAndType(int tag, int nameIndex, int descriptorIndex) implements CpInfo {
}
