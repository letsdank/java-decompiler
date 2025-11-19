package net.letsdank.jd.bytecode.insn;

import net.letsdank.jd.bytecode.Opcode;

/**
 * Инструкция, у которой операнд - индекс локальной переменной.
 */
public record LocalVarInsn(int offset, Opcode opcode, int localIndex) implements Insn {
}
