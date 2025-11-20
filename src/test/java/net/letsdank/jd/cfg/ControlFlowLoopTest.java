package net.letsdank.jd.cfg;

import net.letsdank.jd.bytecode.BytecodeDecoder;
import net.letsdank.jd.bytecode.Opcode;
import net.letsdank.jd.bytecode.insn.Insn;
import net.letsdank.jd.bytecode.insn.JumpInsn;
import net.letsdank.jd.bytecode.insn.SimpleInsn;
import net.letsdank.jd.fixtures.SimpleMethods;
import net.letsdank.jd.io.ClassFileReader;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.CodeAttribute;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.MethodInfo;
import net.letsdank.jd.utils.JDUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ControlFlowLoopTest {

    @Test
    void loopMethodHasConditionalBackEdgeAndReturn() throws IOException {
        InputStream in = SimpleMethods.class.getResourceAsStream("SimpleMethods.class");
        assertNotNull(in);

        ClassFileReader reader = new ClassFileReader();
        ClassFile cf = reader.read(in);
        ConstantPool cp = cf.constantPool();

        MethodInfo loop = JDUtils.findMethod(cf, cp, "loop", "(I)V");
        CodeAttribute codeAttr = loop.findCodeAttribute();
        assertNotNull(codeAttr);

        byte[] code = codeAttr.code();

        // sanity: декодируем байткод
        BytecodeDecoder decoder = new BytecodeDecoder();
        List<Insn> insns = decoder.decode(code);
        assertFalse(insns.isEmpty());

        // CFG
        CfgBuilder cfgBuilder = new CfgBuilder();
        ControlFlowGraph cfg = cfgBuilder.build(code);
        List<BasicBlock> blocks = cfg.blocks();
        assertFalse(blocks.isEmpty());

        boolean hasCond = false;
        boolean hasBackEdge = false;
        boolean hasReturn = false;

        for (BasicBlock bb : blocks) {
            List<Insn> bInsns = bb.instructions();
            if (bInsns.isEmpty()) continue;

            Insn last = bInsns.getLast();

            // условный переход
            if (last instanceof JumpInsn j && JDUtils.isConditional(j.opcode())) {
                hasCond = true;
            }

            // back-edge: goto вверх
            if (last instanceof JumpInsn j && j.opcode() == Opcode.GOTO) {
                if (j.targetOffset() < j.offset()) {
                    hasBackEdge = true;
                }
            }

            // return
            if (last instanceof SimpleInsn s && s.opcode() == Opcode.RETURN) {
                hasReturn = true;
            }
        }

        assertTrue(hasCond, "loop(I)V must have conditional branch");
        assertTrue(hasBackEdge, "loop(I)V must have back-edge goto");
        assertTrue(hasReturn, "loop(I)V must have return");
    }
}
