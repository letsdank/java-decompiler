package net.letsdank.jd.model.attribute;

import net.letsdank.jd.model.LineNumberTableAttribute;
import net.letsdank.jd.model.LocalVariableTableAttribute;

import java.util.List;

/**
 * Представление атрибута Code метода.
 * Пока: только "сырые" данные байткода и базовая информация.
 *
 * @param name                   Всегда "Code"
 * @param maxStack               Максимальная глубина стека
 * @param maxLocals              Кол-во локальных переменных
 * @param code                   Байткод
 * @param exceptionTable         Таблица обработчиков исключений (exception_table из spec)
 * @param lineNumberTable        Атрибут LineNumberTable, если есть
 * @param localVariableAttribute Атрибут LocalVariableTable, если есть
 */
public record CodeAttribute(String name, int maxStack, int maxLocals, byte[] code,
                            List<ExceptionTableEntry> exceptionTable,
                            LineNumberTableAttribute lineNumberTable,
                            LocalVariableTableAttribute localVariableAttribute) implements AttributeInfo {

    /**
     * Одна запись из exception_table.
     * <p>
     * Поля совпадают со структурой из JVM spec:
     * - startPc / endPc - диапазон защищенного кода
     * - handlerPc - адрес обработчика
     * - catchTypeIndex - индекс в constant pool (0 = finally / catch all)
     */
    public record ExceptionTableEntry(
            int startPc,
            int endPc,
            int handlerPc,
            int catchTypeIndex
    ) {
    }
}
