package net.letsdank.jd.bytecode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Примитивный декодер байткода.
 * Сейчас поддерживает только инструкции без операндов,
 * и только те опкоды, которые есть в enum Opcode.
 */
public final class BytecodeDecoder {
    public List<Insn> decode(byte[] code) {
        List<Insn> insns = new ArrayList<>();
        int offset = 0;

        while (offset < code.length) {
            int opByte = code[offset] & 0xFF;
            Opcode opcode = Opcode.fromCode(opByte);
            if (opcode == null) {
                // Неизвестный/неподдерживаемый опкод - фиксируем и останавливаемся
                byte[] remaining = Arrays.copyOfRange(code, offset, code.length);
                insns.add(new UnknownInsn(offset, opByte, remaining));
                break;
            }

            // Пока все поддерживаемые считаем без операндов (длина - 1)
            insns.add(new SimpleInsn(offset, opcode));
            offset += 1;
        }

        return insns;
    }
}
