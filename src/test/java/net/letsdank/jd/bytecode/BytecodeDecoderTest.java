package net.letsdank.jd.bytecode;

import net.letsdank.jd.bytecode.insn.Insn;
import net.letsdank.jd.bytecode.insn.SimpleInsn;
import net.letsdank.jd.bytecode.insn.UnknownInsn;
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

class BytecodeDecoderTest {
    @Test
    void decodesSimpleAddMethod() throws IOException {
        // Загружаем .class SimpleMethods
        InputStream in = SimpleMethods.class.getResourceAsStream("SimpleMethods.class");
        assertNotNull(in, "Failed to load SimpleMethods.class");

        ClassFileReader reader = new ClassFileReader();
        ClassFile cf = reader.read(in);
        ConstantPool cp = cf.constantPool();

        // Ищем метод add(int,int) по имени и дескриптору "(II)I"
        MethodInfo target = null;
        for (MethodInfo m : cf.methods()) {
            String name = cp.getUtf8(m.nameIndex());
            String desc = cp.getUtf8(m.descriptorIndex());
            if ("add".equals(name) && "(II)I".equals(desc)) {
                target = m;
                break;
            }
        }
        assertNotNull(target, "Method add(int,int) not found");

        CodeAttribute codeAttr = target.findCodeAttribute();
        assertNotNull(codeAttr, "Method add must have Code attribute");

        byte[] code = codeAttr.code();
        assertTrue(code.length > 0, "Bytecode must not be empty");

        BytecodeDecoder decoder = new BytecodeDecoder();
        List<Insn> insns = decoder.decode(code);

        // Здесь невозможно не получить UnknownInsn в этом простом методе
        assertTrue(insns.stream().noneMatch(i -> i instanceof UnknownInsn),
                "There must be no UnknownInsn for simple add method");

        // Собираем список опкодов
        List<Opcode> opcodes = insns.stream()
                .map(i -> ((SimpleInsn) i).opcode())
                .toList();

        // Ожидаем iload_1, iload_2, iadd, ireturn
        assertEquals(4, opcodes.size(), "Unexpected number of opcodes: " + opcodes);
        assertEquals(Opcode.ILOAD_1, opcodes.get(0));
        assertEquals(Opcode.ILOAD_2, opcodes.get(1));
        assertEquals(Opcode.IADD, opcodes.get(2));
        assertEquals(Opcode.IRETURN, opcodes.get(3));
    }
}