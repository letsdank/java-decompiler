package net.letsdank.jd.utils;

import net.letsdank.jd.bytecode.Opcode;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.MethodInfo;

public class JDUtils {
    public static boolean isConditional(Opcode opcode) {
        return switch (opcode) {
            case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE,
                 IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE -> true;
            default -> false;
        };
    }

    public static MethodInfo findMethod(ClassFile cf, ConstantPool cp, String name, String desc) {
        for (MethodInfo m : cf.methods()) {
            String n = cp.getUtf8(m.nameIndex());
            String d = cp.getUtf8(m.descriptorIndex());
            if (name.equals(n) && desc.equals(d)) return m;
        }
        throw new IllegalArgumentException("Method not found: " + name + desc);
    }
}
