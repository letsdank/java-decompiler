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
    ARETURN(0xB0, "areturn"),
    IRETURN(0xAC, "ireturn"),
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
