package net.letsdank.jd.cfg.exception;

import net.letsdank.jd.cfg.BasicBlock;

import java.util.List;

/**
 * Структурированное представление одного try/catch(/finally) региона.
 * <p>
 * Ровно один "protected range" (диапазон PC) и набор "обработчиков".
 */
public record ExceptionRegion(int startPc, int endPc, List<Handler> handlers) {

    /**
     * Описание одного обработчика.
     * <p>
     * catchTypeIndex == 0 означает "любой" (finally / catch(Throwable)).
     */
    public record Handler(int catchTypeIndex, BasicBlock handleBlock) {
        public boolean isCatchAll() {
            return catchTypeIndex == 0;
        }
    }
}
