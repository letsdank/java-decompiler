package net.letsdank.jd.gui;

import net.letsdank.jd.ast.DecompilerOptions;
import net.letsdank.jd.ast.MethodDecompiler;
import net.letsdank.jd.bytecode.BytecodeDecoder;
import net.letsdank.jd.bytecode.insn.*;
import net.letsdank.jd.io.ClassFileReader;
import net.letsdank.jd.lang.Language;
import net.letsdank.jd.lang.LanguageBackend;
import net.letsdank.jd.lang.LanguageBackends;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.MethodInfo;
import net.letsdank.jd.model.attribute.CodeAttribute;
import net.letsdank.jd.model.cp.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Простейшее GUI: слева дерево, справа панель с текстом
 * Пока открывает один .class и показывает базовую инфу.
 */
public final class DecompilerFrame extends JFrame {
    private final JTree tree;
    private final JTextArea bytecodeArea;
    private final JTextArea javaArea;
    private final JTabbedPane tabbedPane;

    private JMenu recentFilesMenu;
    private final AppSettings settings;

    private LanguageBackend currentBackend = LanguageBackends.forLanguage(Language.JAVA);
    private final DecompilerOptions decompilerOptions = new DecompilerOptions();
    private final MethodDecompiler methodDecompiler = new MethodDecompiler(decompilerOptions);

    private File currentFile; // .class или .jar
    private boolean currentIsJar;

    public DecompilerFrame() {
        super("Java Decompiler");

        this.settings = AppSettings.load();

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
                Object userObject = dtmn.getUserObject();
                if (userObject instanceof ClassFile cf) {
                    // Если узел представляет ClassFile - покажем итог по классу
                    showClassSummary(cf);
                } else {
                    // Для других узлов покажем общую информацию по классу,
                    // если в корне лежит ClassFile
                    Object rootObj = ((DefaultMutableTreeNode) tree.getModel().getRoot()).getUserObject();
                    if (rootObj instanceof ClassFile cfRoot) {
                        showClassSummary(cfRoot);
                    }
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

        // Панель настроек слева (TODO: надо будет вынести в отдельное диалоговое окно)
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Settings"));

        JCheckBox hideIntrinsicsBox = new JCheckBox("Hide Kotlin Intrinsics", decompilerOptions.hideKotlinIntrinsics());
        hideIntrinsicsBox.addActionListener(e -> {
            decompilerOptions.setHideKotlinIntrinsics(hideIntrinsicsBox.isSelected());
            refreshCurrentSelection();
        });

        JCheckBox useKotlinxMetadataBox = new JCheckBox("Use Kotlin metadata library", true);
        useKotlinxMetadataBox.addActionListener(e -> {
            decompilerOptions.setUseKotlinxMetadata(useKotlinxMetadataBox.isSelected());
            refreshCurrentSelection();
        });

        settingsPanel.add(hideIntrinsicsBox);
        settingsPanel.add(useKotlinxMetadataBox);

        add(settingsPanel, BorderLayout.EAST);
        pack();

        // Меню "File -> Open .class"
        setJMenuBar(createMenuBar());

        setSize(900, 600);
        setLocationRelativeTo(null);
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");

        JMenuItem openClassItem = new JMenuItem("Open .class...");
        openClassItem.addActionListener(e -> openClassFile());

        JMenuItem openJarItem = new JMenuItem("Open .jar...");
        openJarItem.addActionListener(e -> openJarFile());

        JMenuItem reloadItem = new JMenuItem("Reload");
        reloadItem.addActionListener(e -> reloadCurrentFile());

        recentFilesMenu = new JMenu("Recent files");
        rebuildRecentFilesMenu();

        fileMenu.add(openClassItem);
        fileMenu.add(openJarItem);
        fileMenu.addSeparator();
        fileMenu.add(reloadItem);
        fileMenu.addSeparator();
        fileMenu.add(recentFilesMenu);

        bar.add(fileMenu);
        return bar;
    }

    private void refreshCurrentSelection() {
        Object node = tree.getLastSelectedPathComponent();
        if(node == null) return;

        if(node instanceof MethodTreeNode mNode) {
            showMethodDetails(mNode.classFile(), mNode.method());
        } else if (node instanceof DefaultMutableTreeNode dtmn) {
            Object userObject = dtmn.getUserObject();
            if(userObject instanceof ClassFile cf) {
                showClassSummary(cf);
            }
        }
    }

    private void openClassFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter("Class files (*.class)", "class"));

        if (settings.lastDirectory != null && settings.lastDirectory.isDirectory()) {
            chooser.setCurrentDirectory(settings.lastDirectory);
        }

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        openClassFile(file);
    }

    private void openClassFile(File file) {
        try (FileInputStream in = new FileInputStream(file)) {
            ClassFileReader reader = new ClassFileReader();
            ClassFile cf = reader.read(in);
            // Автоопределение языка на основе class-файла
            currentBackend = LanguageBackends.autoDetect(cf);

            currentFile = file;
            currentIsJar = false;

            settings.rememberFile(file);
            settings.rememberLastDirectory(file.getParentFile());
            settings.save();
            rebuildRecentFilesMenu();

            showClassFile(file, cf);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to read class file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace(System.err);
        }
    }

    private void openJarFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter("JAR files (*.jar)", "jar"));

        if (settings.lastDirectory != null && settings.lastDirectory.isDirectory()) {
            chooser.setCurrentDirectory(settings.lastDirectory);
        }

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        openJarFile(file);
    }

    private void openJarFile(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            ClassFileReader reader = new ClassFileReader();

            DefaultMutableTreeNode root = new DefaultMutableTreeNode(jarFile.getName());
            Map<String, DefaultMutableTreeNode> packageNodes = new HashMap<>();

            Enumeration<JarEntry> entries = jar.entries();
            boolean backendInitialized = false;

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                if (!name.endsWith(".class")) continue;

                try (InputStream in = jar.getInputStream(entry)) {
                    ClassFile cf = reader.read(in);
                    if (!backendInitialized) {
                        currentBackend = LanguageBackends.autoDetect(cf);
                        backendInitialized = true;
                    }

                    String fqcn = cf.thisClassFqn().replace('/', '.');
                    int lastDot = fqcn.lastIndexOf('.');
                    String pkg = (lastDot == -1) ? "" : fqcn.substring(0, lastDot);
                    String simpleName = (lastDot == -1) ? fqcn : fqcn.substring(lastDot + 1);

                    DefaultMutableTreeNode pkgNode = root;
                    if (!pkg.isEmpty()) {
                        pkgNode = packageNodes.computeIfAbsent(pkg, p -> {
                            DefaultMutableTreeNode node = new DefaultMutableTreeNode(p);
                            root.add(node);
                            return node;
                        });
                    }

                    DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(cf) {
                        @Override
                        public String toString() {
                            return simpleName;
                        }
                    };
                    pkgNode.add(classNode);

                    for (MethodInfo m : cf.methods()) {
                        classNode.add(new MethodTreeNode(cf, m));
                    }
                }
            }

            tree.setModel(new DefaultTreeModel(root));

            bytecodeArea.setText("Opened JAR: " + jarFile.getAbsolutePath()
                    + "\nSelect a class or method in the tree.");
            bytecodeArea.setCaretPosition(0);

            javaArea.setText("");
            javaArea.setCaretPosition(0);

            currentFile = jarFile;
            currentIsJar = true;

            settings.rememberFile(jarFile);
            settings.rememberLastDirectory(jarFile.getParentFile());
            settings.save();
            rebuildRecentFilesMenu();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to read jar file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace(System.err);
        }
    }

    private void reloadCurrentFile() {
        if (currentFile == null) return;
        if (!currentFile.exists()) {
            JOptionPane.showMessageDialog(this, "File no longer exists:\n" + currentFile,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (currentIsJar) openJarFile(currentFile);
        else openClassFile(currentFile);
    }

    private void rebuildRecentFilesMenu() {
        if (recentFilesMenu == null) return;

        recentFilesMenu.removeAll();

        List<File> files = settings.getRecentFiles();
        if (files.isEmpty()) {
            JMenuItem empty = new JMenuItem("(empty)");
            empty.setEnabled(false);
            recentFilesMenu.add(empty);
            return;
        }

        for (File f : files) {
            JMenuItem item = new JMenuItem(f.getAbsolutePath());
            item.addActionListener(e -> {
                if (f.getName().toLowerCase().endsWith(".jar")) {
                    openJarFile(f);
                } else {
                    openClassFile(f);
                }
            });
            recentFilesMenu.add(item);
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

        String source = currentBackend.decompileMethod(cf, method, methodDecompiler.decompile(method, cf));
        javaArea.setText(source);
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
        bytecodeArea.setCaretPosition(0);

        String source = currentBackend.decompileClass(cf, methodDecompiler);
        javaArea.setText(source);
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

    private static final class AppSettings {
        private static final String FILE_NAME = ".mini-jd-gui.properties";

        private final List<File> recentFiles = new ArrayList<>();
        private File lastDirectory;

        static AppSettings load() {
            AppSettings s = new AppSettings();
            File file = getSettingsFile();
            if (!file.exists()) return s;

            Properties p = new Properties();
            try (FileInputStream in = new FileInputStream(file)) {
                p.load(in);
            } catch (IOException e) {
                // Битый конфиг игнорируем, стартуем с пустыми настройками
                return s;
            }

            String lastDirStr = p.getProperty("lastDirectory");
            if (lastDirStr != null && !lastDirStr.isBlank()) {
                File dir = new File(lastDirStr);
                if (dir.isDirectory()) {
                    s.lastDirectory = dir;
                }
            }

            for (int i = 0; ; i++) {
                String path = p.getProperty("recent." + i);
                if (path == null) break;
                File f = new File(path);
                if (f.exists()) {
                    s.recentFiles.add(f);
                }
            }

            return s;
        }

        void save() {
            File file = getSettingsFile();
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }

            Properties p = new Properties();
            if (lastDirectory != null) {
                p.setProperty("lastDirectory", lastDirectory.getAbsolutePath());
            }

            int idx = 0;
            for (File f : recentFiles) {
                p.setProperty("recent." + idx++, f.getAbsolutePath());
            }

            try (FileOutputStream out = new FileOutputStream(file)) {
                p.store(out, "Mini JD GUI settings");
            } catch (IOException e) {
                // настройки - не критично, можно молча игнорировать
            }
        }

        List<File> getRecentFiles() {
            return Collections.unmodifiableList(recentFiles);
        }

        void rememberFile(File file) {
            recentFiles.removeIf(f -> f.equals(file));
            recentFiles.addFirst(file);
            while (recentFiles.size() > 10) {
                recentFiles.removeLast();
            }
        }

        void rememberLastDirectory(File dir) {
            if (dir != null && dir.isDirectory()) {
                lastDirectory = dir;
            }
        }

        private static File getSettingsFile() {
            String home = System.getProperty("user.home", ".");
            return new File(home, FILE_NAME);
        }
    }
}
