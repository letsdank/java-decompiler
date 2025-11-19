package net.letsdank.jd.bytecode;

/**
 * Набор известных опкодов. Пока только те, что нужны для
 * простых методов из тестов. Остальные будем считать неизвестными.
 */
public enum Opcode {
    ILOAD_0(0x1A, "iload_0"),
    ILOAD_1(0x1B,"iload_1"),
    ILOAD_2(0x1C,"iload_2"),
    ILOAD_3(0x1D,"iload_3"),

    IADD(0x60,"iadd"),

    IRETURN(0xAC,"ireturn"),
    RETURN(0xB1,"return");

    private final int code;
    private final String mnemonic;

    Opcode(int code, String mnemonic) {
        this.code = code;
        this.mnemonic = mnemonic;
    }

    public int code() {
        return code;
    }

    public String mnemonic() {
        return mnemonic;
    }

    public static Opcode fromCode(int code) {
        for (Opcode op : values()) {
            if (op.code == code) return op;
        }
        return null;
    }
}
