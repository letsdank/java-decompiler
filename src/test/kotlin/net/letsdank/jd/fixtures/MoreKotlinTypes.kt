package net.letsdank.jd.fixtures

enum class Color {
    RED, GREEN, BLUE
}

sealed class Expr {
    data class Const(val value: Int) : Expr()
    data class Sum(val left: Expr, val right: Expr) : Expr()
    object None : Expr()
}

@JvmInline
value class UserId(val value: Int)

object GlobalObject {
    fun answer(): Int = 42
}
