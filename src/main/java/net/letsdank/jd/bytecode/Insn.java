package net.letsdank.jd.bytecode;

/**
 * Базовый интерфейс инструкции.
 */
public sealed interface Insn
permits SimpleInsn, UnknownInsn{
    int offset();
}
