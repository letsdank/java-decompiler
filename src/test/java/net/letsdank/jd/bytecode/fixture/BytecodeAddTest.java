package net.letsdank.jd.bytecode.fixture;

import net.letsdank.jd.bytecode.BytecodeDecoder;
import net.letsdank.jd.bytecode.Opcode;
import net.letsdank.jd.bytecode.insn.Insn;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeAddTest {

    @Test
    void addMethodIsSimpleLoadAddReturn() throws IOException {
        InputStream in = SimpleMethods.class.getResourceAsStream("SimpleMethods.class");
        assertNotNull(in);

        ClassFileReader reader = new ClassFileReader();
        ClassFile cf = reader.read(in);
        ConstantPool cp = cf.constantPool();

        MethodInfo add = JDUtils.findMethod(cf, cp, "add", "(II)I");
        CodeAttribute codeAttr = add.findCodeAttribute();
        assertNotNull(codeAttr);

        BytecodeDecoder decoder = new BytecodeDecoder();
        List<Insn> insns = decoder.decode(codeAttr.code());

        // В простейшем случае: iload_1, iload_2, iadd, ireturn
        var opcodes = insns.stream()
                .filter(i -> i instanceof SimpleInsn)
                .map(i -> ((SimpleInsn) i).opcode())
                .toList();

        assertTrue(opcodes.contains(Opcode.IADD), "add must contain IADD");
        assertTrue(opcodes.stream().anyMatch(op -> op == Opcode.IRETURN), "add must contain IRETURN");
    }
}
