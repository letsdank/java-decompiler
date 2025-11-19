package net.letsdank.jd.bytecode.insn;

import net.letsdank.jd.bytecode.Opcode;

/**
 * Инструкция, которая ссылается на constant pool (field/method/class/string/etc.)
 */
public record ConstantPoolInsn(int offset, Opcode opcode, int cpIndex) implements Insn {
}
