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
    LDC(0x12,"ldc",OperandType.CONSTPOOL_U1),

    IADD(0x60, "iadd"),
    INEG(0x74, "ineg"),

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

    // --- поля/методы (cp_index u2) ---
    GETSTATIC(0xB2,"getstatic",OperandType.CONSTPOOL_U2),
    PUTSTATIC(0xB3,"putstatic",OperandType.CONSTPOOL_U2),
    GETFIELD(0xB4,"getfield",OperandType.CONSTPOOL_U2),
    PUTFIELD(0xB5,"putfield",OperandType.CONSTPOOL_U2),

    INVOKEVIRTUAL(0xB6,"invokevirtual",OperandType.CONSTPOOL_U2),
    INVOKESPECIAL(0xB7,"invokespecial",OperandType.CONSTPOOL_U2),
    INVOKESTATIC(0xB8,"invokestatic",OperandType.CONSTPOOL_U2),

    // --- возвраты ---
    IRETURN(0xAC, "ireturn"),
    RETURN(0xB1, "return");

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
