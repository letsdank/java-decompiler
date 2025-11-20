package net.letsdank.jd.cfg;

import net.letsdank.jd.bytecode.BytecodeDecoder;
import net.letsdank.jd.bytecode.Opcode;
import net.letsdank.jd.bytecode.insn.Insn;
import net.letsdank.jd.bytecode.insn.JumpInsn;
import net.letsdank.jd.bytecode.insn.SimpleInsn;

import java.util.*;

/**
 * Строит CFG (basic blocks) по байткоду метода.
 */
public final class CfgBuilder {
    private final BytecodeDecoder decoder = new BytecodeDecoder();

    public ControlFlowGraph build(byte[] code) {
        List<Insn> insns = decoder.decode(code);
        return buildFromInsns(insns);
    }

    public ControlFlowGraph buildFromInsns(List<Insn> insns) {
        if (insns.isEmpty()) {
            return new ControlFlowGraph(List.of(), null, Map.of());
        }

        // 1. лидеры блоков
        Set<Integer> leaders = new HashSet<>();
        leaders.add(insns.get(0).offset());

        for (int i = 0; i < insns.size(); i++) {
            Insn insn = insns.get(i);
            if (insn instanceof JumpInsn j) {
                leaders.add(j.targetOffset());
                if (i + 1 < insns.size()) {
                    leaders.add(insns.get(i + 1).offset());
                }
            }
        }

        List<Integer> leaderList = new ArrayList<>(leaders);
        Collections.sort(leaderList);

        // 2. создаем блоки
        List<BasicBlock> blocks = new ArrayList<>();
        Map<Integer, BasicBlock> byStart = new HashMap<>();

        for (int i = 0; i < leaderList.size(); i++) {
            int startOffset = leaderList.get(i);
            BasicBlock bb = new BasicBlock(i, startOffset);
            blocks.add(bb);
            byStart.put(startOffset, bb);
        }

        // 3. раскидываем инструкции по блокам
        for (Insn insn : insns) {
            BasicBlock owner = findOwnerBlockForInsn(blocks, insn.offset());
            if (owner == null) {
                // такого быть не должно, но на всякий случай
                continue;
            }
            owner.addInstruction(insn);
        }

        // 4. проставляем successors
        for (int i = 0; i < blocks.size(); i++) {
            BasicBlock bb = blocks.get(i);
            if (bb.instructions().isEmpty()) continue;

            Insn last = bb.instructions().get(bb.instructions().size() - 1);
            if (last instanceof JumpInsn j) {
                // переход на цель
                BasicBlock target = byStart.get(j.targetOffset());
                if (target != null) {
                    bb.addSuccessor(target);
                }

                if (isUnconditionalGoto(j)) {
                    // безусловный переход - больше нет successors
                    continue;
                } else {
                    // условный if_xxx - еще и fall-through блок
                    BasicBlock fallthrough = findNextBlock(blocks, bb);
                    if (fallthrough != null) {
                        bb.addSuccessor(fallthrough);
                    }
                }
            } else if (isReturn(last)) {
                // return - нет successors
                continue;
            } else {
                // обычный конец блока: fall-through, если есть следующий
                BasicBlock fallthrough = findNextBlock(blocks, bb);
                if (fallthrough != null) {
                    bb.addSuccessor(fallthrough);
                }
            }
        }

        BasicBlock entry = blocks.get(0);
        return new ControlFlowGraph(blocks, entry, byStart);
    }

    private static BasicBlock findOwnerBlockForInsn(List<BasicBlock> blocks, int offset) {
        // Блок с максимальным startOffset <= offset
        BasicBlock result = null;
        for (BasicBlock bb : blocks) {
            if (bb.startOffset() <= offset) {
                if (result == null || bb.startOffset() > result.startOffset()) {
                    result = bb;
                }
            }
        }
        return result;
    }

    private static BasicBlock findNextBlock(List<BasicBlock> blocks, BasicBlock current) {
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i) == current) {
                if (i + 1 < blocks.size()) {
                    return blocks.get(i + 1);
                }
                return null;
            }
        }
        return null;
    }

    private static boolean isUnconditionalGoto(JumpInsn j) {
        return j.opcode() == Opcode.GOTO;
    }

    private static boolean isReturn(Insn insn) {
        if (insn instanceof SimpleInsn s) {
            return s.opcode() == Opcode.RETURN || s.opcode() == Opcode.IRETURN;
        }
        return false;
    }
}
