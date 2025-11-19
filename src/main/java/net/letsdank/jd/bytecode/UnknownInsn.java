package net.letsdank.jd.bytecode;

/**
 * Инструкция, которую мы не умеем декодировать.
 * Используем, чтобы не "ломать" декодер на сложных методах:
 * он остановится на этой точке.
 */
public record UnknownInsn(int offset, int opcodeByte, byte[] remainingBytes) implements Insn {
}
