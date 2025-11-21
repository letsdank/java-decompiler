package net.letsdank.jd.model;

import net.letsdank.jd.model.cp.CpClass;
import net.letsdank.jd.model.cp.CpInfo;
import net.letsdank.jd.model.cp.CpInteger;
import net.letsdank.jd.model.cp.CpUtf8;

/**
 * Обертка над массивом constant pool.
 * Индексация 1..count-1 (элемент 0 не используется).
 */
public final class ConstantPool {
    private final CpInfo[] entries; // длина = cpCount

    public ConstantPool(CpInfo[] entries) {
        this.entries = entries;
    }

    public int size() {
        return entries.length;
    }

    public CpInfo entry(int index) {
        if (index <= 0 || index >= entries.length) {
            throw new IndexOutOfBoundsException("cp index: " + index);
        }
        return entries[index];
    }

    public Integer getInteger(int index) {
        CpInfo e = entry(index);
        if (e instanceof CpInteger ci) {
            return ci.value();
        }
        throw new IllegalStateException("Expected CONSTANT_Integer at #" + index +
                " but was " + e.getClass().getSimpleName());
    }

    public String getUtf8(int index) {
        CpInfo e = entry(index);
        if (e instanceof CpUtf8 utf8) {
            return utf8.value();
        }
        throw new IllegalStateException("Expected CONSTANT_Utf8 at #" + index +
                " but was " + e.getClass().getSimpleName());
    }

    public String getClassName(int index) {
        CpInfo e = entry(index);
        if (e instanceof CpClass cls) {
            return getUtf8(cls.nameIndex());
        }
        throw new IllegalStateException("Expected CONSTANT_Class at #" + index +
                " but was " + e.getClass().getSimpleName());
    }

    /**
     * Удобный метод для тестов: есть ли в пуле такая строка.
     */
    public boolean containsUtf8(String value) {
        for (int i = 1; i < entries.length; i++) {
            CpInfo e = entries[i];
            if (e instanceof CpUtf8 utf8 && value.equals(utf8.value())) {
                return true;
            }
        }
        return false;
    }
}
