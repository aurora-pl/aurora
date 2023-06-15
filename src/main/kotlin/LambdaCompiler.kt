var Env: Array<HashMap<Int, Any>> = Array(1) { HashMap() }
var ReturnValue: Any? = null
var BreakFlag: Boolean = false
var ContinueFlag: Boolean = false

operator fun Any.plus(other: Any): Any {
    return when (this) {
        is Int -> this + other as Int
        is Double -> this + other as Double
        is String -> this + other.toString()
        else -> throw Exception("invalid operand types for +: ${this::class} and ${other::class}")
    }
}

operator fun Any.minus(other: Any): Any {
    return when (this) {
        is Int -> this - other as Int
        is Double -> this - other as Double
        else -> throw Exception("invalid operand types for -: ${this::class} and ${other::class}")
    }
}

operator fun Any.times(other: Any): Any {
    return when (this) {
        is Int -> this * other as Int
        is Double -> this * other as Double
        else -> throw Exception("invalid operand types for *: ${this::class} and ${other::class}")
    }
}

operator fun Any.div(other: Any): Any {
    return when (this) {
        is Int -> this / other as Int
        is Double -> this / other as Double
        else -> throw Exception("invalid operand types for /: ${this::class} and ${other::class}")
    }
}

operator fun Any.rem(other: Any): Any {
    return when (this) {
        is Int -> this % other as Int
        is Double -> this % other as Double
        else -> throw Exception("invalid operand types for %: ${this::class} and ${other::class}")
    }
}

operator fun Any.compareTo(other: Any): Int {
    return when (this) {
        is Int -> this.compareTo(other as Int)
        is Double -> this.compareTo(other as Double)
        is String -> this.compareTo(other as String)
        else -> throw Exception("invalid operand types for comparison: ${this::class} and ${other::class}")
    }
}

class LambdaCompiler : Expr.Visitor<() -> Any>, Stmt.Visitor<() -> Boolean> {

    var envTracker = mutableListOf<MutableList<String>>()
    var biggestScope = 1

    init {
        envTracker.add(mutableListOf())
    }

    fun pushScope() {
        envTracker.add(mutableListOf())
        biggestScope++
    }

    fun popScope() {
        envTracker.removeAt(envTracker.lastIndex)
    }

    fun addVariable(name: String) {
        envTracker.last().add(name)
    }

    fun getScopeOf(name: String): Int {
        for ((i, env) in envTracker.withIndex().reversed()) {
            if (env.contains(name)) {
                return i
            }
        }
        return -1
    }

    override fun visit(expr: Binary): () -> Any {
        val source = source // TODO: make this better
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
            is Token.Arrow -> { ->
                // map operator
                val leftVal = left()
                val rightVal = right()
                if(rightVal !is AuroraFunction && rightVal !is AuroraNativeFn) {
                    Error(expr.right.line, expr.right.column, "cannot map ${rightVal::class}", source)
                    throw KillStage
                }
                rightVal as AuroraCallable
                when (leftVal) {
                    is List<*> -> leftVal.map { rightVal.call(listOf(it!!)) }
                    is Map<*, *> -> leftVal.mapValues { rightVal.call(listOf(it.value!!)) }
                    else -> {
                        Error(expr.left.line, expr.left.column, "cannot map ${leftVal::class}", source)
                        throw KillStage
                    }
                }
            }
            is Token.Dot -> {
                // foo | bar == bar(foo)
                // foo | bar(x, ...) == bar(foo, x, ...)
                // foo | (bar(x, ...)) == bar(x, ...)(foo)
                when (expr.right) {
                    is Call -> {
                        val rightArgs = expr.right.arguments.map { it.accept(this) }
                        val rightFn = expr.right.callee.accept(this)
                        return { ->
                            val right = rightFn()
                            if(right !is AuroraFunction && right !is AuroraNativeFn) {
                                Error(expr.right.callee.line, expr.right.callee.column, "cannot pipe to ${right::class}", source)
                            }
                            (right as AuroraCallable).call(mutableListOf(left(), *(rightArgs.map { it() }.toTypedArray())))
                        }
                    }
                    else -> {
                        val rightFn = expr.right.accept(this)
                        return { ->
                            val right = rightFn()
                            if(right !is AuroraFunction && right !is AuroraNativeFn) {
                                Error(expr.right.line, expr.right.column, "cannot pipe to ${right::class}", source)
                            }
                            (rightFn() as AuroraCallable).call(listOf(left()))
                        }
                    }
                }
            }
            else -> {
                Error(expr.operator.line, expr.operator.column, "invalid binary operator: ${expr.operator}", source)
                throw KillStage
            }
        }
    }

    override fun visit(expr: Unary): () -> Any {
        val source = source // TODO: make this better
        val right = expr.right.accept(this)

        return when (expr.operator) {
            is Token.Minus -> { -> -(right() as Double) }
            is Token.Not -> { -> !(right() as Boolean) }
            else -> {
                Error(expr.operator.line, expr.operator.column, "invalid unary operator: ${expr.operator}", source)
                throw KillStage
            }
        }
    }

    override fun visit(expr: Literal): () -> Any {
        return { -> expr.value.literal!! }
    }

    override fun visit(expr: Variable): () -> Any {
        val source = source // TODO: make this better
        val index = getScopeOf(expr.name.lexeme)
        if (index == -1) {
            Error(expr.line, expr.column,"undefined variable: ${expr.name.lexeme}", source).print()
            throw KillStage
        }
        val nameHashCode = expr.name.lexeme.hashCode()
        return { ->
            Env[index][nameHashCode]!!
        }
    }

    override fun visit(expr: ListLiteral): () -> Any {
        val list = mutableListOf<() -> Any>()
        for (item in expr.elements) {
            list.add(item.accept(this))
        }
        return { -> list.map { it() } }
    }

    override fun visit(expr: Index): () -> Any {
        val source = source // TODO: make this better
        val list = expr.list.accept(this)
        val index = expr.index.accept(this)
        return { ->
            when(val listVal = list()) {
                is List<*> -> listVal[(index() as Number).toInt()]!!
                is Map<*, *> -> listVal[index()]!!
                else -> {
                    Error(expr.line, expr.column, "cannot index ${listVal::class}", source).print()
                    throw KillStage
                }
            }
        }
    }

    override fun visit(expr: MapLiteral): () -> Any {
        val map = mutableMapOf<() -> Any, () -> Any>()
        for ((key, value) in expr.pairs) {
            map[key.accept(this)] = value.accept(this)
        }
        return { -> map.mapKeys { it.key() }.mapValues { it.value() } }
    }

    override fun visit(expr: Call): () -> Any {
        val source = source // TODO: make this better
        val callee = expr.callee.accept(this)
        val args = mutableListOf<() -> Any>()
        for (arg in expr.arguments) {
            args.add(arg.accept(this))
        }
        return { ->
            val func = callee()
            if (func !is AuroraCallable) {
                Error(expr.line, expr.column, "cannot call ${func::class}", source).print()
                throw KillStage
            }
            if (func is AuroraSubroutine || func is AuroraNativeSub) {
                Error(expr.line, expr.column, "cannot call subroutine in expression", source).print()
                throw KillStage
            }
            if (func.arity() != null && args.size != func.arity()) {
                Error(expr.line, expr.column, "expected ${func.arity()} arguments but got ${args.size}", source).print()
                throw KillStage
            }
            func.call(List(expr.arguments.size) { i -> args[i]() })
        }
    }

    override fun visit(expr: Lambda): () -> Any {
        val source = source // TODO: make this better
        val tmpEnv = envTracker
        envTracker = mutableListOf(envTracker[0])
        val prevBiggestScope = biggestScope
        pushScope()
        for (param in expr.args) {
            addVariable(param.lexeme)
        }
        val fnBody = expr.body.accept(this)
        popScope()
        val argHashCodes = expr.args.map { it.lexeme.hashCode() }
        val depth = biggestScope
        biggestScope = prevBiggestScope
        val fnObj = { args: List<Any> ->
            val tmpEnv = Env
            Env = Array(depth) { if (it == 0) Env[0] else hashMapOf() }
            for ((i, arg) in args.withIndex()) {
                Env[1][argHashCodes[i]] = arg
            }
            if(!fnBody() || ReturnValue == null) {
                Error(expr.line, expr.column, "function [lambda at ${expr.line}:${expr.column}] did not return a value", source).print()
                throw KillStage
            }
            val result = ReturnValue!!
            ReturnValue = null
            Env = tmpEnv
            result
        }
        envTracker = tmpEnv
        return { ->
            AuroraFunction("[lambda at ${expr.line}:${expr.column}]", expr.args.size, fnObj)
        }
    }

    override fun visit(expr: DoBlock): () -> Any {
        val source = source // TODO: make this better
        val tmpEnv = envTracker
        envTracker = mutableListOf(envTracker[0])
        val prevBiggestScope = biggestScope
        pushScope()
        for (param in expr.args) {
            addVariable(param.lexeme)
        }
        val fnBody = expr.body.accept(this)
        popScope()
        val argHashCodes = expr.args.map { it.lexeme.hashCode() }
        val depth = biggestScope
        biggestScope = prevBiggestScope
        val fnObj = { args: List<Any> ->
            val tmpEnv = Env
            Env = Array(depth) { if (it == 0) Env[0] else hashMapOf() }
            for ((i, arg) in args.withIndex()) {
                Env[1][argHashCodes[i]] = arg
            }
            fnBody()
            if(ReturnValue != null) {
                Error(expr.line, expr.column, "subroutine [do block at ${expr.line}:${expr.column}] cannot return a value", source).print()
                throw KillStage
            }
            Env = tmpEnv
        }
        envTracker = tmpEnv
        return { ->
            AuroraSubroutine("[do block at ${expr.line}:${expr.column}]", expr.args.size, fnObj)
        }
    }

    override fun visit(stmt: If): () -> Boolean {
        val condition = stmt.condition.accept(this)
        pushScope()
        val thenBranch = stmt.thenBranch.accept(this)
        popScope()
        val elseBranch = if(stmt.elseBranch != null) {
            pushScope()
            val elseBranch = stmt.elseBranch.accept(this)
            popScope()
            elseBranch
        } else null
        return { ->
            if (condition() as Boolean) {
                thenBranch()
            } else {
                elseBranch?.invoke() ?: false
            }
        }
    }

    override fun visit(stmt: While): () -> Boolean {
        val condition = stmt.condition.accept(this)
        pushScope()
        val body = stmt.body.accept(this)
        popScope()
        return invoke@ { ->
            while (condition() as Boolean) {
                if (body()) {
                    if(BreakFlag) {
                        BreakFlag = false
                        break
                    } else if(ContinueFlag) {
                        ContinueFlag = false
                        continue
                    } else {
                        return@invoke true
                    }
                }
            }
            false
        }
    }

    override fun visit(stmt: For): () -> Boolean {
        val name = stmt.variable.lexeme
        val iterable = stmt.iterable.accept(this)
        pushScope()
        addVariable(name)
        val body = stmt.body.accept(this)
        popScope()
        val nameHashCode = name.hashCode()
        return invoke@ { ->
            for (item in iterable() as Iterable<*>) {
                Env.last()[nameHashCode] = item as Any
                if (body()) {
                    if(BreakFlag) {
                        BreakFlag = false
                        break
                    } else if(ContinueFlag) {
                        ContinueFlag = false
                        continue
                    } else {
                        return@invoke true
                    }
                }
            }
            false
        }
    }

    override fun visit(stmt: Assign): () -> Boolean {
        val value = stmt.value.accept(this)
        var scope = getScopeOf(stmt.name.lexeme)
        if (scope == -1) {
            addVariable(stmt.name.lexeme)
            scope = envTracker.lastIndex
        }
        val nameHashCode = stmt.name.lexeme.hashCode()
        return { ->
            Env[scope][nameHashCode] = value()
            false
        }
    }

    override fun visit(stmt: Func): () -> Boolean {
        val source = source // TODO: make this better
        addVariable(stmt.name.lexeme)
        val tmpEnv = envTracker
        envTracker = mutableListOf(envTracker[0])
        val prevBiggestScope = biggestScope
        pushScope()
        for (param in stmt.params) {
            addVariable(param.lexeme)
        }
        val fnBody = stmt.body.accept(this)
        popScope()
        val argHashCodes = stmt.params.map { it.lexeme.hashCode() }
        val depth = biggestScope
        biggestScope = prevBiggestScope
        val fnObj = { args: List<Any> ->
            val tmpEnv = Env
            Env = Array(depth) { if (it == 0) Env[0] else hashMapOf() }
            for ((i, arg) in args.withIndex()) {
                Env[1][argHashCodes[i]] = arg
            }
            if(!fnBody() || ReturnValue == null) {
                Error(stmt.line, stmt.column, "function ${stmt.name.lexeme} did not return a value", source).print()
                throw KillStage
            }
            val result = ReturnValue!!
            ReturnValue = null
            Env = tmpEnv
            result
        }
        envTracker = tmpEnv
        val nameHashCode = stmt.name.lexeme.hashCode()
        return { ->
            Env[0][nameHashCode] = AuroraFunction(stmt.name.lexeme, stmt.params.size, fnObj)
            false
        }
    }

    override fun visit(stmt: Sub): () -> Boolean {
        val source = source // TODO: make this better
        addVariable(stmt.name.lexeme)
        val tmpEnv = envTracker
        envTracker = mutableListOf(envTracker[0])
        val prevBiggestScope = biggestScope
        pushScope()
        for (param in stmt.params) {
            addVariable(param.lexeme)
        }
        val fnBody = stmt.body.accept(this)
        popScope()
        val argHashCodes = stmt.params.map { it.lexeme.hashCode() }
        val depth = biggestScope
        biggestScope = prevBiggestScope
        val fnObj = { args: List<Any> ->
            val tmpEnv = Env
            Env = Array(depth) { if (it == 0) Env[0] else hashMapOf() }
            for ((i, arg) in args.withIndex()) {
                Env[1][argHashCodes[i]] = arg
            }
            fnBody()
            if(ReturnValue != null) {
                Error(stmt.line, stmt.column, "subroutine ${stmt.name.lexeme} cannot return a value", source).print()
                throw KillStage
            }
            Env = tmpEnv
        }
        envTracker = tmpEnv
        val nameHashCode = stmt.name.lexeme.hashCode()
        return { ->
            Env[0][nameHashCode] = AuroraSubroutine(stmt.name.lexeme, stmt.params.size, fnObj)
            false
        }
    }

    override fun visit(stmt: Return): () -> Boolean {
        val value = stmt.value?.accept(this)
        return { ->
            if (value != null)
                ReturnValue = value()
            true
        }
    }

    override fun visit(stmt: Break): () -> Boolean {
        return { ->
            BreakFlag = true
            true
        }
    }

    override fun visit(stmt: Continue): () -> Boolean {
        return { ->
            ContinueFlag = true
            true
        }
    }

    override fun visit(stmt: AssignIndex): () -> Boolean {
        val source = source // TODO: make this better
        val list = stmt.list.accept(this)
        val index = stmt.index.accept(this)
        val value = stmt.value.accept(this)
        return { ->
            (list() as? MutableList<Any>)?.set((index() as Number).toInt(), value()) ?:
            (list() as? MutableMap<Any, Any>)?.set(index(), value()) ?:
            Error(stmt.line, stmt.column, "cannot assign to index of non-list", source).print()
                .also { throw KillStage }
            false
        }
    }

    override fun visit(stmt: CallStmt): () -> Boolean {
        val source = source // TODO: make this better
        val callee = stmt.callee.accept(this)
        val args = mutableListOf<() -> Any>()
        for (arg in stmt.arguments) {
            args.add(arg.accept(this))
        }
        return { ->
            val func = callee()
            if (func !is AuroraCallable) {
                Error(stmt.line, stmt.column, "can only call functions and subroutines", source).print()
                throw KillStage
            }
            if (func.arity() != null && args.size != func.arity()) {
                Error(stmt.line, stmt.column, "expected ${func.arity()} arguments but got ${args.size}", source).print()
                throw KillStage
            }
            func.call(args.map { it() })
            false
        }
    }

    override fun visit(stmt: Block): () -> Boolean {
        val stmts = stmt.statements.map { it.accept(this) }
        return invoke@ { ->
            for(stmt in stmts) {
                if(stmt()) return@invoke true
            }
            false
        }
    }

    override fun visit(stmt: Switch): () -> Boolean {
        val condition = stmt.value.accept(this)
        val cases = stmt.cases.map { Pair(it.first.accept(this), it.second.accept(this)) }
        val default = stmt.default?.accept(this)
        return invoke@ { ->
            val cond = condition()
            for (case in cases) {
                if (case.first() == cond) {
                    if (case.second()) return@invoke true
                }
            }
            if (default != null) {
                if (default()) return@invoke true
            }
            false
        }
    }

    override fun visit(stmt: Select): () -> Boolean {
        val cases = stmt.cases.map { Pair(it.first.accept(this), it.second.accept(this)) }
        val default = stmt.default?.accept(this)
        return invoke@ { ->
            var doElse = true
            for (case in cases) {
                if (case.first() as Boolean) {
                    if (case.second()) return@invoke true
                    doElse = false
                }
            }
            if (default != null && doElse) {
                if (default()) return@invoke true
            }
            false
        }
    }

}