package net.letsdank.jd.ast;

import net.letsdank.jd.ast.expr.*;
import net.letsdank.jd.ast.stmt.AssignStmt;
import net.letsdank.jd.ast.stmt.BlockStmt;
import net.letsdank.jd.ast.stmt.ExprStmt;
import net.letsdank.jd.ast.stmt.ReturnStmt;
import net.letsdank.jd.bytecode.insn.*;
import net.letsdank.jd.model.ConstantPool;
import net.letsdank.jd.model.attribute.BootstrapMethodsAttribute;
import net.letsdank.jd.model.cp.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Стековый интерпретатор байткода для одного basic block'а.
 * Очень примитивный: только int-операции и возвраты.
 */
public final class ExpressionBuilder {
    private final LocalNameProvider localNames;
    private final ConstantPool cp;
    private final DecompilerOptions options;
    private final BootstrapMethodsAttribute bootstrapMethods;

    public ExpressionBuilder(LocalNameProvider localNames, ConstantPool cp) {
        this(localNames, cp, new DecompilerOptions(), null);
    }

    public ExpressionBuilder(LocalNameProvider localNames, ConstantPool cp, DecompilerOptions options) {
        this(localNames, cp, options, null);
    }

    public ExpressionBuilder(LocalNameProvider localNames, ConstantPool cp,
                             DecompilerOptions options, BootstrapMethodsAttribute bootstrapMethods) {
        this.localNames = localNames;
        this.cp = cp;
        this.options = options;
        this.bootstrapMethods = bootstrapMethods;
    }

    /**
     * Превращает список инструкций в блок операторов.
     * Для условных блоков можно передать список без последнего JumpInsn.
     */
    public BlockStmt buildBlock(List<Insn> insns) {
        BlockStmt block = new BlockStmt();
        Deque<Expr> stack = new ArrayDeque<>();

        for (Insn insn : insns) {
            if (insn instanceof SimpleInsn s) {
                switch (s.opcode()) {
                    // арифметика (int, long, float, double)
                    case IADD, LADD, FADD, DADD -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr("+", left, right));
                    }
                    case ISUB, LSUB, FSUB, DSUB -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr("-", left, right));
                    }
                    case IMUL, LMUL, FMUL, DMUL -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr("*", left, right));
                    }
                    case IDIV, LDIV, FDIV, DDIV -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr("/", left, right));
                    }
                    case IREM, LREM, FREM, DREM -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr("%", left, right));
                    }
                    case INEG, LNEG, FNEG, DNEG -> {
                        Expr value = stack.pop();
                        stack.push(new UnaryExpr("-", value));
                    }

                    // побитовые операции
                    case IAND -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr("&", left, right));
                    }
                    case IOR -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr("|", left, right));
                    }
                    case IXOR -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr("^", left, right));
                    }

                    // сдвиги
                    case ISHL -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr("<<", left, right));
                    }
                    case ISHR -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr(">>", left, right));
                    }
                    case IUSHR -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr(">>>", left, right));
                    }

                    // преобразования типов
                    case I2L, I2F, I2D, L2I, L2F, L2D, F2I, F2L, F2D, D2I, D2L, D2F -> {
                        Expr value = stack.pop();
                        // Пока просто оставляем как есть, т.к. в Java часто неявное преобразование
                        stack.push(value);
                    }
                    case I2B,I2C,I2S -> {
                        // Преобразования в меньшие типы
                        Expr value = stack.pop();
                        stack.push(value);
                    }

                    // сравнения данных, float, double (результат: -1, 0 или 1)
                    case LCMP,FCMPL,FCMPG,DCMPL,DCMPG -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        // Представим как обычное сравнение (хотя технически это не совсем правильно)
                        stack.push(new BinaryExpr("-", left,right));
                    }

                    // возвраты
                    case IRETURN -> {
                        Expr value = stack.pop();
                        block.add(new ReturnStmt(value));
                    }
                    case LRETURN -> {
                        Expr value = stack.pop();
                        block.add(new ReturnStmt(value));
                    }
                    case FRETURN -> {
                        Expr value = stack.pop();
                        block.add(new ReturnStmt(value));
                    }
                    case DRETURN -> {
                        Expr value = stack.pop();
                        block.add(new ReturnStmt(value));
                    }
                    case ARETURN -> {
                        Expr value = stack.pop();
                        block.add(new ReturnStmt(value));
                    }
                    case RETURN -> {
                        block.add(new ReturnStmt(null));
                    }

                    // целочисленные константы iconst_*
                    case ICONST_M1 -> stack.push(new IntConstExpr(-1));
                    case ICONST_0 -> stack.push(new IntConstExpr(0));
                    case ICONST_1 -> stack.push(new IntConstExpr(1));
                    case ICONST_2 -> stack.push(new IntConstExpr(2));
                    case ICONST_3 -> stack.push(new IntConstExpr(3));
                    case ICONST_4 -> stack.push(new IntConstExpr(4));
                    case ICONST_5 -> stack.push(new IntConstExpr(5));

                    // локалки без операнда: iload_0..3
                    case ILOAD_0 -> stack.push(varExpr(0));
                    case ILOAD_1 -> stack.push(varExpr(1));
                    case ILOAD_2 -> stack.push(varExpr(2));
                    case ILOAD_3 -> stack.push(varExpr(3));

                    // lload_0..3
                    case LLOAD_0 -> stack.push(varExpr(0));
                    case LLOAD_1 -> stack.push(varExpr(1));
                    case LLOAD_2 -> stack.push(varExpr(2));
                    case LLOAD_3 -> stack.push(varExpr(3));

                    // fload_0..3
                    case FLOAD_0 -> stack.push(varExpr(0));
                    case FLOAD_1 -> stack.push(varExpr(1));
                    case FLOAD_2 -> stack.push(varExpr(2));
                    case FLOAD_3 -> stack.push(varExpr(3));

                    // dload_0..3
                    case DLOAD_0 -> stack.push(varExpr(0));
                    case DLOAD_1 -> stack.push(varExpr(1));
                    case DLOAD_2 -> stack.push(varExpr(2));
                    case DLOAD_3 -> stack.push(varExpr(3));

                    // --- чтение из массива ---
                    case IALOAD, LALOAD, FALOAD, DALOAD,
                         AALOAD, BALOAD, CALOAD, SALOAD -> {
                        Expr index = stack.pop();
                        Expr array = stack.pop();
                        stack.push(new ArrayAccessExpr(array, index));
                    }

                    // --- запись в массив ---
                    case IASTORE, LASTORE, FASTORE, DASTORE,
                         AASTORE, BASTORE, CASTORE, SASTORE -> {
                        Expr value = stack.pop();
                        Expr index = stack.pop();
                        Expr array = stack.pop();
                        ArrayAccessExpr target = new ArrayAccessExpr(array, index);
                        block.add(new AssignStmt(target, value));
                    }

                    // --- длина массива ---
                    case ARRAYLENGTH -> {
                        Expr array = stack.pop();
                        stack.push(new ArrayLengthExpr(array));
                    }

                    // ссылочные локалки без операнда: aload_0..3
                    case ALOAD_0 -> stack.push(varExpr(0));
                    case ALOAD_1 -> stack.push(varExpr(1));
                    case ALOAD_2 -> stack.push(varExpr(2));
                    case ALOAD_3 -> stack.push(varExpr(3));

                    // istore_0..3: присваивание
                    case ISTORE_0 -> {
                        Expr value = stack.pop();
                        block.add(new AssignStmt(varExpr(0), value));
                    }
                    case ISTORE_1 -> {
                        Expr value = stack.pop();
                        block.add(new AssignStmt(varExpr(1), value));
                    }
                    case ISTORE_2 -> {
                        Expr value = stack.pop();
                        block.add(new AssignStmt(varExpr(2), value));
                    }
                    case ISTORE_3 -> {
                        Expr value = stack.pop();
                        block.add(new AssignStmt(varExpr(3), value));
                    }

                    // lstore_0..3: присваивание
                    case LSTORE_0 -> {
                        Expr value = stack.pop();
                        block.add(new AssignStmt(varExpr(0), value));
                    }
                    case LSTORE_1 -> {
                        Expr value = stack.pop();
                        block.add(new AssignStmt(varExpr(1), value));
                    }
                    case LSTORE_2 -> {
                        Expr value = stack.pop();
                        block.add(new AssignStmt(varExpr(2), value));
                    }
                    case LSTORE_3 -> {
                        Expr value = stack.pop();
                        block.add(new AssignStmt(varExpr(3), value));
                    }

                    // fstore_0..3: присваивание
                    case FSTORE_0 -> {
                        Expr value = stack.pop();
                        block.add(new AssignStmt(varExpr(0), value));
                    }
                    case FSTORE_1 -> {
                        Expr value = stack.pop();
                        block.add(new AssignStmt(varExpr(1), value));
                    }
                    case FSTORE_2 -> {
                        Expr value = stack.pop();
                        block.add(new AssignStmt(varExpr(2), value));
                    }
                    case FSTORE_3 -> {
                        Expr value = stack.pop();
                        block.add(new AssignStmt(varExpr(3), value));
                    }

                    // dstore_0..3: присваивание
                    case DSTORE_0 -> {
                        Expr value = stack.pop();
                        block.add(new AssignStmt(varExpr(0), value));
                    }
                    case DSTORE_1 -> {
                        Expr value = stack.pop();
                        block.add(new AssignStmt(varExpr(1), value));
                    }
                    case DSTORE_2 -> {
                        Expr value = stack.pop();
                        block.add(new AssignStmt(varExpr(2), value));
                    }
                    case DSTORE_3 -> {
                        Expr value = stack.pop();
                        block.add(new AssignStmt(varExpr(3), value));
                    }

                    // операции со стеком
                    case POP -> {
                        // просто выкидываем верхнее выражение
                        if (!stack.isEmpty()) {
                            stack.pop();
                        }
                    }
                    case DUP -> {
                        // дублируем верхушку стека
                        if (!stack.isEmpty()) {
                            Expr value = stack.peek();
                            stack.push(value);
                        }
                    }

                    // монитор
                    case MONITORENTER -> {
                        Expr monitor = stack.pop();
                        // Храним в виде ExprStmt для простоты; позже можно завернуть в SynchronizedStmt
                        block.add(new ExprStmt(new CallExpr(null, null, "monitorenter", List.of(monitor))));
                    }
                    case MONITOREXIT -> {
                        Expr monitor = stack.pop();
                        block.add(new ExprStmt(new CallExpr(null, null, "monitorexit", List.of(monitor))));
                    }

                    default -> {
                        // игнорируем всякие nop, etc (пока что)
                    }
                }
            } else if (insn instanceof IntOperandInsn io) {
                switch (io.opcode()) {
                    // bipush / sipush -> константные int
                    case BIPUSH, SIPUSH -> stack.push(new IntConstExpr(io.operand()));
                    case NEWARRAY -> {
                        Expr size = stack.pop();
                        int typeCode = io.operand();
                        String elemType = mapNewArrayType(typeCode);
                        stack.push(new NewArrayExpr(elemType, size));
                    }
                    default -> {
                        // другие инструкции с immediate пока игнорируем
                    }
                }
            } else if (insn instanceof LocalVarInsn lv) {
                switch (lv.opcode()) {
                    // ILOAD с явным индексом
                    case ILOAD, LLOAD, FLOAD, DLOAD -> {
                        int idx = lv.localIndex();
                        stack.push(varExpr(idx));
                    }
                    case ALOAD -> {
                        int idx = lv.localIndex();
                        stack.push(varExpr(idx)); // this, obj, массив и прочие ссылки
                    }
                    case ISTORE, LSTORE, FSTORE, DSTORE -> {
                        int idx = lv.localIndex();
                        Expr value = stack.pop();
                        block.add(new AssignStmt(varExpr(idx), value));
                    }
                    default -> {
                        // остальное пока игнорируем
                    }
                }
            } else if (insn instanceof JumpInsn) {
                // предположим, что block для ExpressionBuilder не включает
                // финальный JumpInsn (кроме специального случая cond-блока),
                // поэтому тут можно проигнорировать
            } else if (insn instanceof ConstantPoolInsn cpi) {
                // Если нет cp (теоретически), просто игнорируем
                if (cp == null) continue;

                switch (cpi.opcode()) {
                    case LDC -> {
                        // ldc #idx -> константа из constant pool
                        CpInfo entry = cp.entry(cpi.cpIndex());
                        if (entry instanceof CpString s) {
                            String text = cp.getUtf8(s.stringIndex());
                            stack.push(new StringLiteralExpr(text));
                        } else if (entry instanceof CpClass cls) {
                            // ldc <SomeClass> -> Class-литерал
                            String internalName = cp.getClassName(cpi.cpIndex());
                            // Самый простой вариант - представить это как строку,
                            // чтобы не падать и хоть как-то отобразить:
                            stack.push(new StringLiteralExpr(internalName));
                        } else if (entry instanceof CpInteger i) {
                            int value = i.value();
                            stack.push(new IntConstExpr(value));
                        }
                    }
                    case LDC2_W -> {
                        // ldc2_w #idx -> long или double константа
                        CpInfo entry = cp.entry(cpi.cpIndex());
                        if(entry instanceof CpLong l) {
                            // Представим как строку для упрощения (long literals вроде 123L)
                            stack.push(new StringLiteralExpr(String.valueOf(l.value())));
                        } else if (entry instanceof CpDouble d) {
                            // Double literals
                            stack.push(new StringLiteralExpr(String.valueOf(d.value())));
                        }
                    }
                    case GETSTATIC -> {
                        Expr fieldExpr = makeStaticFieldExpr(cpi.cpIndex());
                        if (fieldExpr != null) {
                            stack.push(fieldExpr);
                        }
                    }
                    case INVOKEVIRTUAL -> {
                        // 1. Определяем количество аргументов и void/non-void
                        String descriptor = resolveMethodDescriptor(cpi.cpIndex());
                        String methodName = resolveMethodName(cpi.cpIndex());
                        int argCount = argCountFromDescriptor(descriptor);
                        boolean isVoid = descriptor != null && descriptor.endsWith(")V");

                        // 1.1. Достаем owner из constant pool
                        CpMethodref mr = resolveMethodref(cpi.cpIndex());
                        String ownerInternal = null;
                        if (mr != null) {
                            ownerInternal = cp.getClassName(mr.classIndex()); // например "net/letsdank/jd/fixtures/User"
                        }

                        // 2. Снимаем аргументы (в обратном порядке) и target
                        List<Expr> args = new ArrayList<>(argCount);
                        for (int i = 0; i < argCount; i++) {
                            // Последний снятый аргумент - правый, поэтому добавляем в начало
                            args.add(0, stack.pop());
                        }

                        // снимаем объект (this)
                        Expr target = stack.pop();
                        CallExpr call = new CallExpr(target, ownerInternal, methodName, List.copyOf(args));
                        System.out.println("INVOKEVIRTUAL " + ownerInternal + "." + methodName);

                        if (isVoid) {
                            // println(...) и прочие void -> statement
                            block.add(new ExprStmt(call));
                        } else {
                            // результат используется дальше -> оставим в стеке
                            stack.push(call);
                        }
                    }
                    case INVOKESPECIAL -> {
                        String descriptor = resolveMethodDescriptor(cpi.cpIndex());
                        String methodName = resolveMethodName(cpi.cpIndex());
                        int argCount = argCountFromDescriptor(descriptor);

                        CpMethodref mr = resolveMethodref(cpi.cpIndex());
                        String ownerInternal = null;
                        if (mr != null) {
                            ownerInternal = cp.getClassName(mr.classIndex());
                        }

                        // собираем аргументы (последний снятый -> правый)
                        List<Expr> args = new ArrayList<>(argCount);
                        for (int i = 0; i < argCount; i++) {
                            args.add(0, stack.pop());
                        }

                        // target: либо UninitializedNewExpr (NEW+DUP),
                        // либо this/super/обычный объект
                        Expr target = stack.pop();

                        if ("<init>".equals(methodName)) {
                            // --- КОНСТРУКТОР ---

                            if (target instanceof UninitializedNewExpr une &&
                                    ownerInternal != null &&
                                    ownerInternal.equals(une.internalName())) {

                                // Паттерн "NEW X; DUP; ...; INVOKESPECIAL X.<init>"
                                // После DUP на стеке две копии UninitializedNewExpr.
                                // Мы только что сняли верхнюю; нижняя еще лежит - уберем ее.
                                if (!stack.isEmpty() &&
                                        stack.peek() instanceof UninitializedNewExpr une2 &&
                                        une2.internalName().equals(une.internalName())) {
                                    stack.pop();
                                }

                                String ownerSimple = simpleClassName(ownerInternal);
                                NewExpr newExpr = new NewExpr(ownerSimple, List.copyOf(args));
                                // Результат new X(...) остается на стеке
                                stack.push(newExpr);
                            } else {
                                // this(...) или super(...) в конструкторе:
                                // представим как обычный вызов и запишем как statement
                                CallExpr call = new CallExpr(target, ownerInternal, "<init>", List.copyOf(args));
                                block.add(new ExprStmt(call));
                            }
                        } else {
                            // Обычный INVOKESPECIAL (private, super.method())
                            CallExpr call = new CallExpr(target, ownerInternal, methodName, List.copyOf(args));
                            boolean isVoid = descriptor != null && descriptor.endsWith(")V");
                            if (isVoid) {
                                block.add(new ExprStmt(call));
                            } else {
                                stack.push(call);
                            }
                        }
                    }
                    case INVOKESTATIC -> {
                        String descriptor = resolveMethodDescriptor(cpi.cpIndex());
                        String methodName = resolveMethodName(cpi.cpIndex());
                        int argCount = argCountFromDescriptor(descriptor);

                        // Спец-кейс: kotlin.jvm.internal.Intrinsics.checkNotNullParameter(...)
                        CpMethodref mr = resolveMethodref(cpi.cpIndex());
                        String ownerInternal = cp.getClassName(mr.classIndex());

                        // 1. Kotlin Intrinsics
                        if (options != null &&
                                options.hideKotlinIntrinsics() &&
                                "kotlin/jvm/internal/Intrinsics".equals(ownerInternal) &&
                                "checkNotNullParameter".equals(methodName)) {
                            // Просто снимаем аргументы со стека и не добавляем statement
                            for (int i = 0; i < argCount; i++) {
                                stack.pop();
                            }
                            break;
                        }

                        // 2. Любые статические методы, имя которых начинается на '$'
                        if (options != null &&
                                options.hideDollarMethods() &&
                                methodName != null &&
                                !methodName.isEmpty() &&
                                methodName.charAt(0) == '$') {
                            // Снимаем аргументы со стека, но не создаем ExprStmt/CallExpr.
                            for (int i = 0; i < argCount; i++) {
                                stack.pop();
                            }
                            break;
                        }

                        boolean isVoid = descriptor != null && descriptor.endsWith(")V");

                        // args
                        List<Expr> args = new ArrayList<>(argCount);
                        for (int i = 0; i < argCount; i++) {
                            args.add(0, stack.pop());
                        }

                        String ownerSimple = simpleClassName(ownerInternal);

                        // Представим статический вызов как expr "Owner.method(args...)"
                        Expr target = new VarExpr(ownerSimple);

                        CallExpr call = new CallExpr(target, ownerInternal, methodName, List.copyOf(args));
                        if (isVoid) {
                            block.add(new ExprStmt(call));
                        } else {
                            stack.push(call);
                        }
                    }
                    case INVOKEDYNAMIC -> {
                        // TODO: почему это было вынесено отдельно?
                        //  может остальные опкоды в ConstantPool тоже стоит вынести?
                        handleInvokeDynamic(stack, block, cpi);
                    }
                    case GETFIELD -> {
                        CpFieldref fr = (CpFieldref) cp.entry(cpi.cpIndex());
                        CpClass owner = (CpClass) cp.entry(fr.classIndex());
                        CpNameAndType nt = (CpNameAndType) cp.entry(fr.nameAndTypeIndex());

                        String fieldName = cp.getUtf8(nt.nameIndex());

                        Expr obj = stack.pop();
                        stack.push(new FieldAccessExpr(obj, fieldName));
                    }
                    case PUTFIELD -> {
                        CpFieldref fr = (CpFieldref) cp.entry(cpi.cpIndex());
                        CpNameAndType nt = (CpNameAndType) cp.entry(fr.nameAndTypeIndex());
                        String fieldName = cp.getUtf8(nt.nameIndex());

                        Expr value = stack.pop();
                        Expr obj = stack.pop();

                        Expr lhs = new FieldAccessExpr(obj, fieldName);
                        block.add(new AssignStmt(lhs, value));
                    }
                    case NEW -> {
                        // NEW #idx -> CONSTANT_Class
                        CpInfo entry = cp.entry(cpi.cpIndex());
                        if (entry instanceof CpClass cls) {
                            String ownerInternal = cp.getClassName(cpi.cpIndex());
                            // Кладем маркер "неинициализированный объект"
                            stack.push(new UninitializedNewExpr(ownerInternal));
                        }
                    }
                    case CHECKCAST -> {
                        // CHECKCAST <cp_index:u2> (CONSTANT_Class)
                        String internalName = cp.getClassName(cpi.cpIndex());
                        String typeName = internalName.replace('/', '.');
                        Expr value = stack.pop();
                        stack.push(new CastExpr(typeName, value));
                    }
                    case INSTANCEOF -> {
                        // INSTANCEOF <cp_index:u2> (CONSTANT_Class)
                        String internalName = cp.getClassName(cpi.cpIndex());
                        String typeName = internalName.replace('/', '.');
                        Expr value = stack.pop();
                        stack.push(new InstanceOfExpr(value, typeName));
                    }
                    case ANEWARRAY -> {
                        // stack: ..., size
                        Expr size = stack.pop();
                        String internalName = cp.getClassName(cpi.cpIndex());
                        String typeName = internalName.replace('/', '.');
                        stack.push(new NewArrayExpr(typeName, size));
                    }
                    default -> {
                        // остальные инструкции с cp пока игнорируем
                    }
                }
            } else if (insn instanceof IincInsn inc) {
                int idx = inc.localIndex();
                int delta = inc.delta();
                // v = v + delta;
                VarExpr v = varExpr(idx);
                Expr rhs;
                if (delta == 1) {
                    // пока вместо v++ выведем v += 1
                    rhs = new BinaryExpr("+", v, new IntConstExpr(1));
                } else {
                    rhs = new BinaryExpr("+", v, new IntConstExpr(delta));
                }
                block.add(new AssignStmt(v, rhs));
            } else if (insn instanceof UnknownInsn) {
                // ничего
            }
        }

        return block;
    }

    private void handleInvokeDynamic(Deque<Expr> stack, BlockStmt block, ConstantPoolInsn cpi) {
        CpInfo info = cp.entry(cpi.cpIndex());
        if (!(info instanceof CpInvokeDynamic indy)) {
            // На всякий случай - ничего умного сделать не можем
            return;
        }

        // Дескриптор вызова (аргументы + возвращаемый тип)
        CpNameAndType nt = (CpNameAndType) cp.entry(indy.nameAndTypeIndex());
        String desc = cp.getUtf8(nt.descriptorIndex());
        int argCount = argCountFromDescriptor(desc);

        // Снимаем аргументы (в обратном порядке, как обычно)
        List<Expr> args = new ArrayList<>(argCount);
        for (int i = 0; i < argCount; i++) {
            args.add(0, stack.pop());
        }

        // Если нет BootstrapMethods - просто представим это как обычный вызов
        if (bootstrapMethods == null || bootstrapMethods.methods().length == 0) {
            stack.push(makeGenericInvokeDynamicCall(nt, args));
            return;
        }

        int bmIndex = indy.bootstrapMethodAttrIndex();
        // bootstrap_method_attr_index - индекс в массиве bootstrapMethods
        if (bmIndex < 0 || bmIndex >= bootstrapMethods.methods().length) {
            stack.push(makeGenericInvokeDynamicCall(nt, args));
            return;
        }

        BootstrapMethodsAttribute.BootstrapMethod bm = bootstrapMethods.methods()[bmIndex];

        // Разбираем bootstrap method handle
        CpMethodHandle mh = (CpMethodHandle) cp.entry(bm.bootstrapMethodRef());
        CpMethodref targetRef = (CpMethodref) cp.entry(mh.referenceIndex());
        String ownerInternal = cp.getClassName(targetRef.classIndex());
        String owner = ownerInternal.replace('/', '.');

        CpNameAndType targetNt = (CpNameAndType) cp.entry(targetRef.nameAndTypeIndex());
        String targetName = cp.getUtf8(targetNt.nameIndex());

        boolean isStringConcatFactory =
                "java.lang.invoke.StringConcatFactory".equals(owner) &&
                        ("makeConcatWithConstants".equals(targetName) || "makeConcat".equals(targetName));

        if (isStringConcatFactory) {
            Expr concat = buildStringConcatFromIndy(args, bm);
            if (concat != null) {
                stack.push(concat);
                return;
            }
            // если не удалось - упадем в generic
        }

        // Fallback: псевдо-вызов, чтобы не ломать стек
        stack.push(makeGenericInvokeDynamicCall(nt, args));
    }

    private Expr makeGenericInvokeDynamicCall(CpNameAndType nt, List<Expr> args) {
        String name = cp.getUtf8(nt.nameIndex());
        // Представим как обычный "глобальный" вызов: name(arg1, arg2)
        return new CallExpr(null, null, name, List.copyOf(args));
    }

    private String mapNewArrayType(int typeCode) {
        // JVM spec:
        // 4=boolean
        // 5=char
        // 6=float
        // 7=double
        // 8=byte
        // 9=short
        // 10=int
        // 11=long
        return switch (typeCode) {
            case 4 -> "boolean";
            case 5 -> "char";
            case 6 -> "float";
            case 7 -> "double";
            case 8 -> "byte";
            case 9 -> "short";
            case 10 -> "int";
            case 11 -> "long";
            default -> "/*unknown*/int";
        };
    }

    private VarExpr varExpr(int index) {
        return new VarExpr(localNames.nameForLocal(index));
    }

    /**
     * Вспомогательный метод: симуляция стека до JumpInsn, чтобы вытащить
     * операнды условия. Нужен для if/if_icmp и прочих IFxx.
     */
    public Deque<Expr> simulateStackBeforeBranch(List<Insn> insns) {
        Deque<Expr> stack = new ArrayDeque<>();

        for (Insn insn : insns) {
            if (insn instanceof JumpInsn) {
                break; // не обрабатываем сам Jump
            }

            if (insn instanceof SimpleInsn s) {
                switch (s.opcode()) {
                    // арифметика
                    case IADD -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr("+", left, right));
                    }
                    case ISUB -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr("-", left, right));
                    }
                    case IMUL -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr("*", left, right));
                    }
                    case IDIV -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr("/", left, right));
                    }
                    case IREM -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr("%", left, right));
                    }
                    case INEG -> {
                        Expr v = stack.pop();
                        stack.push(new UnaryExpr("-", v));
                    }

                    // побитовые операции
                    case IAND -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr("&", left, right));
                    }
                    case IOR -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr("|", left, right));
                    }
                    case IXOR -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr("^", left, right));
                    }

                    // сдвиги
                    case ISHL -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr("<<", left, right));
                    }
                    case ISHR -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr(">>", left, right));
                    }
                    case IUSHR -> {
                        Expr right = stack.pop();
                        Expr left = stack.pop();
                        stack.push(new BinaryExpr(">>>", left, right));
                    }

                    // iconst_*
                    case ICONST_M1 -> stack.push(new IntConstExpr(-1));
                    case ICONST_0 -> stack.push(new IntConstExpr(0));
                    case ICONST_1 -> stack.push(new IntConstExpr(1));
                    case ICONST_2 -> stack.push(new IntConstExpr(2));
                    case ICONST_3 -> stack.push(new IntConstExpr(3));
                    case ICONST_4 -> stack.push(new IntConstExpr(4));
                    case ICONST_5 -> stack.push(new IntConstExpr(5));

                    // iload_0..3
                    case ILOAD_0 -> stack.push(varExpr(0));
                    case ILOAD_1 -> stack.push(varExpr(1));
                    case ILOAD_2 -> stack.push(varExpr(2));
                    case ILOAD_3 -> stack.push(varExpr(3));

                    // aload_0..3
                    case ALOAD_0 -> stack.push(varExpr(0));
                    case ALOAD_1 -> stack.push(varExpr(1));
                    case ALOAD_2 -> stack.push(varExpr(2));
                    case ALOAD_3 -> stack.push(varExpr(3));

                    // длина массива
                    case ARRAYLENGTH -> {
                        Expr array = stack.pop();
                        stack.push(new ArrayLengthExpr(array));
                    }

                    // стековые операции
                    case POP -> {
                        if (!stack.isEmpty()) {
                            stack.pop();
                        }
                    }
                    case DUP -> {
                        if (!stack.isEmpty()) {
                            stack.push(stack.peek());
                        }
                    }

                    default -> {
                        // остальные SimpleInsn для целей условия пока игнорируем
                    }
                }
            } else if (insn instanceof LocalVarInsn lv) {
                switch (lv.opcode()) {
                    case ILOAD -> {
                        int idx = lv.localIndex();
                        stack.push(varExpr(idx));
                    }
                    case ALOAD -> {
                        int idx = lv.localIndex();
                        stack.push(varExpr(idx));
                    }
                    default -> {
                    }
                }
            } else if (insn instanceof IntOperandInsn io) {
                switch (io.opcode()) {
                    case BIPUSH, SIPUSH -> stack.push(new IntConstExpr(io.operand()));
                    default -> {
                    }
                }
            } else if (insn instanceof ConstantPoolInsn cpi) {
                // Иногда условие завязано на ldc-константах
                switch (cpi.opcode()) {
                    case LDC -> {
                        CpInfo entry = cp.entry(cpi.cpIndex());
                        if (entry instanceof CpInteger i) {
                            stack.push(new IntConstExpr(i.value()));
                        } else if (entry instanceof CpString s) {
                            String text = cp.getUtf8(s.stringIndex());
                            stack.push(new StringLiteralExpr(text));
                        }
                        // остальные типы для условий пока можно игнорировать
                    }
                    default -> {
                    }
                }
            }
            // IINC и прочее на стек не влияют напрямую - пропускаем
        }

        return stack;
    }

    private Expr makeStaticFieldExpr(int cpIndex) {
        if (cp == null) return null;
        CpInfo e = cp.entry(cpIndex);
        if (!(e instanceof CpFieldref fr)) {
            return null;
        }

        // owner internal name: java/lang/System
        String ownerInternal = cp.getClassName(fr.classIndex());
        String ownerSimple = simpleClassName(ownerInternal);

        CpNameAndType nt = (CpNameAndType) cp.entry(fr.nameAndTypeIndex());
        String fieldName = cp.getUtf8(nt.nameIndex());

        // System.out
        return new FieldAccessExpr(new VarExpr(ownerSimple), fieldName);
    }

    private String simpleClassName(String internalName) {
        // internalName: java/lang/System
        int slash = internalName.lastIndexOf('/');
        if (slash >= 0 && slash < internalName.length() - 1) {
            return internalName.substring(slash + 1);
        }
        return internalName.replace('/', '.');
    }

    // Возвращает Methodref / InterfaceMethodref из cp

    private CpMethodref resolveMethodref(int cpIndex) {
        CpInfo info = cp.entry(cpIndex);
        if (info instanceof CpMethodref mr) {
            return mr;
        }
        if (info instanceof CpInterfaceMethodref imr) {
            // если есть интерфейсные вызовы
            // TODO: Methodref создает ClassFileReader, который прокидывает динамический tag
            //  поэтому пропишем константой 10, чтобы компилятор не ругался
            return new CpMethodref(10, imr.classIndex(), imr.nameAndTypeIndex());
        }
        return null;
    }

    private String resolveMethodDescriptor(int cpIndex) {
        CpMethodref mr = resolveMethodref(cpIndex);
        if (mr == null) return null;
        CpNameAndType nt = (CpNameAndType) cp.entry(mr.nameAndTypeIndex());
        return cp.getUtf8(nt.descriptorIndex());
    }

    private String resolveMethodName(int cpIndex) {
        CpMethodref mr = resolveMethodref(cpIndex);
        if (mr == null) return null;
        CpNameAndType nt = (CpNameAndType) cp.entry(mr.nameAndTypeIndex());
        return cp.getUtf8(nt.nameIndex());
    }

    private Expr buildStringConcatFromIndy(List<Expr> args, BootstrapMethodsAttribute.BootstrapMethod bm) {
        // По спецификации: первый bootstrap-аргумент для makeConcatWithConstants - recipe-строка
        if (bm.bootstrapArguments().length == 0) {
            // Нет recipe - просто склеим аргумент через '+'.
            return buildPlusChain(args);
        }

        int recipeIndex = bm.bootstrapArguments()[0];
        CpInfo recipeEntry = cp.entry(recipeIndex);

        if (!(recipeEntry instanceof CpString s)) {
            return buildPlusChain(args);
        }

        String recipe = cp.getUtf8(s.stringIndex());
        if (recipe == null) {
            return buildPlusChain(args);
        }

        // В recipe используется '\u0001' как placeholder для аргументов
        String[] parts = recipe.split("\u0001", -1);

        Expr current = null;

        // Префикс до первого аргумента
        if (parts.length > 0 && !parts[0].isEmpty()) {
            current = new StringLiteralExpr(parts[0]);
        }

        int dynamicCount = args.size();
        for (int i = 0; i < dynamicCount; i++) {
            Expr arg = args.get(i);

            if (current == null) {
                current = arg;
            } else {
                current = new BinaryExpr("+", current, arg);
            }

            // Следующий статический кусок после аргумента
            if (i + 1 < parts.length && !parts[i + 1].isEmpty()) {
                Expr staticPart = new StringLiteralExpr(parts[i + 1]);
                current = new BinaryExpr("+", current, staticPart);
            }
        }

        // На всякий случай: если recipe странный и current так и остался null
        if (current == null) {
            return buildPlusChain(args);
        }

        return current;
    }

    private Expr buildPlusChain(List<Expr> args) {
        if (args.isEmpty()) return new StringLiteralExpr("");
        Expr current = args.getFirst();
        for (int i = 1; i < args.size(); i++) {
            current = new BinaryExpr("+", current, args.get(i));
        }
        return current;
    }

    // Определяем количество аргументов по дескриптору, типа (II)I -> 2.
    // Очень простой парсер: только примитивы и ссылочные типы.
    private int argCountFromDescriptor(String descriptor) {
        if (descriptor == null || !descriptor.startsWith("(")) return 0;
        int i = 1;
        int count = 0;
        while (i < descriptor.length()) {
            char c = descriptor.charAt(i);
            if (c == ')') break;

            if (c == 'L') {
                // объектный тип Ljava/lang/String;
                int semi = descriptor.indexOf(';', i);
                if (semi < 0) break;
                i = semi + 1;
                count++;
            } else if (c == '[') {
                // массивы игнорируем, но считаем как часть типа
                i++;
            } else {
                // примитивный тип I/J/F/D/Z/B/C/S
                count++;
                i++;
            }
        }
        return count;
    }
}
