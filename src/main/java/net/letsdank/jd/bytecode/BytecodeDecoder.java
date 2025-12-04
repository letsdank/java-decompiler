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
                    insns.add(new SimpleInsn(start, opcode));
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
                case LOCAL_INDEX_U2 -> {
                    if (offset + 1 >= code.length) {
                        insns.add(new UnknownInsn(start, opByte, Arrays.copyOfRange(code, start, code.length)));
                        return insns;
                    }
                    int hi = code[offset] & 0xFF;
                    int lo = code[offset + 1] & 0xFF;
                    offset += 2;
                    int index = (hi << 8) | lo;
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
                    int delta = (short) ((hi << 8) | lo); // signed short
                    int target = start + delta; // offset уже указывает на следующую инструкцию
                    insns.add(new JumpInsn(start, opcode, target, delta));
                }
                case BRANCH_S4 -> {
                    // goto_w, jsr_w: delta:int
                    if (offset + 3 >= code.length) {
                        insns.add(new UnknownInsn(start, opByte, Arrays.copyOfRange(code, start, code.length)));
                        return insns;
                    }
                    int b1 = code[offset] & 0xFF;
                    int b2 = code[offset] & 0xFF;
                    int b3 = code[offset] & 0xFF;
                    int b4 = code[offset] & 0xFF;
                    offset += 4;
                    int delta = (b1 << 24) | (b2 << 16) | (b3 << 8) | b4; // signed int
                    int target = start + delta;
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
                case IINC -> {
                    // iinc <index:u1> <const:s1>
                    if (offset + 1 >= code.length) {
                        insns.add(new UnknownInsn(start, opByte, Arrays.copyOfRange(code, start, code.length)));
                        return insns;
                    }
                    int index = code[offset] & 0xFF;
                    int delta = (byte) code[offset + 1]; // signed
                    offset += 2;
                    insns.add(new IincInsn(start, opcode, index, delta));
                }
                case INVOKEDYNAMIC -> {
                    // invokedynamic <cp_index:u2> <0:u1> <0:u1>
                    if (offset + 3 >= code.length) {
                        insns.add(new UnknownInsn(start, opByte, Arrays.copyOfRange(code, start, code.length)));
                        return insns;
                    }
                    int hi = code[offset] & 0xFF;
                    int lo = code[offset + 1] & 0xFF;
                    int cpIndex = (hi << 8) | lo;
                    offset += 4; // пропускаем два "reserved" байта (обычно 0,0)
                    insns.add(new ConstantPoolInsn(start, opcode, cpIndex));
                }
                case TABLESWITCH -> {
                    // формат:
                    // [padding до 4-байтовой границы]
                    // default:int, low:int, high:int, затем (high-low+1) int-ов смещений
                    int relative = offset - start;
                    int pad = (4 - (relative & 0x3)) & 0x3;
                    if (offset + pad + 12 > code.length) {
                        insns.add(new UnknownInsn(start, opByte, Arrays.copyOfRange(code, start, code.length)));
                        return insns;
                    }
                    offset += pad;

                    int def = readInt(code, offset);
                    offset += 4;
                    int low = readInt(code, offset);
                    offset += 4;
                    int high = readInt(code, offset);
                    offset += 4;

                    long count = (long) high - (long) low + 1L;
                    if (count < 0 || offset + count * 4 > code.length) {
                        insns.add(new UnknownInsn(start, opByte, Arrays.copyOfRange(code, start, code.length)));
                        return insns;
                    }

                    // пропускаем все таргеты
                    offset += (int) (count * 4);

                    // Пока не сохраняем подробности - просто фиксируем, что здесь есть tableswitch
                    insns.add(new SimpleInsn(start, opcode));
                }
                case LOOKUPSWITCH -> {
                    // формат:
                    // [padding], default:int, npairs:int, затем npairs пар (match:int, offset:int)
                    int relative = offset - start;
                    int pad = (4 - (relative & 0x3)) & 0x3;
                    if (offset + pad + 8 > code.length) {
                        insns.add(new UnknownInsn(start, opByte, Arrays.copyOfRange(code, start, code.length)));
                        return insns;
                    }
                    offset += pad;

                    int def = readInt(code, offset);
                    offset += 4;
                    int npairs = readInt(code, offset);
                    offset += 4;

                    long total = (long) npairs * 8L;
                    if (npairs < 0 || offset + total > code.length) {
                        insns.add(new UnknownInsn(start, opByte, Arrays.copyOfRange(code, start, code.length)));
                        return insns;
                    }

                    offset += (int) total;

                    insns.add(new SimpleInsn(start, opcode));
                }
                case MULTIANEWARRAY -> {
                    // multianewarray cp_index:u2 dimensions:u1
                    if (offset + 2 >= code.length) {
                        insns.add(new UnknownInsn(start, opByte, Arrays.copyOfRange(code, start, code.length)));
                        return insns;
                    }
                    int hi = code[offset] & 0xFF;
                    int lo = code[offset + 1] & 0xFF;
                    int cpIndex = (hi << 8) | lo;
                    int dims = code[offset + 2] & 0xFF;
                    // Пока игнорируем dims, но хотя бы держим cpIndex
                    insns.add(new ConstantPoolInsn(start, opcode, cpIndex));
                }
                case WIDE -> {
                    // wide меняет формат следующей инструкции.
                    // Корректная поддержка потребует спец-логики.
                    // Пока честно зафиксируем, что дальше мы не умеем, и завершим декодирование.
                    byte[] remaining = Arrays.copyOfRange(code, start, code.length);
                    insns.add(new UnknownInsn(start, opByte, remaining));
                    return insns;
                }
            }
        }

        return insns;
    }

    private static int readInt(byte[] code, int offset) {
        int b1 = code[offset] & 0xFF;
        int b2 = code[offset + 1] & 0xFF;
        int b3 = code[offset + 2] & 0xFF;
        int b4 = code[offset + 3] & 0xFF;
        return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
    }
}
