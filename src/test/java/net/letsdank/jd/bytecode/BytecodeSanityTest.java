package net.letsdank.jd.bytecode;

import net.letsdank.jd.bytecode.insn.Insn;
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

class BytecodeSanityTest {

    @Test
    void instructionOffsetAreMonotonicAndWithinCodeRange() throws IOException {
        InputStream in = SimpleMethods.class.getResourceAsStream("SimpleMethods.class");
        assertNotNull(in);

        ClassFileReader reader = new ClassFileReader();
        ClassFile cf = reader.read(in);
        ConstantPool cp = cf.constantPool();
        BytecodeDecoder decoder = new BytecodeDecoder();

        for (MethodInfo m : cf.methods()) {
            CodeAttribute codeAttr = m.findCodeAttribute();
            if (codeAttr == null) continue; // skip abstract/native

            byte[] code = codeAttr.code();
            List<Insn> insns = decoder.decode(code);

            int prevOffset = -1;
            for (Insn insn : insns) {
                int o = insn.offset();
                assertTrue(o >= 0 && o < code.length,
                        "Insn offset out of range: " + o + " for method " +
                                cp.getUtf8(m.nameIndex()) + cp.getUtf8(m.descriptorIndex()));
                assertTrue(o >= prevOffset, "Insn offsets must be non-decreasing: prev=" +
                        prevOffset + ", curr=" + o);
                prevOffset = o;
            }
        }
    }
}
