package net.letsdank.jd.bytecode;

import net.letsdank.jd.bytecode.insn.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Примитивный декодер байткода.
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

            int start = offset;
            offset++; // ушли за опкод

            switch (opcode.operandType()) {
                case NONE -> {
                    insns.add(new SimpleInsn(offset, opcode));
                }
                case LOCAL_INDEX_U1 -> {
                    if (offset >= code.length) {
                        insns.add(new UnknownInsn(start, opByte, Arrays.copyOfRange(code, start, code.length)));
                        return insns;
                    }
                    int index = code[offset] & 0xFF;
                    offset++;
                    insns.add(new LocalVarInsn(start, opcode, index));
                }
                case BYTE_IMM -> {
                    if (offset >= code.length) {
                        insns.add(new UnknownInsn(start, opByte, Arrays.copyOfRange(code, start, code.length)));
                        return insns;
                    }
                    int imm = (byte) code[offset]; // signed
                    offset++;
                    insns.add(new IntOperandInsn(start, opcode, imm));
                }
                case SHORT_IMM -> {
                    if (offset + 1 >= code.length) {
                        insns.add(new UnknownInsn(start, opByte, Arrays.copyOfRange(code, start, code.length)));
                        return insns;
                    }
                    int hi = code[offset] & 0xFF;
                    int lo = code[offset + 1] & 0xFF;
                    offset += 2;
                    int imm = (short) ((hi << 8) | lo); // signed
                    insns.add(new IntOperandInsn(start, opcode, imm));
                }
                case BRANCH_S2 -> {
                    if (offset + 1 >= code.length) {
                        insns.add(new UnknownInsn(start, opByte, Arrays.copyOfRange(code, start, code.length)));
                        return insns;
                    }
                    int hi = code[offset] & 0xFF;
                    int lo = code[offset + 1] & 0xFF;
                    offset += 2;
                    int delta = (short) ((hi << 8) | lo); // signed offset
                    int target = offset + delta; // offset уже указывает на следующую инструкцию
                    insns.add(new JumpInsn(start, opcode, target, delta));
                }
                case CONSTPOOL_U1 -> {
                    if (offset >= code.length) {
                        insns.add(new UnknownInsn(start, opByte, Arrays.copyOfRange(code, start, code.length)));
                        return insns;
                    }
                    int cpIndex = code[offset] & 0xFF;
                    offset++;
                    insns.add(new ConstantPoolInsn(start, opcode, cpIndex));
                }
                case CONSTPOOL_U2 -> {
                    if (offset + 1 >= code.length) {
                        insns.add(new UnknownInsn(start, opByte, Arrays.copyOfRange(code, start, code.length)));
                        return insns;
                    }
                    int hi = code[offset] & 0xFF;
                    int lo = code[offset + 1] & 0xFF;
                    offset += 2;
                    int cpIndex = (hi << 8) | lo;
                    insns.add(new ConstantPoolInsn(start, opcode, cpIndex));
                }
            }
        }

        return insns;
    }
}
