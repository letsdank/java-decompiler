package net.letsdank.jd.bytecode.insn;

/**
 * Базовый интерфейс инструкции.
 */
public sealed interface Insn
        permits SimpleInsn, LocalVarInsn, IntOperandInsn,
        JumpInsn, ConstantPoolInsn, UnknownInsn, IincInsn,
        TableSwitchInsn, LookupSwitchInsn {
    int offset();
}
