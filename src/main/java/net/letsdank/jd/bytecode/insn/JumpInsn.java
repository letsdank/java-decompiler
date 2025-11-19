package net.letsdank.jd.bytecode.insn;

import net.letsdank.jd.bytecode.Opcode;

/**
 * Инструкция перехода.
 * targetOffset = offset следующей инструкции + смещение (signed short).
 */
public record JumpInsn(int offset, Opcode opcode, int targetOffset, int rawOffsetDelta) implements Insn {
}
