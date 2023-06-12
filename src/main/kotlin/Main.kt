import java.io.File
import java.util.*
import kotlin.collections.HashMap
import kotlin.system.exitProcess

val compiler: LambdaCompiler = LambdaCompiler()
val loader: LibraryLoader = LibraryLoader(compiler)
var source: String = ""

fun repl() {
    val scanner = Scanner(System.`in`)
    load()
    while(true) {
        try {
            print("aurora ->\n\t. ")
            var buf = scanner.nextLine() + "\n"
            print("\t. ")
            var line = scanner.nextLine() + "\n"
            while(line != "\n") {
                buf += line
                print("\t. ")
                line = scanner.nextLine() + "\n"
            }
            source = buf
            val lexer = Lexer(buf)
            val tokens = lexer.scanTokens()
            val parser = Parser(tokens)
            val program = parser.parse().map { it.accept(compiler) }
            val loadedLibrary = Env[0]
            Env = Array(compiler.biggestScope) { HashMap() }
            Env[0] = loadedLibrary
            for(stmt in program) {
                stmt()
            }
        } catch (_: KillStage) {
            continue
        }
    }
}
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        repl()
        return
    }
    val file = args[0]
    val code = File(file).readText()
    source = code
    try {
        val lexer = Lexer(code)
        val tokens = lexer.scanTokens()
        val parser = Parser(tokens)
        load()
        val program = parser.parse().map { it.accept(compiler) }
        val loadedLibrary = Env[0]
        Env = Array(compiler.biggestScope) { HashMap() }
        Env[0] = loadedLibrary
        for(stmt in program) {
            stmt()
        }
    } catch (_: KillStage) {
        exitProcess(1)
    }
}