package net.letsdank.jd.cfg;

import net.letsdank.jd.bytecode.Opcode;
import net.letsdank.jd.bytecode.insn.Insn;
import net.letsdank.jd.bytecode.insn.JumpInsn;
import net.letsdank.jd.bytecode.insn.SimpleInsn;
import net.letsdank.jd.fixtures.SimpleMethods;
import net.letsdank.jd.io.ClassFileReader;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.attribute.CodeAttribute;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.MethodInfo;
import net.letsdank.jd.utils.JDUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ControlFlowGraphTest {
    @Test
    void addMethodHasSingleLinearBlock() throws IOException {
        InputStream in = SimpleMethods.class.getResourceAsStream("SimpleMethods.class");
        assertNotNull(in);

        ClassFileReader reader = new ClassFileReader();
        ClassFile cf = reader.read(in);
        ConstantPool cp = cf.constantPool();

        MethodInfo add = JDUtils.findMethod(cf, cp, "add", "(II)I");
        CodeAttribute codeAttr = add.findCodeAttribute();
        assertNotNull(codeAttr, "add(int, int) must have Code");

        byte[] code = codeAttr.code();
        CfgBuilder cfgBuilder = new CfgBuilder();
        ControlFlowGraph cfg = cfgBuilder.build(code);

        // Для простого add ожидаем либо 1 блок, либо несколько,
        // но без условных ветвлений. Проверим отсутствие JumpInsn.
        boolean hasJump = cfg.blocks().stream()
                .flatMap(bb -> bb.instructions().stream())
                .anyMatch(i -> i instanceof JumpInsn);

        assertFalse(hasJump, "add(int,int) CFG must not contain branches");
    }

    @Test
    void absMethodHasConditionalBranchBlockWithTwoSuccessors() throws IOException {
        // Загружаем .class SimpleMethods
        InputStream in = SimpleMethods.class.getResourceAsStream("SimpleMethods.class");
        assertNotNull(in, "Failed to load SimpleMethods.class");

        ClassFileReader reader = new ClassFileReader();
        ClassFile cf = reader.read(in);
        ConstantPool cp = cf.constantPool();

        MethodInfo abs = JDUtils.findMethod(cf, cp, "abs", "(I)I");
        CodeAttribute codeAttr = abs.findCodeAttribute();
        assertNotNull(codeAttr, "abs(int) must have Code");

        byte[] code = codeAttr.code();
        CfgBuilder cfgBuilder = new CfgBuilder();
        ControlFlowGraph cfg = cfgBuilder.build(code);

        List<BasicBlock> blocks = cfg.blocks();
        assertFalse(blocks.isEmpty(), "CFG must have at least one block");

        // Ищем блок, заканчивающийся условным переходом
        BasicBlock condBlock = null;
        JumpInsn condJump = null;

        for (BasicBlock bb : blocks) {
            List<Insn> insns = bb.instructions();
            if (insns.isEmpty()) continue;
            Insn last = insns.get(insns.size() - 1);
            if (last instanceof JumpInsn j && JDUtils.isConditional(j.opcode())) {
                condBlock = bb;
                condJump = j;
                break;
            }
        }

        assertNotNull(condBlock, "No conditional branch block found in abs(int)");

        // У условного блока должно быть два successors
        assertEquals(2, condBlock.successors().size(),
                "Conditional block should have 2 successors");

        // Проверяем, что обе ветки ведут к return (возможно, через один блок)
        for (BasicBlock succ : condBlock.successors()) {
            assertTrue(pathLeadsToReturn(cfg, succ), "Successor path must lead to return");
        }
    }

    private boolean pathLeadsToReturn(ControlFlowGraph cfg, BasicBlock start) {
        Set<BasicBlock> visited = new HashSet<>();
        Deque<BasicBlock> stack = new ArrayDeque<>();
        stack.push(start);

        while (!stack.isEmpty()) {
            BasicBlock bb = stack.pop();
            if (!visited.add(bb)) continue;

            var insns = bb.instructions();
            if (!insns.isEmpty()) {
                Insn last = insns.get(insns.size() - 1);
                if (last instanceof SimpleInsn s &&
                        (s.opcode() == Opcode.IRETURN || s.opcode() == Opcode.RETURN)) {
                    return true;
                }
            }

            for (BasicBlock succ : bb.successors()) {
                stack.push(succ);
            }
        }

        return false;
    }
}