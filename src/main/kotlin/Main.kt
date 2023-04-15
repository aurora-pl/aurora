fun main(args: Array<String>) {
    val code = """
        fn test
            print "Hello, world!", 1.2, foobar
            if 1 == 2
                print "1 == 2"
            else
                print "1 != 2"
            end
        end
    """.trimIndent()
    val lexer = Lexer(code)
    val tokens = lexer.scanTokens()
    for (token in tokens) {
        if (token is Token.Newline) {
            println("Newline")
            continue
        }
        println(token)
    }
    val parser = Parser(tokens)
    println(parser.parse())
}