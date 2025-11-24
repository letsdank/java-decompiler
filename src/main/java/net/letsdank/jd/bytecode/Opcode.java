package net.letsdank.jd.bytecode;

/**
 * Набор известных опкодов. Пока только те, что нужны для
 * простых методов из тестов. Остальные будем считать неизвестными.
 */
public enum Opcode {
    // --- константы / локалки ---
    ILOAD(0x15, "iload", OperandType.LOCAL_INDEX_U1),
    ILOAD_0(0x1A, "iload_0"),
    ILOAD_1(0x1B, "iload_1"),
    ILOAD_2(0x1C, "iload_2"),
    ILOAD_3(0x1D, "iload_3"),

    LLOAD(0x16, "iload", OperandType.LOCAL_INDEX_U1),
    FLOAD(0x17, "fload", OperandType.LOCAL_INDEX_U1),
    DLOAD(0x18, "dload", OperandType.LOCAL_INDEX_U1),

    LLOAD_0(0x1E, "lload_0"),
    LLOAD_1(0x1F, "lload_1"),
    LLOAD_2(0x20, "lload_2"),
    LLOAD_3(0x21, "lload_3"),

    FLOAD_0(0x22, "fload_0"),
    FLOAD_1(0x23, "fload_1"),
    FLOAD_2(0x24, "fload_2"),
    FLOAD_3(0x25, "fload_3"),

    DLOAD_0(0x26, "dload_0"),
    DLOAD_1(0x27, "dload_1"),
    DLOAD_2(0x28, "dload_2"),
    DLOAD_3(0x29, "dload_3"),

    // --- array load ---
    IALOAD(0x2E, "iaload"),
    LALOAD(0x2F, "laload"),
    FALOAD(0x30, "faload"),
    DALOAD(0x31, "daload"),
    AALOAD(0x32, "aaload"),
    BALOAD(0x33, "baload"),
    CALOAD(0x34, "caload"),
    SALOAD(0x35, "saload"),

    // --- array store ---
    IASTORE(0x4F, "iastore"),
    LASTORE(0x50, "lastore"),
    FASTORE(0x51, "fastore"),
    DASTORE(0x52, "dastore"),
    AASTORE(0x53, "aastore"),
    BASTORE(0x54, "bastore"),
    CASTORE(0x55, "castore"),
    SASTORE(0x56, "aastore"),

    ALOAD(0x19, "aload", OperandType.LOCAL_INDEX_U1),
    ALOAD_0(0x2A, "aload_0"),
    ALOAD_1(0x2B, "aload_1"),
    ALOAD_2(0x2C, "aload_2"),
    ALOAD_3(0x2D, "aload_3"),

    ISTORE(0x36, "istore", OperandType.LOCAL_INDEX_U1),
    ISTORE_0(0x3B, "istore_0"),
    ISTORE_1(0x3C, "istore_1"),
    ISTORE_2(0x3D, "istore_2"),
    ISTORE_3(0x3E, "istore_3"),

    LSTORE(0x37, "lstore", OperandType.LOCAL_INDEX_U1),
    FSTORE(0x38, "fstore", OperandType.LOCAL_INDEX_U1),
    DSTORE(0x39, "dstore", OperandType.LOCAL_INDEX_U1),

    LSTORE_0(0x3F, "lstore_0"),
    LSTORE_1(0x40, "lstore_1"),
    LSTORE_2(0x41, "lstore_2"),
    LSTORE_3(0x42, "lstore_3"),

    FSTORE_0(0x43, "fstore_0"),
    FSTORE_1(0x44, "fstore_1"),
    FSTORE_2(0x45, "fstore_2"),
    FSTORE_3(0x46, "fstore_3"),

    DSTORE_0(0x47, "dstore_0"),
    DSTORE_1(0x48, "dstore_1"),
    DSTORE_2(0x49, "dstore_2"),
    DSTORE_3(0x4A, "dstore_3"),

    ICONST_M1(0x02, "iconst_m1"),
    ICONST_0(0x03, "iconst_0"),
    ICONST_1(0x04, "iconst_1"),
    ICONST_2(0x05, "iconst_2"),
    ICONST_3(0x06, "iconst_3"),
    ICONST_4(0x07, "iconst_4"),
    ICONST_5(0x08, "iconst_5"),

    BIPUSH(0x10, "bipush", OperandType.BYTE_IMM),
    SIPUSH(0x11, "sipush", OperandType.SHORT_IMM),

    // ldc index (u1)
    LDC(0x12, "ldc", OperandType.CONSTPOOL_U1),

    // --- стек ---
    POP(0x57, "pop"),
    DUP(0x59, "dup"),

    // --- арифметика ---
    IADD(0x60, "iadd"),
    ISUB(0x64, "isub"),
    IMUL(0x68, "imul"),
    IDIV(0x6C, "idiv"),
    IREM(0x70, "irem"),
    INEG(0x74, "ineg"),

    // --- побитовые операции / сдвиги ---
    ISHL(0x78, "ishl"),
    ISHR(0x7A, "ishr"),
    IUSHR(0x7C, "iushr"),
    IAND(0x7E, "iand"),
    IOR(0x80, "ior"),
    IXOR(0x82, "ixor"),

    IINC(0x84, "iinc", OperandType.IINC),

    // --- условные переходы ---
    IFEQ(0x99, "ifeq", OperandType.BRANCH_S2),
    IFNE(0x9A, "ifne", OperandType.BRANCH_S2),
    IFLT(0x9B, "iflt", OperandType.BRANCH_S2),
    IFGE(0x9C, "ifge", OperandType.BRANCH_S2),
    IFGT(0x9D, "ifgt", OperandType.BRANCH_S2),
    IFLE(0x9E, "ifle", OperandType.BRANCH_S2),

    IF_ICMPEQ(0x9F, "if_icmpeq", OperandType.BRANCH_S2),
    IF_ICMPNE(0xA0, "ic_icmpne", OperandType.BRANCH_S2),
    IF_ICMPLT(0xA1, "if_icmplt", OperandType.BRANCH_S2),
    IF_ICMPGE(0xA2, "if_icmpge", OperandType.BRANCH_S2),
    IF_ICMPGT(0xA3, "if_icmpgt", OperandType.BRANCH_S2),
    IF_ICMPLE(0xA4, "if_icmple", OperandType.BRANCH_S2),

    // --- безусловные ---
    GOTO(0xA7, "goto", OperandType.BRANCH_S2),

    // --- возвраты ---
    IRETURN(0xAC, "ireturn"),
    LRETURN(0xAD, "lreturn"),
    FRETURN(0xAE, "freturn"),
    DRETURN(0xAF, "dreturn"),
    ARETURN(0xB0, "areturn"),
    RETURN(0xB1, "return"),

    // --- поля/методы (cp_index u2) ---,
    GETSTATIC(0xB2, "getstatic", OperandType.CONSTPOOL_U2),
    PUTSTATIC(0xB3, "putstatic", OperandType.CONSTPOOL_U2),
    GETFIELD(0xB4, "getfield", OperandType.CONSTPOOL_U2),
    PUTFIELD(0xB5, "putfield", OperandType.CONSTPOOL_U2),

    // --- вызовы ---
    INVOKEVIRTUAL(0xB6, "invokevirtual", OperandType.CONSTPOOL_U2),
    INVOKESPECIAL(0xB7, "invokespecial", OperandType.CONSTPOOL_U2),
    INVOKESTATIC(0xB8, "invokestatic", OperandType.CONSTPOOL_U2),
    INVOKEINTERFACE(0xB9, "invokeinterface", OperandType.CONSTPOOL_U2),
    INVOKEDYNAMIC(0xBA, "invokedynamic", OperandType.INVOKEDYNAMIC),

    // --- создание объектов ---
    NEW(0xBB, "new", OperandType.CONSTPOOL_U2),

    // массивы
    NEWARRAY(0xBC, "newarray", OperandType.BYTE_IMM), // typecode:u1
    ANEWARRAY(0xBD, "anewarray", OperandType.CONSTPOOL_U2),
    ARRAYLENGTH(0xBE, "arraylength"),

    // --- проверки типов ---
    CHECKCAST(0xC0, "checkcast", OperandType.CONSTPOOL_U2),
    INSTANCEOF(0xC1, "instanceof", OperandType.CONSTPOOL_U2),
    ;

    private final int code;
    private final String mnemonic;
    private final OperandType operandType;

    Opcode(int code, String mnemonic) {
        this(code, mnemonic, OperandType.NONE);
    }

    Opcode(int code, String mnemonic, OperandType operandType) {
        this.code = code;
        this.mnemonic = mnemonic;
        this.operandType = operandType;
    }

    public int code() {
        return code;
    }

    public String mnemonic() {
        return mnemonic;
    }

    public OperandType operandType() {
        return operandType;
    }

    public static Opcode fromCode(int code) {
        for (Opcode op : values()) {
            if (op.code == code) return op;
        }
        return null;
    }
}
