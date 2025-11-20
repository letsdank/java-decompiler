package net.letsdank.jd.gui;

import net.letsdank.jd.ast.JavaPrettyPrinter;
import net.letsdank.jd.ast.MethodAst;
import net.letsdank.jd.ast.MethodDecompiler;
import net.letsdank.jd.ast.stmt.BlockStmt;
import net.letsdank.jd.ast.stmt.Stmt;
import net.letsdank.jd.bytecode.BytecodeDecoder;
import net.letsdank.jd.bytecode.insn.*;
import net.letsdank.jd.io.ClassFileReader;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.CodeAttribute;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.MethodInfo;
import net.letsdank.jd.model.cp.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Простейшее GUI: слева дерево, справа панель с текстом
 * Пока открывает один .class и показывает базовую инфу.
 */
public final class DecompilerFrame extends JFrame {
    private final JTree tree;
    private final JTextArea bytecodeArea;
    private final JTextArea javaArea;
    private final JTabbedPane tabbedPane;

    private final MethodDecompiler methodDecompiler = new MethodDecompiler();

    public DecompilerFrame() {
        super("Java Decompiler");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Левая панель: дерево классов/пакетов
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("No file");
        tree = new JTree(new DefaultTreeModel(root));
        tree.addTreeSelectionListener(e -> {
            Object node = tree.getLastSelectedPathComponent();
            if (node == null) return;

            if (node instanceof MethodTreeNode mNode) {
                // Показать код конкретного метода
                showMethodDetails(mNode.classFile(), mNode.method());
            } else if (node instanceof DefaultMutableTreeNode dtmn) {
                // Для других узлов покажем общую информацию по классу,
                // если в корне лежит ClassFile
                Object rootObj = ((DefaultMutableTreeNode) tree.getModel().getRoot()).getUserObject();
                if (rootObj instanceof ClassFile cf) {
                    showClassSummary(cf);
                }
            }
        });

        // Правая панель: текст (будущий Java-код)
        bytecodeArea = new JTextArea();
        bytecodeArea.setEditable(false);
        bytecodeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        javaArea = new JTextArea();
        javaArea.setEditable(false);
        javaArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Bytecode", new JScrollPane(bytecodeArea));
        tabbedPane.addTab("Java", new JScrollPane(javaArea));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tree), tabbedPane);
        split.setDividerLocation(250);
        getContentPane().add(split, BorderLayout.CENTER);

        add(split, BorderLayout.CENTER);

        // Меню "File -> Open .class"
        setJMenuBar(createMenuBar());

        setSize(900, 600);
        setLocationRelativeTo(null);
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open .class...");
        openItem.addActionListener(e -> openClassFile());

        fileMenu.add(openItem);
        bar.add(fileMenu);
        return bar;
    }

    private void openClassFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        try (FileInputStream in = new FileInputStream(file)) {
            ClassFileReader reader = new ClassFileReader();
            ClassFile cf = reader.read(in);
            showClassFile(file, cf);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to read class file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace(System.err);
        }
    }

    private void showClassFile(File file, ClassFile cf) {
        // Обновим дерево
        // Корень дерева будет содержать сам ClassFile как userObject,
        // а имя файла - в toString()
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(file.getName()) {
            @Override
            public Object getUserObject() {
                return cf; // внутри держим ClassFile
            }

            @Override
            public String toString() {
                return file.getName();
            }
        };
        DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(cf.thisClassFqn());
        root.add(classNode);

        // Пока добавим детей-узлов: "Fields и "Methods"
        DefaultMutableTreeNode fieldsNode = new DefaultMutableTreeNode("Fields (" + cf.fields().length + ")");
        DefaultMutableTreeNode methodsNode = new DefaultMutableTreeNode("Methods (" + cf.methods().length + ")");

        classNode.add(fieldsNode);
        classNode.add(methodsNode);

        // Добавляем каждый метод отдельным узлом
        for (var m : cf.methods()) {
            MethodTreeNode mNode = new MethodTreeNode(cf, m);
            methodsNode.add(mNode);
        }

        ((DefaultTreeModel) tree.getModel()).setRoot(root);
        expandAll(tree);

        // По умолчанию показываем сводку по классу
        showClassSummary(cf);
    }

    private void showMethodDetails(ClassFile cf, MethodInfo method) {
        // --- заголовок ---
        ConstantPool cp = cf.constantPool();
        String className = cf.thisClassFqn();
        String methodName = cp.getUtf8(method.nameIndex());
        String desc = cp.getUtf8(method.descriptorIndex());

        // --- байткод ---
        StringBuilder bc = new StringBuilder();
        bc.append("Class: ").append(className).append('\n');
        bc.append("Method: ").append(methodName).append(desc).append('\n');
        bc.append('\n');

        CodeAttribute codeAttr = method.findCodeAttribute();
        if (codeAttr == null) {
            bc.append("<no code>\n");
            bytecodeArea.setText(bc.toString());
            bytecodeArea.setCaretPosition(0);

            javaArea.setText(bc.toString());
            javaArea.setCaretPosition(0);
            return;
        }

        byte[] code = codeAttr.code();
        BytecodeDecoder decoder = new BytecodeDecoder();
        List<Insn> insns = decoder.decode(code);

        bc.append("Bytecode:\n\n");
        for (Insn insn : insns) {
            if (insn instanceof SimpleInsn s) {
                bc.append(String.format("  %4d: %s\n",
                        s.offset(), s.opcode().mnemonic()));
            } else if (insn instanceof LocalVarInsn lv) {
                bc.append(String.format("  %4d: %s %d%n",
                        lv.offset(), lv.opcode().mnemonic(), lv.localIndex()));
            } else if (insn instanceof IincInsn inc) {
                bc.append(String.format("  %4d: %s %d %d%n",
                        inc.offset(), inc.opcode().mnemonic(), inc.localIndex(), inc.delta()));
            } else if (insn instanceof IntOperandInsn io) {
                bc.append(String.format("  %4d: %s %d%n",
                        io.offset(), io.opcode().mnemonic(), io.operand()));
            } else if (insn instanceof JumpInsn j) {
                bc.append(String.format("  %4d: %s %d  ; target=%d%n",
                        j.offset(), j.opcode().mnemonic(), j.rawOffsetDelta(), j.targetOffset()));
            } else if (insn instanceof ConstantPoolInsn cpi) {
                int cpIndex = cpi.cpIndex();
                String cpText = formatCpEntry(cp, cpIndex);
                bc.append(String.format("  %4d: %s #%d    ; %s%n",
                cpi.offset(), cpi.opcode().mnemonic(), cpIndex, cpText));
            } else if (insn instanceof UnknownInsn u) {
                bc.append(String.format("  %4d: <unknown opcode 0x%02X, %d bytes remaining>\n",
                        u.offset(), u.opcodeByte(), u.remainingBytes().length));
            }
        }

        bytecodeArea.setText(bc.toString());
        bytecodeArea.setCaretPosition(0);

        // --- Java (AST) ---

        MethodAst ast = methodDecompiler.decompile(method, cf);
        JavaPrettyPrinter printer = new JavaPrettyPrinter();
        String javaText = printer.printMethod(cf, method, ast);
        javaArea.setText(javaText);
        javaArea.setCaretPosition(0);
    }

    private void showClassSummary(ClassFile cf) {
        StringBuilder sb = new StringBuilder();
        sb.append("Class: ").append(cf.thisClassFqn()).append('\n');
        sb.append("Super: ").append(cf.superClassIndex()).append('\n');
        sb.append("Major: ").append(cf.majorVersion())
                .append("  Minor: ").append(cf.minorVersion()).append('\n');
        sb.append("Fields: ").append(cf.fields().length).append('\n');
        sb.append("Methods: ").append(cf.methods().length).append('\n');

        sb.append("Select a method in the tree to see its bytecode.\n\n");

        bytecodeArea.setText(sb.toString());
        javaArea.setText(sb.toString());
        bytecodeArea.setCaretPosition(0);
        javaArea.setCaretPosition(0);
    }

    private String formatCpEntry(ConstantPool cp, int index) {
        if (index <= 0 || index >= cp.size()) {
            return "#" + index;
        }

        CpInfo e = cp.entry(index);
        if (e instanceof CpClass cls) {
            String name = cp.getUtf8(cls.nameIndex());
            return name;
        }
        if (e instanceof CpString s) {
            String value = cp.getUtf8(s.stringIndex());
            return "\"" + value + "\"";
        }
        if (e instanceof CpFieldref fr) {
            String owner = cp.getClassName(fr.classIndex());
            CpNameAndType nt = (CpNameAndType) cp.entry(fr.nameAndTypeIndex());
            String name = cp.getUtf8(nt.nameIndex());
            String desc = cp.getUtf8(nt.descriptorIndex());
            return owner + "." + name + ":" + desc;
        }
        if (e instanceof CpMethodref mr) {
            String owner = cp.getClassName(mr.classIndex());
            CpNameAndType nt = (CpNameAndType) cp.entry(mr.nameAndTypeIndex());
            String name = cp.getUtf8(nt.nameIndex());
            String desc = cp.getUtf8(nt.descriptorIndex());
            return owner + "." + name + desc;
        }
        if (e instanceof CpInterfaceMethodref imr) {
            String owner = cp.getClassName(imr.classIndex());
            CpNameAndType nt = (CpNameAndType) cp.entry(imr.nameAndTypeIndex());
            String name = cp.getUtf8(nt.nameIndex());
            String desc = cp.getUtf8(nt.descriptorIndex());
            return owner + "." + name + desc;
        }

        // fallback
        return "#" + index + " (" + e.getClass().getSimpleName() + ")";
    }

    private static void expandAll(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }
}
