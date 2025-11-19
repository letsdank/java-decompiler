package net.letsdank.jd.bytecode;

/**
 * Простая инструкция без операндов (пока).
 */
public record SimpleInsn(int offset, Opcode opcode) implements Insn {
}
