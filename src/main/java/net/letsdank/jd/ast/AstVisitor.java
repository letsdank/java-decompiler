package net.letsdank.jd.ast;

import net.letsdank.jd.ast.expr.*;
import net.letsdank.jd.ast.stmt.*;

/**
 * Базовый visitor для обхода AST выражений и операторов.
 * Реализует паттерн Visitor для упрощения трансформаций и анализа.
 */
public interface AstVisitor<R, P> {

    // === Expression visitors ===

    default R visitExpr(Expr expr, P param) {
        return switch (expr) {
            case IntConstExpr ic -> visitIntConst(ic, param);
            case VarExpr v -> visitVar(v, param);
            case BinaryExpr be -> visitBinary(be, param);
            case UnaryExpr ue -> visitUnary(ue, param);
            case CallExpr ce -> visitCall(ce, param);
            case FieldAccessExpr fa -> visitFieldAccess(fa, param);
            case StringLiteralExpr sl -> visitStringLiteral(sl, param);
            case CastExpr ce -> visitCast(ce, param);
            case InstanceOfExpr io -> visitInstanceOf(io, param);
            case NewExpr ne -> visitNew(ne, param);
            case UninitializedNewExpr une -> visitUninitializedNew(une, param);
            case ArrayAccessExpr aa -> visitArrayAccess(aa, param);
            case NewArrayExpr na -> visitNewArray(na, param);
            case ArrayLengthExpr al -> visitArrayLength(al, param);
            case TernaryExpr te -> visitTernary(te, param);
            case NullExpr ne -> visitNull(ne, param);
        };
    }

    default R visitIntConst(IntConstExpr expr, P param) {
        return null;
    }

    default R visitVar(VarExpr expr, P param) {
        return null;
    }

    default R visitBinary(BinaryExpr expr, P param) {
        visitExpr(expr.left(), param);
        visitExpr(expr.right(), param);
        return null;
    }

    default R visitUnary(UnaryExpr expr, P param) {
        visitExpr(expr.expr(), param);
        return null;
    }

    default R visitCall(CallExpr expr, P param) {
        if (expr.target() != null) {
            visitExpr(expr.target(), param);
        }
        for (Expr arg : expr.args()) {
            visitExpr(arg, param);
        }
        return null;
    }

    default R visitFieldAccess(FieldAccessExpr expr, P param) {
        visitExpr(expr.target(), param);
        return null;
    }

    default R visitStringLiteral(StringLiteralExpr expr, P param) {
        return null;
    }

    default R visitCast(CastExpr expr, P param) {
        visitExpr(expr.value(), param);
        return null;
    }

    default R visitInstanceOf(InstanceOfExpr expr, P param) {
        visitExpr(expr.value(), param);
        return null;
    }

    default R visitNew(NewExpr expr, P param) {
        for (Expr arg : expr.args()) {
            visitExpr(arg, param);
        }
        return null;
    }

    default R visitUninitializedNew(UninitializedNewExpr expr, P param) {
        return null;
    }

    default R visitArrayAccess(ArrayAccessExpr expr, P param) {
        visitExpr(expr.array(), param);
        visitExpr(expr.index(), param);
        return null;
    }

    default R visitNewArray(NewArrayExpr expr, P param) {
        visitExpr(expr.size(), param);
        return null;
    }

    default R visitArrayLength(ArrayLengthExpr expr, P param) {
        visitExpr(expr.array(), param);
        return null;
    }

    default R visitTernary(TernaryExpr expr, P param) {
        visitExpr(expr.condition(), param);
        visitExpr(expr.thenExpr(), param);
        visitExpr(expr.elseExpr(), param);
        return null;
    }

    default R visitNull(NullExpr expr, P param) {
        return null;
    }

    // === Statement visitors ===

    default R visitStmt(Stmt stmt, P param) {
        return switch (stmt) {
            case BlockStmt bs -> visitBlock(bs, param);
            case IfStmt ifs -> visitIf(ifs, param);
            case LoopStmt ls -> visitLoop(ls, param);
            case ForStmt fs -> visitFor(fs, param);
            case EnhancedForStmt efs -> visitEnhancedFor(efs, param);
            case SwitchStmt ss -> visitSwitch(ss, param);
            case AssignStmt as -> visitAssign(as, param);
            case ReturnStmt rs -> visitReturn(rs, param);
            case ExprStmt es -> visitExprStmt(es, param);
            case TryCatchStmt tcs -> visitTryCatch(tcs, param);
            case CatchClause cc -> visitCatchClause(cc, param);
            case FinallyClause fc -> visitFinallyClause(fc, param);
            case SynchronizedStmt ss -> visitSynchronized(ss, param);
            case CommentStmt cs -> visitComment(cs, param);
        };
    }

    default R visitBlock(BlockStmt stmt, P param) {
        for (Stmt s : stmt.statements()) {
            visitStmt(s, param);
        }
        return null;
    }

    default R visitIf(IfStmt stmt, P param) {
        visitExpr(stmt.condition(), param);
        visitBlock(stmt.thenBlock(), param);
        if (stmt.elseBlock() != null) {
            visitBlock(stmt.elseBlock(), param);
        }
        return null;
    }

    default R visitLoop(LoopStmt stmt, P param) {
        visitExpr(stmt.condition(), param);
        visitBlock(stmt.body(), param);
        return null;
    }

    default R visitFor(ForStmt stmt, P param) {
        if (stmt.init() != null) {
            visitStmt(stmt.init(), param);
        }
        visitExpr(stmt.condition(), param);
        if (stmt.update() != null) {
            visitStmt(stmt.update(), param);
        }
        visitBlock(stmt.body(), param);
        return null;
    }

    default R visitEnhancedFor(EnhancedForStmt stmt, P param) {
        visitExpr(stmt.iterable(), param);
        visitBlock(stmt.body(), param);
        return null;
    }

    default R visitSwitch(SwitchStmt stmt, P param) {
        visitExpr(stmt.selector(), param);
        for (var entry : stmt.cases().entrySet()) {
            visitBlock(entry.getValue(), param);
        }
        if (stmt.defaultBlock() != null) {
            visitBlock(stmt.defaultBlock(), param);
        }
        return null;
    }

    default R visitAssign(AssignStmt stmt, P param) {
        visitExpr(stmt.target(), param);
        visitExpr(stmt.value(), param);
        return null;
    }

    default R visitReturn(ReturnStmt stmt, P param) {
        if (stmt.value() != null) {
            visitExpr(stmt.value(), param);
        }
        return null;
    }

    default R visitExprStmt(ExprStmt stmt, P param) {
        visitExpr(stmt.expr(), param);
        return null;
    }

    default R visitTryCatch(TryCatchStmt stmt, P param) {
        visitBlock(stmt.tryBlock(), param);
        if (stmt.catchBlock() != null) {
            visitBlock(stmt.catchBlock(), param);
        }
        if (stmt.finallyBlock() != null) {
            visitBlock(stmt.finallyBlock(), param);
        }
        return null;
    }

    default R visitCatchClause(CatchClause stmt, P param) {
        if (stmt.filterExpr() != null) {
            visitExpr(stmt.filterExpr(), param);
        }
        for (Stmt s : stmt.body()) {
            visitStmt(s, param);
        }
        return null;
    }

    default R visitFinallyClause(FinallyClause stmt, P param) {
        for (Stmt s : stmt.body()) {
            visitStmt(s, param);
        }
        return null;
    }

    default R visitSynchronized(SynchronizedStmt stmt, P param) {
        visitExpr(stmt.monitor(), param);
        visitBlock(stmt.body(), param);
        return null;
    }

    default R visitComment(CommentStmt stmt, P param) {
        return null;
    }
}
