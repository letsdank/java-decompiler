package net.letsdank.jd.ast;

/**
 * Дает имена локальных переменных по индексу.
 * На старте можно просто генерировать v0, v1 и т.п.
 */
public interface LocalNameProvider {
    String nameForLocal(int index);
}
