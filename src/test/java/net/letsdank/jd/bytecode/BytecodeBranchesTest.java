package net.letsdank.jd.bytecode;

import net.letsdank.jd.bytecode.insn.Insn;
import net.letsdank.jd.bytecode.insn.JumpInsn;
import net.letsdank.jd.fixtures.SimpleMethods;
import net.letsdank.jd.io.ClassFileReader;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.attribute.CodeAttribute;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.MethodInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeBranchesTest {

    @Test
    void absMethodHasBranchAndValidTarget() throws IOException {
        InputStream in = SimpleMethods.class.getResourceAsStream("SimpleMethods.class");
        assertNotNull(in, "Failed to load SimpleMethods.class");

        ClassFileReader reader = new ClassFileReader();
        ClassFile cf = reader.read(in);
        ConstantPool cp = cf.constantPool();

        MethodInfo target = null;
        for (MethodInfo m : cf.methods()) {
            String name = cp.getUtf8(m.nameIndex());
            String desc = cp.getUtf8(m.descriptorIndex());
            if ("abs".equals(name) && "(I)I".equals(desc)) {
                target = m;
                break;
            }
        }
        assertNotNull(target, "Method abs(int) not found");

        CodeAttribute codeAttr = target.findCodeAttribute();
        assertNotNull(codeAttr);

        byte[] code = codeAttr.code();
        BytecodeDecoder decoder = new BytecodeDecoder();
        List<Insn> insns = decoder.decode(code);

        // Должен быть хотя бы один JumpInsn (if / goto)
        long jumpCount = insns.stream().filter(i -> i instanceof JumpInsn).count();
        assertTrue(jumpCount >= 1, "Expected at least one branch instruction");

        // У всех переходов targetOffset должен быть в пределах кода
        for (Insn insn : insns) {
            if (insn instanceof JumpInsn j) {
                int jumpTarget = j.targetOffset();
                assertTrue(jumpTarget >= 0 && jumpTarget <= code.length,
                        "Jump target out of range: " + jumpTarget + " for offset " + j.offset());
            }
        }
    }
}
