package net.letsdank.jd.bytecode.insn;

import net.letsdank.jd.bytecode.Opcode;

/**
 * Инструкция с простым целочисленным операндом (byte/short и т.п.).
 */
public record IntOperandInsn(int offset, Opcode opcode, int operand) implements Insn {
}
