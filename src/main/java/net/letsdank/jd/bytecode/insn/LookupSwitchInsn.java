package net.letsdank.jd.bytecode.insn;

import net.letsdank.jd.bytecode.Opcode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Инструкция LOOKUPSWITCH: хранит default-таргет и пары (match -> target).
 */
public final class LookupSwitchInsn implements Insn {
    private final int offset;
    private final Opcode opcode;
    private final int defaultTarget;
    private final Map<Integer, Integer> matchTargets; // match -> targetOffset

    public LookupSwitchInsn(int offset, int defaultTarget, Map<Integer, Integer> matchTargets) {
        this.offset = offset;
        this.opcode = Opcode.LOOKUPSWITCH;
        this.defaultTarget = defaultTarget;
        this.matchTargets = Collections.unmodifiableMap(new LinkedHashMap<>(matchTargets));
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

    public Map<Integer, Integer> matchTargets() {
        return matchTargets;
    }

    public Set<Integer> distinctTargets() {
        return Set.copyOf(matchTargets.values());
    }
}
