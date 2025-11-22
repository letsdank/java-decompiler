package net.letsdank.jd.bytecode;

/**
 * Какой операнд имеет инструкция.
 */
public enum OperandType {
    NONE,           // без операндов
    LOCAL_INDEX_U1, // 1 байт: индекс локальной переменной
    CONSTPOOL_U1,   // 1 байт: индекс в constant pool
    CONSTPOOL_U2,   // 2 байта: индекс в constant pool
    BRANCH_S2,      // переход: 2-байтовый signed offset относительно следующей инструкции
    BYTE_IMM,       // 1-байтовый immediate (bipush)
    SHORT_IMM,      // 2-байтовый immediate (sipush)
    IINC,
    INVOKEDYNAMIC   // специальный формат: cp_index:u2 + 2 байта нулей
}
