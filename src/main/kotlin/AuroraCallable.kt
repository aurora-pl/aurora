interface AuroraCallable {
    fun arity(): Int?
    fun call(arguments: List<Any>): Any
    override fun toString(): String
}

class AuroraFunction(
    private val name: String,
    private val arity: Int,
    private val function: (List<Any>) -> Any
) : AuroraCallable {
    override fun arity(): Int {
        return arity
    }

    override fun call(arguments: List<Any>): Any {
        return function(arguments)
    }

    override fun toString(): String {
        return "[fn $name]"
    }
}

class AuroraSubroutine(
    private val name: String,
    private val arity: Int,
    private val function: (List<Any>) -> Unit
) : AuroraCallable {
    override fun arity(): Int {
        return arity
    }

    override fun call(arguments: List<Any>): Any {
        return function(arguments)
    }

    override fun toString(): String {
        return "[sub $name]"
    }
}

class AuroraNativeFn (
    private val name: String,
    private val arity: Int? = null,
    private val function: (List<Any>) -> Any
) : AuroraCallable {
    override fun arity(): Int? {
        return arity
    }

    override fun call(arguments: List<Any>): Any {
        return function(arguments)
    }

    override fun toString(): String {
        return "[native fn $name]"
    }
}

class AuroraNativeSub (
    private val name: String,
    private val arity: Int? = null,
    private val function: (List<Any>) -> Unit
) : AuroraCallable {
    override fun arity(): Int? {
        return arity
    }

    override fun call(arguments: List<Any>): Any {
        function(arguments)
        return Unit
    }

    override fun toString(): String {
        return "[native sub $name]"
    }
}