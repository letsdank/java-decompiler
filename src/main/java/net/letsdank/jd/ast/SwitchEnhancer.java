package net.letsdank.jd.ast;

import net.letsdank.jd.ast.expr.Expr;
import net.letsdank.jd.ast.stmt.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Улучшает представление switch statements:
 * - Объединяет несколько case с одинаковым телом
 * - Обнаруживает fallthrough и добавляет комментарии
 * - Минимизирует дублирование кода
 */
public final class SwitchEnhancer {

    /**
     * Улучшает switch statement.
     */
    public SwitchStmt enhance(SwitchStmt switchStmt) {
        Map<Integer, BlockStmt> cases = switchStmt.cases();
        BlockStmt defaultBlock = switchStmt.defaultBlock();

        // 1. Находим case с идентичным телом для объединения
        Map<BlockStmt, List<Integer>> blockToValues = groupIdenticalBlocks(cases);

        // 2. Если у нас есть группы из нескольких case с одним телом,
        // создаем более компактное представление
        if (hasMultipleCasesPerBlock(blockToValues)) {
            return createCompactSwitch(switchStmt.selector(), blockToValues, defaultBlock);
        }

        // 3. Обнаруживаем fallthrough паттерны
        SwitchStmt withFallthrough = detectFallthrough(switchStmt);

        return withFallthrough;
    }

    /**
     * Группирует case по их телу
     */
    private Map<BlockStmt, List<Integer>> groupIdenticalBlocks(Map<Integer, BlockStmt> cases) {
        Map<BlockStmt, List<Integer>> result = new LinkedHashMap<>();

        for (var entry : cases.entrySet()) {
            BlockStmt block = entry.getValue();
            Integer value = entry.getKey();

            // Ищем эквивалентный блок
            BlockStmt existing = null;
            for (BlockStmt key : result.keySet()) {
                if (areBlocksEquivalent(key, block)) {
                    existing = key;
                    break;
                }
            }

            if (existing != null) {
                result.get(existing).add(value);
            } else {
                List<Integer> values = new ArrayList<>();
                values.add(value);
                result.put(block, values);
            }
        }

        return result;
    }

    /**
     * Проверяет, эквивалентны ли два блока.
     */
    private boolean areBlocksEquivalent(BlockStmt a, BlockStmt b) {
        if (a == b) return true;
        if (a == null || b == null) return false;

        List<Stmt> stmtsA = a.statements();
        List<Stmt> stmtsB = b.statements();

        if (stmtsA.size() != stmtsB.size()) return false;

        for (int i = 0; i < stmtsA.size(); i++) {
            if (!statementsEqual(stmtsA.get(i), stmtsB.get(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Проверяет равенство statements (упрощенно).
     */
    private boolean statementsEqual(Stmt a, Stmt b) {
        // Для полной проверки нужен глубокий анализ AST
        // Здесь используем упрощенный подход через toString()
        return a.toString().equals(b.toString());
    }

    /**
     * Проверяет, есть ли case с несколькими метками.
     */
    private boolean hasMultipleCasesPerBlock(Map<BlockStmt, List<Integer>> blockToValues) {
        for (List<Integer> values : blockToValues.values()) {
            if (values.size() > 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Создает компактный switch с множественными case метками.
     *
     * Вместо:
     *   case 1: stmt; break;
     *   case 2: stmt; break;
     *
     * Генерируем:
     *   case 1:
     *   case 2:
     *     stmt;
     *     break;
     */
    private SwitchStmt createCompactSwitch(Expr selector,
                                           Map<BlockStmt, List<Integer>> blockToValues,
                                           BlockStmt defaultBlock) {
        Map<Integer, BlockStmt> newCases = new LinkedHashMap<>();

        for (var entry : blockToValues.entrySet()) {
            BlockStmt block = entry.getKey();
            List<Integer> values = entry.getValue();

            // Сортируем значения для красивого вывода
            values.sort(Integer::compareTo);

            // Первые N-1 значений получают пустой блок (fallthrough)
            for (int i = 0; i < values.size() - 1; i++) {
                newCases.put(values.get(i), new BlockStmt());
            }

            // Последнее значение получает настоящий блок
            newCases.put(values.getLast(), block);
        }

        return new SwitchStmt(selector, newCases, defaultBlock);
    }

    /**
     * Обнаруживаем fallthrough паттерны и добавляет комментарии.
     */
    private SwitchStmt detectFallthrough(SwitchStmt switchStmt) {
        Map<Integer, BlockStmt> cases = switchStmt.cases();
        Map<Integer, BlockStmt> enhanced = new LinkedHashMap<>();

        List<Integer> sortedKeys = new ArrayList<>(cases.keySet());
        sortedKeys.sort(Integer::compareTo);

        for (int i = 0; i < sortedKeys.size(); i++) {
            Integer key = sortedKeys.get(i);
            BlockStmt block = cases.get(key);

            // Проверяем, есть ли break/return в конце блока
            if (!endsWithTerminator(block) && i < sortedKeys.size() - 1) {
                // Это fallthrough! Добавляем комментарий
                BlockStmt withComment = addFallthroughComment(block);
                enhanced.put(key, withComment);
            } else {
                enhanced.put(key, block);
            }
        }

        return new SwitchStmt(switchStmt.selector(), enhanced, switchStmt.defaultBlock());
    }

    /**
     * Проверяет, заканчивается ли блок терминатором (break/return/throw).
     */
    private boolean endsWithTerminator(BlockStmt block) {
        List<Stmt> stmts = block.statements();
        if (stmts.isEmpty()) return false;

        Stmt last = stmts.getLast();
        return last instanceof ReturnStmt || last instanceof BreakStmt;
    }

    /**
     * Проверяет, является ли statement break'ом.
     */
    private boolean isBreakComment(Stmt stmt) {
        return stmt instanceof BreakStmt;
    }

    /**
     * Добавляет комментарий о fallthrough.
     */
    private BlockStmt addFallthroughComment(BlockStmt block) {
        List<Stmt> stmts = new ArrayList<>(block.statements());
        stmts.add(new CommentStmt("// fallthrough"));
        return new BlockStmt(stmts);
    }
}
