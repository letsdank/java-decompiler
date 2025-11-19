package net.letsdank.jd.gui;

import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.MethodInfo;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Узел дерева, представляющий конкретный метод.
 */
public final class MethodTreeNode extends DefaultMutableTreeNode {
    private final ClassFile classFile;
    private final MethodInfo method;

    public MethodTreeNode(ClassFile classFile, MethodInfo method) {
        super(buildLabel(classFile, method));
        this.classFile = classFile;
        this.method = method;
    }

    private static String buildLabel(ClassFile cf, MethodInfo m) {
        ConstantPool cp = cf.constantPool();
        String name = cp.getUtf8(m.nameIndex());
        String desc = cp.getUtf8(m.descriptorIndex());
        return name + desc;
    }

    public ClassFile classFile() {
        return classFile;
    }

    public MethodInfo method() {
        return method;
    }
}
