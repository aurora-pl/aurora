import java.lang.Character.isAlphabetic
import java.lang.Character.isDigit

sealed class Token(
    var literal: Any? = null,
    var lexeme: String = "",
    var line: Int = 0,
    var column: Int = 0
) {
    // Keywords
    class If : Token(lexeme = "if")
    class Unless : Token(lexeme = "unless")
    class Else : Token(lexeme = "else")
    class While : Token(lexeme = "while")
    class Until : Token(lexeme = "until")
    class For : Token(lexeme = "for")
    class Fn : Token(lexeme = "fn")
    class Sub : Token(lexeme = "sub")
    class Return : Token(lexeme = "return")
    class Break : Token(lexeme = "break")
    class Continue : Token(lexeme = "continue")
    class End : Token(lexeme = "end")
    class Select : Token(lexeme = "select")
    class Switch : Token(lexeme = "switch")
    class Case : Token(lexeme = "case")
    class Do : Token(lexeme = "do")
    class Lambda : Token(lexeme = "lambda")

    // Operators
    class Plus : Token(lexeme = "+")
    class Minus : Token(lexeme = "-")
    class Star : Token(lexeme = "*")
    class Slash : Token(lexeme = "/")
    class Mod : Token(lexeme = "%")
    class Less : Token(lexeme = "<")
    class LessEq : Token(lexeme = "<=")
    class Greater : Token(lexeme = ">")
    class GreaterEq : Token(lexeme = ">=")
    class Eq : Token(lexeme = "==")
    class NotEq : Token(lexeme = "!=")
    class And : Token(lexeme = "and")
    class Or : Token(lexeme = "or")
    class Not : Token(lexeme = "not")
    class Dot : Token(lexeme = ".")

    // Punctuation
    class LParen : Token(lexeme = "(")
    class RParen : Token(lexeme = ")")
    class LBrace : Token(lexeme = "{")
    class RBrace : Token(lexeme = "}")
    class LBracket : Token(lexeme = "[")
    class RBracket : Token(lexeme = "]")
    class Comma : Token(lexeme = ",")
    class Colon : Token(lexeme = ":")
    class Newline : Token(lexeme = "\n")
    class Assign : Token(lexeme = "=")
    class PlusAssign : Token(lexeme = "+=")
    class MinusAssign : Token(lexeme = "-=")
    class StarAssign : Token(lexeme = "*=")
    class SlashAssign : Token(lexeme = "/=")
    class ModAssign : Token(lexeme = "%=")
    class Arrow : Token(lexeme = "->")

    // Literals
    class Number : Token()
    class Boolean : Token()
    class StringLiteral : Token()
    class Identifier : Token()

    // Special
    class Eof : Token()

    fun new(literal: Any? = null, line: Int = 0, column: Int = 0) =
        this.apply {
            this.literal = literal
            this.line = line
            this.column = column
        }

    fun new(literal: Any? = null, lexeme: String, line: Int = 0, column: Int = 0) =
        this.apply {
            this.literal = literal
            this.lexeme = lexeme
            this.line = line
            this.column = column
        }

    override fun toString(): String {
        return "${this::class.qualifiedName} '$lexeme' ${if (literal != null) literal else ""} $line:$column"
    }
}

class Lexer(private val input: String) {
    private var start = 0
    private var current = 0

    private var line = 1
    private var column = 1

    fun scanTokens(): List<Token> {
        val tokens = mutableListOf<Token>()
        while (!isAtEnd()) {
            start = current
            tokens.add(scanToken())
        }
        tokens.add(Token.Eof().new(line = line, column = column))
        return tokens
    }

    private fun isAtEnd() = current >= input.length

    private fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (input[current] != expected) return false
        current++
        return true
    }

    private fun peek(): Char {
        if (isAtEnd()) return '\u0000'
        return input[current]
    }

    private fun advance(): Char {
        current++
        column++
        return input[current - 1]
    }

    private fun scanToken(): Token {
        return when (val c = advance()) {
            '(' -> Token.LParen().new(line = line, column = column)
            ')' -> Token.RParen().new(line = line, column = column)
            '{' -> Token.LBrace().new(line = line, column = column)
            '}' -> Token.RBrace().new(line = line, column = column)
            '[' -> Token.LBracket().new(line = line, column = column)
            ']' -> Token.RBracket().new(line = line, column = column)
            ',' -> Token.Comma().new(line = line, column = column)
            '-' ->
                if (match('='))
                    Token.MinusAssign().new(line = line, column = column)
                else if (match('>'))
                    Token.Arrow().new(line = line, column = column)
                else Token.Minus().new(
                    line = line,
                    column = column
                )

            '+' ->
                if (match('='))
                    Token.PlusAssign().new(line = line, column = column)
                else Token.Plus().new(
                    line = line,
                    column = column
                )

            '*' ->
                if (match('='))
                    Token.StarAssign().new(line = line, column = column)
                else Token.Star().new(
                    line = line,
                    column = column
                )

            '.' -> Token.Dot().new(line = line, column = column)

            '!' ->
                if (match('='))
                    Token.NotEq().new(line = line, column = column)
                else {
                    Error(line, column, "unexpected character: $c", source).print()
                    throw KillStage
                }

            '=' ->
                if (match('='))
                    Token.Eq().new(line = line, column = column)
                else Token.Assign().new(
                    line = line,
                    column = column
                )

            '<' ->
                if (match('='))
                    Token.LessEq().new(line = line, column = column)
                else Token.Less().new(
                    line = line,
                    column = column
                )

            '>' ->
                if (match('='))
                    Token.GreaterEq().new(line = line, column = column)
                else Token.Greater().new(
                    line = line,
                    column = column
                )

            '#' -> {
                while (peek() != '\n' && !isAtEnd())
                    advance()
                scanToken()
            }

            '/' ->
                if (match('='))
                    Token.SlashAssign().new(line = line, column = column)
                else Token.Slash().new(
                    line = line,
                    column = column
                )

            '%' ->
                if (match('='))
                    Token.ModAssign().new(line = line, column = column)
                else Token.Mod().new(
                    line = line,
                    column = column
                )

            ':' -> Token.Colon().new(line = line, column = column)

            '\n' -> {
                line++
                column = 1
                Token.Newline().new(line = line, column = column)
            }

            ' ', '\r', '\t' -> scanToken()

            '"' -> string('"')
            '`' -> string('`')
            '\'' -> {
                // label
                val column = column
                while (isAlphabetic(peek().code) || isDigit(peek()) || peek() == '_' || peek() == '!' || peek() == '?')
                    advance()
                Token.StringLiteral().new(
                    literal = input.substring(start, current).trim().substring(1),
                    lexeme = input.substring(start, current).trim(),
                    line = line,
                    column = column
                )
            }

            else -> {
                if (isDigit(c)) return number()
                if (isAlphabetic(c.code)) return identifier()
                Error(line, column, "unexpected character: $c", source).print()
                throw KillStage
            }
        }
    }

    private fun peekNext(): Char {
        if (current + 1 >= input.length) return '\u0000'
        return input[current + 1]
    }

    private fun number(): Token {
        while (isDigit(peek()))
            advance()
        if (peek() == '.' && isDigit(peekNext())) {
            advance()
            while (isDigit(peek()))
                advance()
        }
        return Token.Number().new(
            literal = input.substring(start, current).trim().toDouble(),
            lexeme = input.substring(start, current).trim(),
            line = line,
            column = column
        )
    }

    private fun identifier(): Token {
        val column = column
        while (isAlphabetic(peek().code) || isDigit(peek()) || peek() == '_' || peek() == '!' || peek() == '?')
            advance()
        return when (val text = input.substring(start, current).trim()) {
            "if" -> Token.If().new(line = line, column = column)
            "unless" -> Token.Unless().new(line = line, column = column)
            "else" -> Token.Else().new(line = line, column = column)
            "while" -> Token.While().new(line = line, column = column)
            "until" -> Token.Until().new(line = line, column = column)
            "for" -> Token.For().new(line = line, column = column)
            "fn" -> Token.Fn().new(line = line, column = column)
            "sub" -> Token.Sub().new(line = line, column = column)
            "return" -> Token.Return().new(line = line, column = column)
            "break" -> Token.Break().new(line = line, column = column)
            "continue" -> Token.Continue().new(line = line, column = column)
            "true" -> Token.Boolean().new(true, line = line, column = column)
            "false" -> Token.Boolean().new(false, line = line, column = column)
            "and" -> Token.And().new(line = line, column = column)
            "or" -> Token.Or().new(line = line, column = column)
            "not" -> Token.Not().new(line = line, column = column)
            "end" -> Token.End().new(line = line, column = column)
            "select" -> Token.Select().new(line = line, column = column)
            "switch" -> Token.Switch().new(line = line, column = column)
            "case" -> Token.Case().new(line = line, column = column)
            "do" -> Token.Do().new(line = line, column = column)
            "lambda" -> Token.Lambda().new(line = line, column = column)
            else -> Token.Identifier().new(lexeme = text, literal = text, line = line, column = column)
        }
    }

    private fun string(delim: Char): Token {
        while (peek() != delim && !isAtEnd()) {
            if (peek() == '\n') {
                line++
                column = 1
            }
            advance()
        }
        if (isAtEnd()) {
            Error(line, column, "unterminated string", source)
            throw KillStage
        }
        advance()
        return Token.StringLiteral().new(
            lexeme = input.substring(start, current).trim(),
            literal = input.substring(start, current).trim().run {
                substring(1, length - 1)
            },
            line = line,
            column = column
        )
    }
}