package net.letsdank.jd.decompiler.cfg;

import net.letsdank.jd.bytecode.insn.Insn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Базовый блок: последовательность инструкций без ветвлений внутри.
 */
public final class BasicBlock {
    private final int id;
    private final int startOffset;
    private final List<Insn> instructions = new ArrayList<>();
    private final List<BasicBlock> successors = new ArrayList<>();

    public BasicBlock(int id, int startOffset) {
        this.id = id;
        this.startOffset = startOffset;
    }

    public int id() {
        return id;
    }

    public int startOffset() {
        return startOffset;
    }

    public List<Insn> instructions() {
        return Collections.unmodifiableList(instructions);
    }

    public List<BasicBlock> successors() {
        return Collections.unmodifiableList(successors);
    }

    void addInstruction(Insn insn) {
        instructions.add(insn);
    }

    void addSuccessor(BasicBlock succ) {
        if (!successors.contains(succ)) {
            successors.add(succ);
        }
    }

    @Override
    public String toString() {
        return "BB#" + id + " @ " + startOffset +
                " (insns=" + instructions.size() +
                ", succ=" + successors.size() + ")";
    }
}
