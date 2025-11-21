package net.letsdank.jd.fixtures

data class User(
    val id: Int,
    val name: String,
)

class SampleService {
    val greeting: String = "Hello"

    fun greet(user: User): String = "$greeting, ${user.name}!"

    fun internalWork(x: Int): Int {
        return x * 2
    }

    companion object {
        fun staticHello(): String = "Hi from companion"
    }
}

fun topLevelSum(a: Int, b: Int): Int = a + b