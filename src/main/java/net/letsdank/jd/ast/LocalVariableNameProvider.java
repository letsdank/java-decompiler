package net.letsdank.jd.ast;

import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.LocalVariableTableAttribute;

import java.util.HashMap;
import java.util.Map;

/**
 * LocalNameProvider, который поверх базового прокидывает имена
 * из LocalVariableTable (LVT) - если есть debug-инфо.
 */
public final class LocalVariableNameProvider implements LocalNameProvider {
    private final LocalNameProvider fallback;
    private final Map<Integer, String> indexToName = new HashMap<>();

    public LocalVariableNameProvider(LocalNameProvider fallback,
                                     LocalVariableTableAttribute lvt,
                                     ConstantPool cp) {
        this.fallback = fallback;
        if (lvt != null) {
            for (LocalVariableTableAttribute.Entry e : lvt.entries()) {
                String name = cp.getUtf8(e.nameIndex());
                if (name != null && !name.isEmpty()) {
                    // не переопределяем уже записанное имя
                    indexToName.putIfAbsent(e.index(), name);
                }
            }
        }
    }

    @Override
    public String nameForLocal(int index) {
        String name = indexToName.get(index);
        if (name != null && !name.isEmpty()) return name;

        return fallback.nameForLocal(index);
    }
}
