package net.letsdank.jd.bytecode;

/**
 * Какой операнд имеет инструкция.
 */
public enum OperandType {
    // без операндов
    NONE,

    // индексы локальных переменных
    LOCAL_INDEX_U1, // 1 байт: индекс локальной переменной
    LOCAL_INDEX_U2, // 2 байта: используется с wide/ret и т.п.

    // непосредственные значения
    BYTE_IMM,       // 1-байтовый immediate (bipush)
    SHORT_IMM,      // 2-байтовый immediate (sipush)

    // индексы в constant pool
    CONSTPOOL_U1,   // 1 байт: индекс в constant pool
    CONSTPOOL_U2,   // 2 байта: индекс в constant pool

    // спец-форматы
    IINC,           // index:u1, const:s1
    BRANCH_S2,      // относительный переход на short (if_xxx, goto, jsr)
    BRANCH_S4,      // относительный переход на int (goto_w, jsr_w)
    TABLESWITCH,    // сложный формат с padding'ом
    LOOKUPSWITCH,   // сложный формат с padding'ом
    INVOKEDYNAMIC,  // cp_index:u2, 0:u2
    MULTIANEWARRAY, // cp_index:u2, dimensions:u1
    WIDE            // префикс, модифицирующий следующий opcode
}
