import java.io.File
import java.util.*
import kotlin.math.*

val print = AuroraNativeSub("print", null) { args ->
    for (arg in args) {
        print(arg)
    }
    println()
}

val `copy!` = AuroraNativeSub("copy!", 1) { args ->
    // file copy, overwrite
    val file = args[0] as String
    val file2 = args[1] as String
    val source = File(file)
    val dest = File(file2)
    source.copyTo(dest, true)
}

val copy = AuroraNativeSub("copy", 1) { args ->
    // file copy
    val file = args[0] as String
    val file2 = args[1] as String
    val source = File(file)
    val dest = File(file2)
    source.copyTo(dest, false)
}

val `move!` = AuroraNativeSub("move!", 1) { args ->
    // file move
    val file = args[0] as String
    val file2 = args[1] as String
    val source = File(file)
    val dest = File(file2)
    source.renameTo(dest)
}

val move = AuroraNativeSub("move", 1) { args ->
    // file move
    val file = args[0] as String
    val file2 = args[1] as String
    val source = File(file)
    val dest = File(file2)
    if (!dest.exists())
        source.renameTo(dest)
}

val `delete!` = AuroraNativeSub("delete!", 1) { args ->
    // file delete
    val file = args[0] as String
    val source = File(file)
    source.delete()
}

val new = AuroraNativeSub("new", 1) { args ->
    // file new
    val file = args[0] as String
    val source = File(file)
    source.createNewFile()
}

val `new!` = AuroraNativeSub("new!", 1) { args ->
    // file new
    val file = args[0] as String
    val source = File(file)
    if (!source.exists())
        source.createNewFile()
}

val `exists?` = AuroraNativeFn("exists?", 1) { args ->
    // file exists
    val file = args[0] as String
    val source = File(file)
    source.exists()
}

val `write!` = AuroraNativeSub("write!", 1) { args ->
    // file write
    val file = args[0] as String
    val source = File(file)
    source.writeText(args[1] as String)
}

val read = AuroraNativeFn("read", 1) { args ->
    // file read
    val file = args[0] as String
    val source = File(file)
    if (!source.exists())
        return@AuroraNativeFn false
    source.readText()
}

val ask = AuroraNativeFn("ask", 1) { args ->
    // ask user for input
    val prompt = args[0] as String
    print(prompt)
    readln()
}

val num = AuroraNativeFn("num", 1) { args ->
    // convert to number
    when (args[0]) {
        is String -> try { (args[0] as String).toDouble() }
                     catch (_: NumberFormatException) { Double.NaN }
        is Double -> args[0] as Double
        is Int -> (args[0] as Int).toDouble()
        else -> Double.NaN
    }
}

val str = AuroraNativeFn("str", 1) { args ->
    // convert to string
    args[0].toString()
}

val type_of = AuroraNativeFn("type_of", 1) { args ->
    // get type of object
    when(args[0]) {
        is String -> "str"
        is Double -> "num"
        is Int -> "num"
        is Boolean -> "bool"
        is List<*> -> "array"
        is Map<*, *> -> "map"
        is AuroraFunction, is AuroraNativeFn -> "fn"
        is AuroraSubroutine, is AuroraNativeSub -> "sub"
        else -> args[0]::class.simpleName ?: "unknown"
    }
}

val `is` = AuroraNativeFn("is?", 2) { args ->
    // check if object is of type
    when(args[0]) {
        is String -> args[1] == "str"
        is Double -> args[1] == "num"
        is Int -> args[1] == "num"
        is Boolean -> args[1] == "bool"
        is List<*> -> args[1] == "array"
        is Map<*, *> -> args[1] == "map"
        is AuroraFunction, is AuroraNativeFn -> args[1] == "fn"
        is AuroraSubroutine, is AuroraNativeSub -> args[1] == "sub"
        else -> args[0]::class.simpleName == args[1]
    }
}

val math = mutableMapOf<Any, Any>(
    "sin" to AuroraNativeFn("sin", 1) { args ->
        sin(args[0] as Double)
    },
    "cos" to AuroraNativeFn("cos", 1) { args ->
        cos(args[0] as Double)
    },
    "tan" to AuroraNativeFn("tan", 1) { args ->
        tan(args[0] as Double)
    },
    "asin" to AuroraNativeFn("asin", 1) { args ->
        asin(args[0] as Double)
    },
    "acos" to AuroraNativeFn("acos", 1) { args ->
        acos(args[0] as Double)
    },
    "atan" to AuroraNativeFn("atan", 1) { args ->
        atan(args[0] as Double)
    },
    "sinh" to AuroraNativeFn("sinh", 1) { args ->
        sinh(args[0] as Double)
    },
    "cosh" to AuroraNativeFn("cosh", 1) { args ->
        cosh(args[0] as Double)
    },
    "tanh" to AuroraNativeFn("tanh", 1) { args ->
        tanh(args[0] as Double)
    },
    "asinh" to AuroraNativeFn("asinh", 1) { args ->
        asinh(args[0] as Double)
    },
    "acosh" to AuroraNativeFn("acosh", 1) { args ->
        acosh(args[0] as Double)
    },
    "atanh" to AuroraNativeFn("atanh", 1) { args ->
        atanh(args[0] as Double)
    },
    "exp" to AuroraNativeFn("exp", 1) { args ->
        exp(args[0] as Double)
    },
    "expm1" to AuroraNativeFn("expm1", 1) { args ->
        expm1(args[0] as Double)
    },
    "ln" to AuroraNativeFn("ln", 1) { args ->
        ln(args[0] as Double)
    },
    "log" to AuroraNativeFn("log", 2) { args ->
        log(args[0] as Double, args[1] as Double)
    },
    "log10" to AuroraNativeFn("log10", 1) { args ->
        log10(args[0] as Double)
    },
    "log2" to AuroraNativeFn("log2", 1) { args ->
        log2(args[0] as Double)
    },
    "sqrt" to AuroraNativeFn("sqrt", 1) { args ->
        sqrt(args[0] as Double)
    },
    "cbrt" to AuroraNativeFn("cbrt", 1) { args ->
        cbrt(args[0] as Double)
    },
    "hypot" to AuroraNativeFn("hypot", 2) { args ->
        hypot(args[0] as Double, args[1] as Double)
    },
    "pow" to AuroraNativeFn("pow", 2) { args ->
        (args[0] as Double).pow(args[1] as Double)
    },
    "ceil" to AuroraNativeFn("ceil", 1) { args ->
        ceil(args[0] as Double)
    },
    "floor" to AuroraNativeFn("floor", 1) { args ->
        floor(args[0] as Double)
    },
    "round" to AuroraNativeFn("round", 1) { args ->
        round(args[0] as Double)
    },
    "abs" to AuroraNativeFn("abs", 1) { args ->
        abs(args[0] as Double)
    },
    "NaN" to Double.NaN,
)

val split = AuroraNativeFn("split", 2) { args ->
    // split string into array
    (args[0] as String).split(args[1] as String)
}

val replace = AuroraNativeFn("replace", 3) { args ->
    // replace string
    (args[0] as String).replace(args[1] as String, args[2] as String)
}

val substring = AuroraNativeFn("substring", 3) { args ->
    // get substring
    (args[0] as String).substring((args[1] as Number).toInt(), (args[2] as Number).toInt())
}

val clock = AuroraNativeFn("clock", 0) { args ->
    // get current time
    System.currentTimeMillis()
}

val now = AuroraNativeFn("now", 0) { args ->
    mapOf<Any, Any>(
        "year" to Calendar.getInstance().get(Calendar.YEAR),
        "month" to Calendar.getInstance().get(Calendar.MONTH),
        "day" to Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
        "hour" to Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        "minute" to Calendar.getInstance().get(Calendar.MINUTE),
        "second" to Calendar.getInstance().get(Calendar.SECOND),
    )
}

val execute = AuroraNativeFn("execute", null) { args ->
    // execute command
    val args = args.map { it as String }.toTypedArray()
    val process = ProcessBuilder(*args).start()
    mapOf<Any, Any>(
        "__output_stream" to process.outputStream,
        "__input_stream" to process.inputStream,
        "__error_stream" to process.errorStream,
        "wait_for" to AuroraNativeFn("wait_for", 0) { args ->
            process.waitFor()
        },
    )
}

val push = AuroraNativeSub("push", 2) { args ->
    // push to array
    (args[0] as MutableList<Any>).add(args[1])
}

val pop = AuroraNativeFn("pop", 1) { args ->
    // pop from array
    (args[0] as MutableList<Any>).removeAt((args[0] as MutableList<Any>).size - 1)
}

val shift = AuroraNativeFn("shift", 1) { args ->
    // shift from array
    (args[0] as MutableList<Any>).removeAt(0)
}

val unshift = AuroraNativeSub("unshift", 2) { args ->
    // unshift to array
    (args[0] as MutableList<Any>).add(0, args[1])
}

val join = AuroraNativeFn("join", 2) { args ->
    // join array
    (args[0] as List<Any>).joinToString(args[1] as String)
}

val len = AuroraNativeFn("len", 1) { args ->
    // get length of array or string
    when (args[0]) {
        is List<*> -> (args[0] as List<*>).size
        is String -> (args[0] as String).length
        else -> Double.NaN
    }
}

val map = AuroraNativeFn("map", 2) { args ->
    // map array
    if(args[1] !is AuroraNativeFn && args[1] !is AuroraFunction) {
        return@AuroraNativeFn mutableListOf<Any>()
    }
    (args[0] as List<Any>).map { (args[1] as AuroraCallable).call(listOf(it)) }
}

val `map!` = AuroraNativeSub("map!", arity = 2) { args ->
    // map array in place
    if(args[1] !is AuroraNativeFn && args[1] !is AuroraFunction) {
        return@AuroraNativeSub
    }
    for (i in (args[0] as MutableList<Any>).indices) {
        (args[0] as MutableList<Any>)[i] = (args[1] as AuroraCallable).call(listOf((args[0] as MutableList<Any>)[i]))
    }
}

val filter = AuroraNativeFn("filter", 2) { args ->
    // filter array
    if(args[1] !is AuroraNativeFn && args[1] !is AuroraFunction) {
        return@AuroraNativeFn mutableListOf<Any>()
    }
    (args[0] as List<Any>).filter { (args[1] as AuroraCallable).call(listOf(it)) as Boolean }
}

val `filter!` = AuroraNativeSub("filter!", arity = 2) { args ->
    // filter array in place
    if(args[1] !is AuroraNativeFn && args[1] !is AuroraFunction) {
        return@AuroraNativeSub
    }
    for (i in (args[0] as MutableList<Any>).indices) {
        if(!((args[1] as AuroraCallable).call(listOf((args[0] as MutableList<Any>)[i])) as Boolean)) {
            (args[0] as MutableList<Any>).removeAt(i)
        }
    }
}

val reduce = AuroraNativeFn("reduce", 3) { args ->
    // reduce array
    if(args[2] !is AuroraNativeFn && args[2] !is AuroraFunction) {
        return@AuroraNativeFn mutableListOf<Any>()
    }
    (args[0] as List<Any>).reduce { acc, any -> (args[2] as AuroraCallable).call(listOf(acc, any)) }
}

val `reduce!` = AuroraNativeSub("reduce!", arity = 3) { args ->
    // reduce array in place
    if(args[2] !is AuroraNativeFn && args[2] !is AuroraFunction) {
        return@AuroraNativeSub
    }
    for (i in (args[0] as MutableList<Any>).indices) {
        (args[0] as MutableList<Any>)[i] = (args[2] as AuroraCallable).call(listOf((args[0] as MutableList<Any>)[i], (args[0] as MutableList<Any>)[i + 1]))
    }
}

val upTo = AuroraNativeFn("upTo", 2) { args ->
    // up to
    val list = mutableListOf<Any>()
    for (i in (args[0] as Number).toInt() ..(args[1] as Number).toInt()) {
        list.add(i)
    }
    list
}

val downTo = AuroraNativeFn("downTo", 2) { args ->
    // down to
    val list = mutableListOf<Any>()
    for (i in (args[0] as Number).toInt() downTo (args[1] as Number).toInt()) {
        list.add(i)
    }
    list
}

val times = AuroraNativeFn("times", 1) { args ->
    return@AuroraNativeFn object : AuroraCallable {
        override fun arity(): Int {
            return 1
        }

        override fun call(arguments: List<Any>): Any {
            for (i in 0 until (args[0] as Number).toInt()) {
                (arguments[0] as AuroraCallable).call(listOf(i))
            }
            return Unit
        }

        override fun toString(): String {
            return "[anonymous sub]"
        }
    }
}
fun load() {
    loader.insertValue("print", print)
    loader.insertValue("copy!", `copy!`)
    loader.insertValue("copy", copy)
    loader.insertValue("move!", `move!`)
    loader.insertValue("move", move)
    loader.insertValue("delete!", `delete!`)
    loader.insertValue("new", new)
    loader.insertValue("new!", `new!`)
    loader.insertValue("exists?", `exists?`)
    loader.insertValue("write!", `write!`)
    loader.insertValue("read", read)
    loader.insertValue("ask", ask)
    loader.insertValue("num", num)
    loader.insertValue("str", str)
    loader.insertValue("type_of", type_of)
    loader.insertValue("is?", `is`)
    loader.insertValue("math", math)
    loader.insertValue("split", split)
    loader.insertValue("replace", replace)
    loader.insertValue("substring", substring)
    loader.insertValue("clock", clock)
    loader.insertValue("now", now)
    loader.insertValue("execute", execute)
    loader.insertValue("push", push)
    loader.insertValue("pop", pop)
    loader.insertValue("shift", shift)
    loader.insertValue("unshift", unshift)
    loader.insertValue("join", join)
    loader.insertValue("len", len)
    loader.insertValue("map", map)
    loader.insertValue("map!", `map!`)
    loader.insertValue("filter", filter)
    loader.insertValue("filter!", `filter!`)
    loader.insertValue("reduce", reduce)
    loader.insertValue("reduce!", `reduce!`)
    loader.insertValue("upTo", upTo)
    loader.insertValue("downTo", downTo)
    loader.insertValue("times", times)
}