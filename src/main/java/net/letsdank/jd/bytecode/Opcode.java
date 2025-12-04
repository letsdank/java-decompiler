package net.letsdank.jd.bytecode;

/**
 * Полный набор опкодов JVM (до Java 8, включая устаревшие jsr/ret).
 * Для каждого опкода указано:
 * - числовое значение (byte & 0xFF)
 * - мнемоника
 * - тип операндов (OperandType), если есть
 */
public enum Opcode {
    //
    // 0x00..0x0F - константы, no-op
    //

    NOP(0x00, "nop"),
    ACONST_NULL(0x01, "aconst_null"),

    ICONST_M1(0x02, "iconst_m1"),
    ICONST_0(0x03, "iconst_0"),
    ICONST_1(0x04, "iconst_1"),
    ICONST_2(0x05, "iconst_2"),
    ICONST_3(0x06, "iconst_3"),
    ICONST_4(0x07, "iconst_4"),
    ICONST_5(0x08, "iconst_5"),

    LCONST_0(0x09, "lconst_0"),
    LCONST_1(0x0A, "lconst_1"),

    FCONST_0(0x0B, "fconst_0"),
    FCONST_1(0x0C, "fconst_1"),
    FCONST_2(0x0D, "fconst_2"),

    DCONST_0(0x0E, "dconst_0"),
    DCONST_1(0x0F, "dconst_1"),

    //
    // 0x10..0x14 - push / ldc
    //

    BIPUSH(0x10, "bipush", OperandType.BYTE_IMM),
    SIPUSH(0x11, "sipush", OperandType.SHORT_IMM),

    LDC(0x12, "ldc", OperandType.CONSTPOOL_U1),
    LDC_W(0x13, "ldc_w", OperandType.CONSTPOOL_U2),
    LDC2_W(0x14, "ldc2_w", OperandType.CONSTPOOL_U2),

    //
    // 0x15..0x19 - xload index
    //

    ILOAD(0x15, "iload", OperandType.LOCAL_INDEX_U1),
    LLOAD(0x16, "lload", OperandType.LOCAL_INDEX_U1),
    FLOAD(0x17, "fload", OperandType.LOCAL_INDEX_U1),
    DLOAD(0x18, "dload", OperandType.LOCAL_INDEX_U1),
    ALOAD(0x19, "aload", OperandType.LOCAL_INDEX_U1),

    //
    // 0x1A..0x2D - xload_<n>
    //

    ILOAD_0(0x1A, "iload_0"),
    ILOAD_1(0x1B, "iload_1"),
    ILOAD_2(0x1C, "iload_2"),
    ILOAD_3(0x1D, "iload_3"),

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

    ALOAD_0(0x2A, "aload_0"),
    ALOAD_1(0x2B, "aload_1"),
    ALOAD_2(0x2C, "aload_2"),
    ALOAD_3(0x2D, "aload_3"),

    //
    // 0x2E..0x35 - array load
    //

    IALOAD(0x2E, "iaload"),
    LALOAD(0x2F, "laload"),
    FALOAD(0x30, "faload"),
    DALOAD(0x31, "daload"),
    AALOAD(0x32, "aaload"),
    BALOAD(0x33, "baload"),
    CALOAD(0x34, "caload"),
    SALOAD(0x35, "saload"),

    //
    // 0x36..0x3A - xstore index
    //

    ISTORE(0x36, "istore", OperandType.LOCAL_INDEX_U1),
    LSTORE(0x37, "lstore", OperandType.LOCAL_INDEX_U1),
    FSTORE(0x38, "fstore", OperandType.LOCAL_INDEX_U1),
    DSTORE(0x39, "dstore", OperandType.LOCAL_INDEX_U1),
    ASTORE(0x3A, "astore", OperandType.LOCAL_INDEX_U1),

    //
    // 0x3B..0x4E - xstore_<n>
    //

    ISTORE_0(0x3B, "istore_0"),
    ISTORE_1(0x3C, "istore_1"),
    ISTORE_2(0x3D, "istore_2"),
    ISTORE_3(0x3E, "istore_3"),

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

    ASTORE_0(0x4B, "astore_0"),
    ASTORE_1(0x4C, "astore_1"),
    ASTORE_2(0x4D, "astore_2"),
    ASTORE_3(0x4E, "astore_3"),

    //
    // 0x4F..0x56 - array store
    //

    IASTORE(0x4F, "iastore"),
    LASTORE(0x50, "lastore"),
    FASTORE(0x51, "fastore"),
    DASTORE(0x52, "dastore"),
    AASTORE(0x53, "aastore"),
    BASTORE(0x54, "bastore"),
    CASTORE(0x55, "castore"),
    SASTORE(0x56, "sastore"),

    //
    // 0x57..0x5F - стековые операции
    //

    POP(0x57, "pop"),
    POP2(0x58, "pop2"),
    DUP(0x59, "dup"),
    DUP_X1(0x5A, "dup_x1"),
    DUP_X2(0x5B, "dup_x2"),
    DUP2(0x5C, "dup2"),
    DUP2_X1(0x5D, "dup2_x1"),
    DUP2_X2(0x5E, "dup2_x2"),
    SWAP(0x5F, "swap"),

    //
    // 0x60..0x77 - арифметика
    //

    IADD(0x60, "iadd"),
    LADD(0x61, "ladd"),
    FADD(0x62, "fadd"),
    DADD(0x63, "dadd"),

    ISUB(0x64, "isub"),
    LSUB(0x65, "lsub"),
    FSUB(0x66, "fsub"),
    DSUB(0x67, "dsub"),

    IMUL(0x68, "imul"),
    LMUL(0x69, "lmul"),
    FMUL(0x6A, "fmul"),
    DMUL(0x6B, "dmul"),

    IDIV(0x6C, "idiv"),
    LDIV(0x6D, "ldiv"),
    FDIV(0x6E, "fdiv"),
    DDIV(0x6F, "ddiv"),

    IREM(0x70, "irem"),
    LREM(0x71, "lrem"),
    FREM(0x72, "frem"),
    DREM(0x73, "drem"),

    INEG(0x74, "ineg"),
    LNEG(0x75, "lneg"),
    FNEG(0x76, "fneg"),
    DNEG(0x77, "dneg"),

    //
    // 0x78..0x83 - побитовые операции / сдвиги
    //

    ISHL(0x78, "ishl"),
    LSHL(0x79, "lshl"),
    ISHR(0x7A, "ishr"),
    LSHR(0x7B, "lshr"),
    IUSHR(0x7C, "iushr"),
    LUSHR(0x7D, "lushr"),

    IAND(0x7E, "iand"),
    LAND(0x7F, "land"),

    IOR(0x80, "ior"),
    LOR(0x71, "lor"),

    IXOR(0x82, "ixor"),
    LXOR(0x83, "lxor"),

    IINC(0x84, "iinc", OperandType.IINC),

    //
    // 0x85..0x93 - преобразования типов
    //

    I2L(0x85, "i2l"),
    I2F(0x86, "i2f"),
    I2D(0x87, "i2d"),
    L2I(0x88, "l2i"),
    L2F(0x89, "l2f"),
    L2D(0x8A, "l2d"),
    F2I(0x8B, "f2i"),
    F2L(0x8C, "f2l"),
    F2D(0x8D, "f2d"),
    D2I(0x8E, "d2i"),
    D2L(0x8F, "d2l"),
    D2F(0x90, "d2f"),
    I2B(0x91, "i2b"),
    I2C(0x92, "i2c"),
    I2S(0x93, "i2s"),

    //
    // 0x94..0x98 - сравнения
    //

    LCMP(0x94, "lcmp"),
    FCMPL(0x95, "fcmpl"),
    FCMPG(0x96, "fcmpg"),
    DCMPL(0x97, "dcmpl"),
    DCMPG(0x98, "dcmpg"),

    //
    // 0x99..0xA6 - условные переходы (branch s2)
    //

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

    IF_ACMPEQ(0xA5, "if_acmpeq", OperandType.BRANCH_S2),
    IF_ACMPNE(0xA6, "if_acmpne", OperandType.BRANCH_S2),

    //
    // 0xA7..0xA9, 0xC8..0xC9 - goto/jsr
    //

    GOTO(0xA7, "goto", OperandType.BRANCH_S2),
    JSR(0xA8, "jsr", OperandType.BRANCH_S2),
    RET(0xA9, "ret", OperandType.LOCAL_INDEX_U1),

    //
    // 0xAA..0xAB - switch
    //

    TABLESWITCH(0xAA, "tableswitch", OperandType.TABLESWITCH),
    LOOKUPSWITCH(0xAB, "lookupswitch", OperandType.LOOKUPSWITCH),

    //
    // 0xAC..0xB1 - возвраты
    //

    IRETURN(0xAC, "ireturn"),
    LRETURN(0xAD, "lreturn"),
    FRETURN(0xAE, "freturn"),
    DRETURN(0xAF, "dreturn"),
    ARETURN(0xB0, "areturn"),
    RETURN(0xB1, "return"),

    //
    // 0xB2..0xB5 - поля (cp_index u2)
    //

    GETSTATIC(0xB2, "getstatic", OperandType.CONSTPOOL_U2),
    PUTSTATIC(0xB3, "putstatic", OperandType.CONSTPOOL_U2),
    GETFIELD(0xB4, "getfield", OperandType.CONSTPOOL_U2),
    PUTFIELD(0xB5, "putfield", OperandType.CONSTPOOL_U2),

    //
    // 0xB6..0xBA - вызовы
    //

    INVOKEVIRTUAL(0xB6, "invokevirtual", OperandType.CONSTPOOL_U2),
    INVOKESPECIAL(0xB7, "invokespecial", OperandType.CONSTPOOL_U2),
    INVOKESTATIC(0xB8, "invokestatic", OperandType.CONSTPOOL_U2),
    INVOKEINTERFACE(0xB9, "invokeinterface", OperandType.CONSTPOOL_U2 /* cp_index, count, 0 */),
    INVOKEDYNAMIC(0xBA, "invokedynamic", OperandType.INVOKEDYNAMIC),

    //
    // 0xBB..0xBE - создание объектов / массивы
    //

    NEW(0xBB, "new", OperandType.CONSTPOOL_U2),

    NEWARRAY(0xBC, "newarray", OperandType.BYTE_IMM), // typecode:u1
    ANEWARRAY(0xBD, "anewarray", OperandType.CONSTPOOL_U2),
    ARRAYLENGTH(0xBE, "arraylength"),

    //
    // 0xBF - бросить исключение
    //

    ATHROW(0xBF, "athrow"),

    //
    // 0xC0..0xC3 - проверки типов и мониторы
    //

    CHECKCAST(0xC0, "checkcast", OperandType.CONSTPOOL_U2),
    INSTANCEOF(0xC1, "instanceof", OperandType.CONSTPOOL_U2),
    MONITORENTER(0xC2, "monitorenter"),
    MONITOREXIT(0xC3, "monitorexit"),

    //
    // 0xC4..0xC5 - wide / multianewarray
    //

    WIDE(0xC4, "wide", OperandType.WIDE),
    MULTIANEWARRAY(0xC5, "multianewarray", OperandType.MULTIANEWARRAY),

    //
    // 0xC6..0xC7 - ifnull/ifnonnull
    //

    IFNULL(0xC6, "ifnull", OperandType.BRANCH_S2),
    IFNONNULL(0xC7, "ifnonnull", OperandType.BRANCH_S2),

    //
    // 0xC8..0xC9 - длинные переходы
    //

    GOTO_W(0xC8, "goto_w", OperandType.BRANCH_S4),
    JSR_W(0xC9, "jsr_w", OperandType.BRANCH_S4),

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
