package net.letsdank.jd.model;

import java.util.List;

public record LocalVariableTableAttribute(List<Entry> entries) {
    public record Entry(int startPc, int length, int nameIndex, int descriptorIndex, int index) {
    }
}
