package net.letsdank.jd.ast;

import net.letsdank.jd.ast.expr.*;
import net.letsdank.jd.ast.stmt.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Устраняет мертвый (недостижимый) код из AST.
 *
 * Удаляет:
 * - Операторы после безусловного return/throw
 * - Пустые блоки
 * - Бесполезные присваивания (если переменная не используется дальше)
 */
public final class DeadCodeEliminator implements AstVisitor<Stmt, Void> {
    /**
     * Устраняет мертвый код из метода.
     */
    public MethodAst eliminate(MethodAst ast) {
        BlockStmt cleaned = (BlockStmt) visitBlock(ast.body(), null);
        return new MethodAst(ast.name(), ast.descriptor(), cleaned);
    }

    @Override
    public Stmt visitBlock(BlockStmt stmt, Void param) {
        List<Stmt> statements = stmt.statements();
        List<Stmt> result = new ArrayList<>();
        boolean reachedTerminator = false;

        for (Stmt s : statements) {
            if (reachedTerminator) {
                // Все после return/throw недостижимо
                break;
            }

            // Рекурсивно обрабатываем вложенные блоки
            Stmt cleaned = visitStmt(s, param);

            // Пропускаем пустые блоки и null
            if (cleaned == null || isEmpty(cleaned)) {
                continue;
            }

            result.add(cleaned);

            // Проверяем, является ли это терминирующим оператором
            if (isTerminator(cleaned)) {
                reachedTerminator = true;
            }
        }

        return new BlockStmt(result);
    }

    @Override
    public Stmt visitIf(IfStmt stmt, Void param) {
        BlockStmt thenCleaned = (BlockStmt) visitBlock(stmt.thenBlock(), param);
        BlockStmt elseCleaned = stmt.elseBlock() != null
                ? (BlockStmt) visitBlock(stmt.elseBlock(), param)
                : null;

        // Упрощение: if (true) { ... } -> ...
        if (isConstantTrue(stmt.condition())) {
            return thenCleaned;
        }

        // Упрощение: if (false) { ... } else { ... } -> ...
        if (isConstantFalse(stmt.condition())) {
            return elseCleaned != null ? elseCleaned : new BlockStmt();
        }

        // Если оба блока пустые, удаляем if
        if (isEmpty(thenCleaned) && (elseCleaned == null || isEmpty(elseCleaned))) {
            return null;
        }

        return new IfStmt(stmt.condition(), thenCleaned, elseCleaned);
    }

    @Override
    public Stmt visitLoop(LoopStmt stmt, Void param) {
        BlockStmt bodyCleaned = (BlockStmt) visitBlock(stmt.body(), param);

        // Упрощение: while (false) { ... } -> удаляем
        if (isConstantFalse(stmt.condition())) {
            return null;
        }

        // Если тело пустое, оставляем как есть (может быть намеренно)
        return new LoopStmt(stmt.condition(), bodyCleaned);
    }

    @Override
    public Stmt visitFor(ForStmt stmt, Void param) {
        BlockStmt bodyCleaned = (BlockStmt) visitBlock(stmt.body(), param);

        // Упрощение: for (...; false; ...) { ... } -> удаляем
        if (isConstantFalse(stmt.condition())) {
            // Но выполняем init, если он есть
            return stmt.init();
        }

        return new ForStmt(stmt.init(), stmt.condition(), stmt.update(), bodyCleaned);
    }

    @Override
    public Stmt visitEnhancedFor(EnhancedForStmt stmt, Void param) {
        BlockStmt bodyCleaned = (BlockStmt) visitBlock(stmt.body(), param);
        return new EnhancedForStmt(stmt.varType(), stmt.varName(), stmt.iterable(), bodyCleaned);
    }

    @Override
    public Stmt visitSwitch(SwitchStmt stmt, Void param) {
        Map<Integer, BlockStmt> cleanedCases = new LinkedHashMap<>();

        for (var entry : stmt.cases().entrySet()) {
            BlockStmt caseCleaned = (BlockStmt) visitBlock(entry.getValue(), param);
            if (!isEmpty(caseCleaned)) {
                cleanedCases.put(entry.getKey(), caseCleaned);
            }
        }

        BlockStmt defaultCleaned = stmt.defaultBlock() != null
                ? (BlockStmt) visitBlock(stmt.defaultBlock(), param)
                : null;

        // Если все ветки пустые, удаляем switch
        // (оставляем только выражение selector для побочных эффектов)
        if (cleanedCases.isEmpty() && (defaultCleaned == null || isEmpty(defaultCleaned))) {
            // Возвращаем ExprStmt с selector, если он имеет побочные эффекты
            if (hasSideEffects(stmt.selector())) {
                return new ExprStmt(stmt.selector());
            }
            return null;
        }

        return new SwitchStmt(stmt.selector(), cleanedCases, defaultCleaned);
    }

    @Override
    public Stmt visitTryCatch(TryCatchStmt stmt, Void param) {
        BlockStmt tryCleaned = (BlockStmt) visitBlock(stmt.tryBlock(), param);
        BlockStmt catchCleaned = stmt.catchBlock() != null
                ? (BlockStmt) visitBlock(stmt.catchBlock(), param)
                : null;
        BlockStmt finallyCleaned = stmt.finallyBlock() != null
                ? (BlockStmt) visitBlock(stmt.finallyBlock(), param)
                : null;

        return new TryCatchStmt(tryCleaned, stmt.exceptionType(), stmt.exceptionVarName(),
                catchCleaned, finallyCleaned);
    }

    @Override
    public Stmt visitCatchClause(CatchClause stmt, Void param) {
        List<Stmt> bodyCleaned = new ArrayList<>();
        for (Stmt s : stmt.body()) {
            Stmt cleaned = visitStmt(s, param);
            if (cleaned != null && !isEmpty(cleaned)) {
                bodyCleaned.add(cleaned);
            }
        }
        return new CatchClause(stmt.exceptionType(), stmt.varName(), bodyCleaned, stmt.filterExpr());
    }

    @Override
    public Stmt visitFinallyClause(FinallyClause stmt, Void param) {
        List<Stmt> bodyCleaned = new ArrayList<>();
        for (Stmt s : stmt.body()) {
            Stmt cleaned = visitStmt(s, param);
            if (cleaned != null && !isEmpty(cleaned)) {
                bodyCleaned.add(cleaned);
            }
        }
        return new FinallyClause(bodyCleaned);
    }

    @Override
    public Stmt visitSynchronized(SynchronizedStmt stmt, Void param) {
        BlockStmt bodyCleaned = (BlockStmt) visitBlock(stmt.body(), param);
        return new SynchronizedStmt(stmt.monitor(), bodyCleaned);
    }

    @Override
    public Stmt visitAssign(AssignStmt stmt, Void param) {
        // Оставляем присваивание как есть
        // TODO: анализ живых переменных для удаления бесполезных присваиваний
        return stmt;
    }

    @Override
    public Stmt visitReturn(ReturnStmt stmt, Void param) {
        return stmt;
    }

    @Override
    public Stmt visitBreak(BreakStmt stmt, Void param) {
        return stmt;
    }

    @Override
    public Stmt visitContinue(ContinueStmt stmt, Void param) {
        return stmt;
    }

    @Override
    public Stmt visitExprStmt(ExprStmt stmt, Void param) {
        // Удаляем ExprStmt без побочных эффектов
        if (!hasSideEffects(stmt.expr())) {
            return null;
        }
        return stmt;
    }

    @Override
    public Stmt visitComment(CommentStmt stmt, Void param) {
        return stmt;
    }

    // === Вспомогательные методы ===

    /**
     * Проверяем, является ли оператор терминирующим (после него код недостижим).
     */
    private boolean isTerminator(Stmt stmt) {
        if (stmt instanceof ReturnStmt) {
            return true;
        }
        if (stmt instanceof ExprStmt es) {
            // throw новое исключение
            Expr expr = es.expr();
            if (expr instanceof CallExpr ce) {
                // Проверяем, вызывается ли метод, который всегда бросает исключение
                // Например: throw new Exception(), System.exit()
                // Пока упрощенно не считаем это терминатором
            }
        }
        return false;
    }

    /**
     * Проверяет, является ли блок пустым.
     */
    private boolean isEmpty(Stmt stmt) {
        if (stmt instanceof BlockStmt bs) {
            return bs.statements().isEmpty();
        }
        return false;
    }

    /**
     * Проверяет, является ли условие константой true.
     */
    private boolean isConstantTrue(Expr expr) {
        if (expr instanceof IntConstExpr ic) {
            return ic.value() != 0;
        }
        // TODO: можно добавить более сложную проверку
        return false;
    }

    /**
     * Проверяет, является ли условие константой false.
     */
    private boolean isConstantFalse(Expr expr) {
        if (expr instanceof IntConstExpr ic) {
            return ic.value() == 0;
        }
        return false;
    }

    /**
     * Проверяет, имеет ли выражение побочные эффекты.
     */
    private boolean hasSideEffects(Expr expr) {
        return switch (expr) {
            case CallExpr ignored -> true; // вызовы методов всегда считаем имеющими эффекты
            case NewExpr ignored -> true; // создание объектов
            case NewArrayExpr ignored -> true;
            case BinaryExpr be -> hasSideEffects(be.left()) || hasSideEffects(be.right());
            case UnaryExpr ue -> hasSideEffects(ue.expr());
            case FieldAccessExpr fa -> hasSideEffects(fa.target());
            case ArrayAccessExpr aa -> hasSideEffects(aa.array()) || hasSideEffects(aa.index());
            case ArrayLengthExpr al -> hasSideEffects(al.array());
            case CastExpr ce -> hasSideEffects(ce.value());
            case InstanceOfExpr io -> hasSideEffects(io.value());
            case TernaryExpr te -> hasSideEffects(te.condition()) ||
                    hasSideEffects(te.thenExpr()) ||
                    hasSideEffects(te.elseExpr());
            case LambdaExpr le -> le.body() != null && hasSideEffects(le.body());
            case MethodRefExpr mr -> mr.target() != null && hasSideEffects(mr.target());
            default -> false; // константы, переменные не имеют побочных эффектов
        };
    }
}
