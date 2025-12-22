package net.letsdank.jd.bytecode.insn;

import net.letsdank.jd.bytecode.Opcode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Инструкция TABLESWITCH: хранит default-таргет и диапазон low..high,
 * а также абсолютные адреса таргетов для каждого кейса.
 */
public final class TableSwitchInsn implements Insn {
    private final int offset;
    private final Opcode opcode;
    private final int defaultTarget;
    private final int low;
    private final int high;
    private final Map<Integer, Integer> caseTargets; // value -> targetOffset

    public TableSwitchInsn(int offset, int defaultTarget, int low, int high, Map<Integer, Integer> caseTargets) {
        this.offset = offset;
        this.opcode = Opcode.TABLESWITCH;
        this.defaultTarget = defaultTarget;
        this.low = low;
        this.high = high;
        this.caseTargets = Collections.unmodifiableMap(new LinkedHashMap<>(caseTargets));
    }

    @Override
    public int offset() {
        return offset;
    }

    public Opcode opcode() {
        return opcode;
    }

    public int defaultTarget() {
        return defaultTarget;
    }

    public int low() {
        return low;
    }

    public int high() {
        return high;
    }

    public Map<Integer, Integer> caseTargets() {
        return caseTargets;
    }

    public Set<Integer> distinctTargets() {
        return Set.copyOf(caseTargets.values());
    }
}
