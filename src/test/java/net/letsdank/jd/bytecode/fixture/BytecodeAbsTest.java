package net.letsdank.jd.bytecode.fixture;

import net.letsdank.jd.bytecode.BytecodeDecoder;
import net.letsdank.jd.bytecode.Opcode;
import net.letsdank.jd.bytecode.insn.Insn;
import net.letsdank.jd.bytecode.insn.JumpInsn;
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

 class BytecodeAbsTest {

     @Test
     void absMethodUsesIfltForBranching()throws IOException {
         InputStream in = SimpleMethods.class.getResourceAsStream("SimpleMethods.class");
         assertNotNull(in);

         ClassFileReader reader = new ClassFileReader();
         ClassFile cf = reader.read(in);
         ConstantPool cp = cf.constantPool();

         MethodInfo abs = JDUtils.findMethod(cf,cp,"abs","(I)I");
         CodeAttribute codeAttr = abs.findCodeAttribute();
         assertNotNull(codeAttr);

         byte[]code = codeAttr.code();
         BytecodeDecoder decoder = new BytecodeDecoder();
         List<Insn> insns = decoder.decode(code);

         // находим первый JumpInsn
         JumpInsn jump = insns.stream()
                 .filter(i->i instanceof JumpInsn)
                 .map(i->(JumpInsn)i)
                 .findFirst()
                 .orElseThrow(()->new AssertionError("No JumpInsn in abs"));

         assertEquals(Opcode.IFLT, jump.opcode(),"abs must use IFLT for branching");
         // sanity^ target должен указывать на вторую ветку
         assertTrue(jump.targetOffset() > jump.offset(), "IFLT target should be forward (else-branch)");
     }

     @Test
     void absMethodHasConditionalBranch()throws IOException {
         InputStream in = SimpleMethods.class.getResourceAsStream("SimpleMethods.class");
         assertNotNull(in);

         ClassFileReader reader = new ClassFileReader();
         ClassFile cf = reader.read(in);
         ConstantPool cp = cf.constantPool();

         MethodInfo abs = JDUtils.findMethod(cf,cp,"abs","(I)I");
         CodeAttribute codeAttr = abs.findCodeAttribute();
         assertNotNull(codeAttr);

         BytecodeDecoder decoder=new BytecodeDecoder();
         List<Insn> insns = decoder.decode(codeAttr.code());

         boolean hasCond = insns.stream().anyMatch(i->
                 i instanceof JumpInsn j && JDUtils.isConditional(j.opcode()));
         assertTrue(hasCond,"abs(int) must contain conditional branch");
     }
 }
