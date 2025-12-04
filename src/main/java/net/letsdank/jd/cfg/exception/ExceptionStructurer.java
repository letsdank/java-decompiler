package net.letsdank.jd.cfg.exception;

import net.letsdank.jd.cfg.BasicBlock;
import net.letsdank.jd.cfg.ControlFlowGraph;
import net.letsdank.jd.model.attribute.CodeAttribute;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Построитель структурированных exception- регионов.
 * <p>
 * Задача: сгруппировать записи из exception_table в осмысленные ExceptionRegion'ы,
 * привязать handlerPc к BasicBlock'ам и выдать список регионов.
 */
public final class ExceptionStructurer {
    private final CodeAttribute code;
    private final ControlFlowGraph cfg;

    public ExceptionStructurer(CodeAttribute code, ControlFlowGraph cfg) {
        this.code = code;
        this.cfg = cfg;
    }

    public List<ExceptionRegion> buildRegions() {
        List<ExceptionRegion> result = new ArrayList<>();

        // сгруппируем по startPc/endPc
        Map<RangeKey, List<CodeAttribute.ExceptionTableEntry>> grouped = groupEntriesByRange();

        for (Map.Entry<RangeKey, List<CodeAttribute.ExceptionTableEntry>> entry : grouped.entrySet()) {
            RangeKey key = entry.getKey();
            List<CodeAttribute.ExceptionTableEntry> entries = entry.getValue();

            List<ExceptionRegion.Handler> handlers = new ArrayList<>();
            for (CodeAttribute.ExceptionTableEntry e : entries) {
                BasicBlock handlerBlock = cfg.blockByStartOffset(e.handlerPc());
                if (handlerBlock == null) {
                    // может быть странный класс или мы еще не построили блоки это этого pc
                    // логируем и продолжаем
                    System.out.println("No basic block for handlerPc=" + e.handlerPc());
                    continue;
                }
                handlers.add(new ExceptionRegion.Handler(e.catchTypeIndex(), handlerBlock));
            }

            if (!handlers.isEmpty()) {
                result.add(new ExceptionRegion(key.startPc, key.endPc, handlers));
            }
        }

        return result;
    }

    private Map<RangeKey, List<CodeAttribute.ExceptionTableEntry>> groupEntriesByRange() {
        Map<RangeKey, List<CodeAttribute.ExceptionTableEntry>> grouped = new LinkedHashMap<>();

        for (CodeAttribute.ExceptionTableEntry e : code.exceptionTable()) {
            RangeKey key = new RangeKey(e.startPc(), e.endPc());
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }

        return grouped;
    }

    private record RangeKey(int startPc, int endPc) {
    }
}
