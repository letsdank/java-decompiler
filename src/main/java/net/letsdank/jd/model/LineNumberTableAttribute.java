package net.letsdank.jd.model;

import java.util.List;

public record LineNumberTableAttribute(List<Entry> entries) {
    public record Entry(int startPc, int lineNumber) {
    }

    /**
     * Находит номер строки для байткодового offset'а.
     */
    public int lineForOffset(int offset) {
        int current = -1;
        for (Entry e : entries) {
            if (e.startPc() <= offset) current = e.lineNumber();
            else break;
        }
        return current; // -1 если ничего не нашли
    }
}
