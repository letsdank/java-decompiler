package net.letsdank.jd.decompiler.cfg;

import net.letsdank.jd.bytecode.Opcode;
import net.letsdank.jd.bytecode.insn.Insn;
import net.letsdank.jd.bytecode.insn.JumpInsn;
import net.letsdank.jd.fixtures.SimpleMethods;
import net.letsdank.jd.io.ClassFileReader;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.CodeAttribute;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.MethodInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ControlFlowGraphTest {
    @Test
    void absMethodHasConditionalBranchBlockWithTwoSuccessors() throws IOException {
        // Загружаем .class SimpleMethods
        InputStream in = SimpleMethods.class.getResourceAsStream("SimpleMethods.class");
        assertNotNull(in, "Failed to load SimpleMethods.class");

        ClassFileReader reader = new ClassFileReader();
        ClassFile cf = reader.read(in);
        ConstantPool cp = cf.constantPool();

        // Находим метод abs(I)I
        MethodInfo target = null;
        for (MethodInfo m : cf.methods()) {
            String name = cp.getUtf8(m.nameIndex());
            String desc = cp.getUtf8(m.descriptorIndex());
            if ("abs".equals(name) && "(I)I".equals(desc)) {
                target = m;
                break;
            }
        }
        assertNotNull(target, "abs(int) method not found");

        CodeAttribute codeAttr = target.findCodeAttribute();
        assertNotNull(codeAttr, "abs(int) must have Code");

        byte[] code = codeAttr.code();
        CfgBuilder cfgBuilder = new CfgBuilder();
        ControlFlowGraph cfg = cfgBuilder.build(code);

        List<BasicBlock> blocks = cfg.blocks();
        assertFalse(blocks.isEmpty(), "CFG must have at least one block");

        // Ищем блок, заканчивающийся условным переходом
        BasicBlock condBlock = null;
        for (BasicBlock bb : blocks) {
            List<Insn> insns = bb.instructions();
            if (insns.isEmpty()) continue;
            Insn last = insns.get(insns.size() - 1);
            if (last instanceof JumpInsn j && isConditional(j.opcode())) {
                condBlock = bb;
                break;
            }
        }

        assertNotNull(condBlock, "No conditional branch block found in abs(int)");

        // У условного блока должно быть два successors
        assertEquals(2, condBlock.successors().size(), "Conditional block should have 2 successors");
    }

    private static boolean isConditional(Opcode opcode) {
        return switch (opcode) {
            case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE,
                 IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE -> true;
            default -> false;
        };
    }
}