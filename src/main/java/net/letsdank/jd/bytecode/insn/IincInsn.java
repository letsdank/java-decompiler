package net.letsdank.jd.bytecode.insn;

import net.letsdank.jd.bytecode.Opcode;

public record IincInsn(int offset, Opcode opcode, int localIndex, int delta) implements Insn {
}
