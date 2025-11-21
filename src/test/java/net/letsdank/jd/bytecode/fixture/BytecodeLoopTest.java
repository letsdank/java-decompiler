package net.letsdank.jd.bytecode.fixture;

import net.letsdank.jd.bytecode.BytecodeDecoder;
import net.letsdank.jd.bytecode.Opcode;
import net.letsdank.jd.bytecode.insn.*;
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

class BytecodeLoopTest {

    @Test
    void loopMethodContainsIincGotoAndReturnAndNoUnknownInsns() throws IOException {
        InputStream in = SimpleMethods.class.getResourceAsStream("SimpleMethods.class");
        assertNotNull(in);

        ClassFileReader reader = new ClassFileReader();
        ClassFile cf = reader.read(in);
        ConstantPool cp = cf.constantPool();

        MethodInfo loop = JDUtils.findMethod(cf, cp, "loop", "(I)V");
        CodeAttribute codeAttr = loop.findCodeAttribute();
        assertNotNull(codeAttr);

        byte[] code = codeAttr.code();
        BytecodeDecoder decoder = new BytecodeDecoder();
        List<Insn> insns = decoder.decode(code);

        // не должно быть UnknownInsn
        boolean hasUnknown = insns.stream().anyMatch(i -> i instanceof UnknownInsn);
        assertFalse(hasUnknown, "loop(I)V must not contain UnknownInsn");

        boolean hasIinc = insns.stream()
                .anyMatch(i -> i instanceof IincInsn ii && ii.opcode() == Opcode.IINC);
        assertTrue(hasIinc, "loop(I)V must contain IINC");

        // переход назад (goto на начало) и return
        boolean hasGoto = insns.stream()
                .anyMatch(i -> i instanceof JumpInsn j && j.opcode() == Opcode.GOTO);
        assertTrue(hasGoto, "loop(I)V must contain GOTO");

        boolean hasReturn = insns.stream()
                .anyMatch(i -> i instanceof SimpleInsn s && s.opcode() == Opcode.RETURN);
        assertTrue(hasReturn, "loop(I)V must contain RETURN");
    }
}
