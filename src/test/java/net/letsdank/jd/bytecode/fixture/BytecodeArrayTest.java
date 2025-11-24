package net.letsdank.jd.bytecode.fixture;

import net.letsdank.jd.bytecode.BytecodeDecoder;
import net.letsdank.jd.bytecode.Opcode;
import net.letsdank.jd.bytecode.insn.Insn;
import net.letsdank.jd.bytecode.insn.SimpleInsn;
import net.letsdank.jd.fixtures.ArrayFixtures;
import net.letsdank.jd.io.ClassFileReader;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.MethodInfo;
import net.letsdank.jd.model.attribute.CodeAttribute;
import net.letsdank.jd.utils.JDUtils;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BytecodeArrayTest {

    @Test
    void testArrayOpcodesPresent() throws Exception {
        try (InputStream in = ArrayFixtures.class.getResourceAsStream("ArrayFixtures.class")) {
            ClassFileReader reader = new ClassFileReader();
            ClassFile cf = reader.read(in);
            ConstantPool cp = cf.constantPool();

            MethodInfo sum = JDUtils.findMethod(cf,cp,"sum","([I)I");
            CodeAttribute codeAttr = sum.findCodeAttribute();

            BytecodeDecoder decoder = new BytecodeDecoder();
            List<Insn> insns = decoder.decode(codeAttr.code());

            var simpleOpcodes = insns.stream()
                    .filter(i->i instanceof SimpleInsn)
                    .map(i -> ((SimpleInsn)i).opcode())
                    .toList();

            assertTrue(simpleOpcodes.contains(Opcode.IALOAD), "sum([I)I must contain IALOAD");
            assertTrue(simpleOpcodes.contains(Opcode.ARRAYLENGTH), "sum([I)I must contain ARRAYLENGTH");
        }
    }
}
