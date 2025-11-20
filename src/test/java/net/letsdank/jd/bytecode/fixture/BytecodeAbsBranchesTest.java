package net.letsdank.jd.bytecode.fixture;

import net.letsdank.jd.bytecode.BytecodeDecoder;
import net.letsdank.jd.bytecode.insn.Insn;
import net.letsdank.jd.bytecode.insn.JumpInsn;
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

 class BytecodeAbsBranchesTest {

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
