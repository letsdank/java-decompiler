package net.letsdank.jd.bytecode;

import net.letsdank.jd.bytecode.insn.ConstantPoolInsn;
import net.letsdank.jd.bytecode.insn.Insn;
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

class ConstPoolInsnTest {
    @Test
    void printHelloHasGetstaticAndInvokevirtual() throws IOException {
        InputStream in = SimpleMethods.class.getResourceAsStream("SimpleMethods.class");
        assertNotNull(in);

        ClassFileReader reader = new ClassFileReader();
        ClassFile cf = reader.read(in);
        ConstantPool cp = cf.constantPool();

        MethodInfo target = null;
        for (MethodInfo m : cf.methods()) {
            String name = cp.getUtf8(m.nameIndex());
            String desc = cp.getUtf8(m.descriptorIndex());
            if ("printHello".equals(name) && "()V".equals(desc)) {
                target = m;
                break;
            }
        }
        assertNotNull(target, "printHello() not found");

        CodeAttribute codeAttr = target.findCodeAttribute();
        assertNotNull(codeAttr);

        BytecodeDecoder decoder = new BytecodeDecoder();
        List<Insn> insns = decoder.decode(codeAttr.code());

        boolean hasGetstatic = insns.stream()
                .anyMatch(i -> i instanceof ConstantPoolInsn cpi && cpi.opcode() == Opcode.GETSTATIC);
        boolean hasInvokevirtual = insns.stream()
                .anyMatch(i -> i instanceof ConstantPoolInsn cpi && cpi.opcode() == Opcode.INVOKEVIRTUAL);

        assertTrue(hasGetstatic, "Expected GETSTATIC in printHello");
        assertTrue(hasInvokevirtual, "Expected INVOKEVIRTUAL in printHello");
    }
}
