package net.letsdank.jd.ast.expr;

/**
 * AST выражений.
 */
public sealed interface Expr
        permits IntConstExpr, VarExpr, BinaryExpr, UnaryExpr,
        CallExpr, FieldAccessExpr, StringLiteralExpr,
        CastExpr, InstanceOfExpr, NewExpr, UninitializedNewExpr {
}
