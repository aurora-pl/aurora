val ExecutionContext = object {
    val variables = Array<Any?>(256) { null }
}

operator fun Any.plus(other: Any): Any {
    return when (this) {
        is Int -> this + other as Int
        is Double -> this + other as Double
        is String -> this + other as String
        else -> throw Exception("Invalid operand types for +: ${this::class} and ${other::class}")
    }
}

operator fun Any.minus(other: Any): Any {
    return when (this) {
        is Int -> this - other as Int
        is Double -> this - other as Double
        else -> throw Exception("Invalid operand types for -: ${this::class} and ${other::class}")
    }
}

operator fun Any.times(other: Any): Any {
    return when (this) {
        is Int -> this * other as Int
        is Double -> this * other as Double
        else -> throw Exception("Invalid operand types for *: ${this::class} and ${other::class}")
    }
}

operator fun Any.div(other: Any): Any {
    return when (this) {
        is Int -> this / other as Int
        is Double -> this / other as Double
        else -> throw Exception("Invalid operand types for /: ${this::class} and ${other::class}")
    }
}

operator fun Any.rem(other: Any): Any {
    return when (this) {
        is Int -> this % other as Int
        is Double -> this % other as Double
        else -> throw Exception("Invalid operand types for %: ${this::class} and ${other::class}")
    }
}

operator fun Any.compareTo(other: Any): Int {
    return when (this) {
        is Int -> this.compareTo(other as Int)
        is Double -> this.compareTo(other as Double)
        is String -> this.compareTo(other as String)
        else -> throw Exception("Invalid operand types for ==: ${this::class} and ${other::class}")
    }
}

class LambdaCompiler : Expr.Visitor<() -> Any>, Stmt.Visitor<() -> Boolean> {
    override fun visit(expr: Binary): () -> Any {
        val left = expr.left.accept(this)
        val right = expr.right.accept(this)

        return when (expr.operator) {
            is Token.Plus -> { -> left() + right() }
            is Token.Minus -> { -> left() - right() }
            is Token.Star -> { -> left() * right() }
            is Token.Slash -> { -> left() / right() }
            is Token.Mod -> { -> left() % right() }
            is Token.Eq -> { -> left() == right() }
            is Token.NotEq -> { -> left() != right() }
            is Token.Less -> { -> left() < right() }
            is Token.LessEq -> { -> left() <= right() }
            is Token.Greater -> { -> left() > right() }
            is Token.GreaterEq -> { -> left() >= right() }
            else -> throw Exception("Invalid binary operator: ${expr.operator}")
        }
    }

    override fun visit(expr: Unary): () -> Any {
        val right = expr.right.accept(this)

        return when (expr.operator) {
            is Token.Minus -> { -> -(right() as Double) }
            is Token.Not -> { -> !(right() as Boolean) }
            else -> throw Exception("Invalid unary operator: ${expr.operator}")
        }
    }

    override fun visit(expr: Literal): () -> Any {
        return { -> expr.value.literal!! }
    }

    override fun visit(expr: Variable): () -> Any {
        TODO("Not yet implemented")
    }

    override fun visit(expr: ListLiteral): () -> Any {
        TODO("Not yet implemented")
    }

    override fun visit(expr: Index): () -> Any {
        TODO("Not yet implemented")
    }

    override fun visit(expr: MapLiteral): () -> Any {
        TODO("Not yet implemented")
    }

    override fun visit(expr: Call): () -> Any {
        TODO("Not yet implemented")
    }

    override fun visit(stmt: If): () -> Boolean {
        TODO("Not yet implemented")
    }

    override fun visit(stmt: While): () -> Boolean {
        TODO("Not yet implemented")
    }

    override fun visit(stmt: For): () -> Boolean {
        TODO("Not yet implemented")
    }

    override fun visit(stmt: Assign): () -> Boolean {
        TODO("Not yet implemented")
    }

    override fun visit(stmt: Func): () -> Boolean {
        TODO("Not yet implemented")
    }

    override fun visit(stmt: Sub): () -> Boolean {
        TODO("Not yet implemented")
    }

    override fun visit(stmt: Return): () -> Boolean {
        TODO("Not yet implemented")
    }

    override fun visit(stmt: Break): () -> Boolean {
        TODO("Not yet implemented")
    }

    override fun visit(stmt: Continue): () -> Boolean {
        TODO("Not yet implemented")
    }

    override fun visit(stmt: AssignIndex): () -> Boolean {
        TODO("Not yet implemented")
    }

    override fun visit(stmt: CallStmt): () -> Boolean {
        TODO("Not yet implemented")
    }

    override fun visit(stmt: Block): () -> Boolean {
        TODO("Not yet implemented")
    }

}