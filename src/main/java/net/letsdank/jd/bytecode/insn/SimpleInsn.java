package net.letsdank.jd.bytecode.insn;

import net.letsdank.jd.bytecode.Opcode;

/**
 * Простая инструкция без операндов (пока).
 */
public record SimpleInsn(int offset, Opcode opcode) implements Insn {
}
