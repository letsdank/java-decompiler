package net.letsdank.jd.gui;

import net.letsdank.jd.io.ClassFileReader;
import net.letsdank.jd.model.ClassFile;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Простейшее GUI: слева дерево, справа панель с текстом
 * Пока открывает один .class и показывает базовую инфу.
 */
public final class DecompilerFrame extends JFrame {
    private final JTree tree;
    private final JTextArea textArea;

    public DecompilerFrame() {
        super("Java Decompiler");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Левая панель: дерево классов/пакетов
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("No file");
        tree = new JTree(new DefaultTreeModel(root));
        JScrollPane treeScroll = new JScrollPane(tree);

        // Правая панель: текст (будущий Java-код)
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane textScroll = new JScrollPane(textArea);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, textScroll);
        split.setDividerLocation(250);

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
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(file.getName());
        DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(cf.thisClassFqn());
        root.add(classNode);

        // Пока добавим детей-узлов: "Fields и "Methods"
        DefaultMutableTreeNode fieldsNode = new DefaultMutableTreeNode("Fields (" + cf.fields().length + ")");
        DefaultMutableTreeNode methodsNode = new DefaultMutableTreeNode("Methods (" + cf.methods().length + ")");

        classNode.add(fieldsNode);
        classNode.add(methodsNode);

        ((DefaultTreeModel) tree.getModel()).setRoot(root);
        expandAll(tree);

        // Обновим правую панель: пока просто информация о классе
        StringBuilder sb = new StringBuilder();
        sb.append("Class: ").append(cf.thisClassFqn()).append('\n');
        sb.append("Super: ").append(cf.superClassIndex()).append('\n');
        sb.append("Major: ").append(cf.majorVersion())
                .append("  Minor: ").append(cf.minorVersion()).append('\n');
        sb.append("Fields: ").append(cf.fields().length).append('\n');
        sb.append("Methods: ").append(cf.methods().length).append('\n');

        textArea.setText(sb.toString());
        textArea.setCaretPosition(0);
    }

    private static void expandAll(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }
}
