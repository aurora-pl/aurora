import Token.*

class Parser(val tokens: List<Token>) {
    private var current = 0
    private var line = 1
    private var column = 1

    fun parse(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!isAtEnd()) {
            statements.add(statement())
        }
        return statements
    }

    private fun peek() = tokens[current]
    private fun isAtEnd() = peek() is Eof

    private fun advance(): Token {
        if (!isAtEnd()) current++
        line = tokens[current - 1].line
        column = tokens[current - 1].column
        return tokens[current - 1]
    }

    private inline fun <reified T> match(): Boolean {
        if (check<T>()) {
            advance()
            return true
        }
        return false
    }

    private inline fun <reified T> eat(): T =
        if (check<T>())
            advance() as T
        else
            throw Exception("Expected ${T::class.java.simpleName} but got ${peek().javaClass.simpleName} instead.")

    private inline fun <reified T> check() = peek() is T

    private fun synchronize() {
        advance()
        while (!isAtEnd()) {
            if (peek() is Newline) return
            when (peek()) {
                is Fn -> return
                is Token.Sub -> return
                is Token.For -> return
                is Token.If -> return
                is Token.While -> return
                is Token.Return -> return
                is Token.Break -> return
                is Token.Continue -> return
                is Identifier -> return
                else -> advance()
            }
        }
    }

    private fun statement(): Stmt {
        return when (peek()) {
            is Token.If -> ifStatement()
            is Token.While -> whileStatement()
            is Token.For -> forStatement()
            is Token.Return -> returnStatement()
            is Token.Break -> breakStatement()
            is Token.Continue -> continueStatement()
            is Token.Fn -> functionStatement()
            is Token.Sub -> subStatement()
            is Identifier -> {
                val identifier = eat<Identifier>()
                if (match<Colon>()) {
                    var list: Expr = Variable(identifier)
                    var index = expression()
                    while(check<Colon>()) {
                        list = Index(list, index)
                        index = expression()
                    }
                    match<Assign>()
                    val value = expression()
                    AssignIndex(list, index, value, identifier.line, identifier.column)
                } else if(match<Assign>()) {
                    val value = expression()
                    Assign(identifier, value, identifier.line, identifier.column)
                } else {
                    val args = mutableListOf<Expr>()
                    if(!check<Newline>()) {
                        args.add(expression())
                        while(match<Comma>()) {
                            args.add(expression())
                        }
                    }
                    CallStmt(Variable(identifier), args, identifier.line, identifier.column)
                }
            }
            else -> throw Exception("Unexpected token ${peek().javaClass.simpleName} at line $line, column $column")
        }.also {
            if (!match<Eof>())
                eat<Newline>()
        }
    }

    private fun primary(): Expr {
        return when (peek()) {
            is Token.Boolean -> {
                val token = eat<Token.Boolean>()
                Literal(token)
            }
            is Token.Number -> {
                val token = eat<Token.Number>()
                Literal(token)
            }
            is StringLiteral -> {
                val token = eat<StringLiteral>()
                Literal(token)
            }
            is LParen -> {
                eat<LParen>()
                val expr = expression()
                eat<RParen>()
                expr
            }
            is LBrace -> {
                eat<LBrace>()
                val pairs = mutableListOf<Pair<Expr, Expr>>()
                if(!check<RBrace>()) {
                    pairs.add(Pair(expression(), expression()))
                    while(match<Comma>() || match<Newline>()) {
                        pairs.add(Pair(expression().also { eat<Colon>() }, expression()))
                    }
                }
                eat<RBrace>()
                MapLiteral(pairs)
            }
            is Identifier -> {
                val identifier = eat<Identifier>()
                Variable(identifier)
            }
            else -> throw Exception("Unexpected token ${peek().javaClass.simpleName} at line $line, column $column")
        }
    }

    private fun unary(): Expr {
        if (check<Minus>()) {
            val operator = advance()
            val right = unary()
            return Unary(operator, right)
        }
        return primary()
    }

    private fun multiplication(): Expr {
        var expr = unary()
        while (check<Star>() || check<Slash>()) {
            val operator = advance()
            val right = unary()
            expr = Binary(expr, operator, right)
        }
        return expr
    }

    private fun addition(): Expr {
        var expr = multiplication()
        while (check<Plus>() || check<Minus>()) {
            val operator = advance()
            val right = multiplication()
            expr = Binary(expr, operator, right)
        }
        return expr
    }

    private fun comparison(): Expr {
        var expr = addition()
        while (check<Greater>() || check<GreaterEq>() || check<Less>() || check<LessEq>()) {
            val operator = advance()
            val right = addition()
            expr = Binary(expr, operator, right)
        }
        return expr
    }

    private fun equality(): Expr {
        var expr = comparison()
        while (check<Eq>() || check<NotEq>()) {
            val operator = advance()
            val right = comparison()
            expr = Binary(expr, operator, right)
        }
        return expr
    }

    private fun or(): Expr {
        var expr = equality()
        while (match<Or>()) {
            val operator = advance()
            val right = equality()
            expr = Binary(expr, operator, right)
        }
        return expr
    }

    private fun and(): Expr {
        var expr = or()
        while (match<And>()) {
            val operator = advance()
            val right = or()
            expr = Binary(expr, operator, right)
        }
        return expr
    }

    private fun expression(): Expr {
        return and()
    }

    private fun ifStatement(): Stmt {
        val ifToken = advance()
        val expr = expression()
        return if(match<Newline>()) {
            val statements = mutableListOf<Stmt>()
            while (!check<End>() && !check<Else>() && !isAtEnd()) {
                statements.add(statement())
            }
            if (match<Else>()) {
                if(match<Newline>()) {
                    val elseStatements = mutableListOf<Stmt>()
                    while (!check<End>() && !isAtEnd()) {
                        elseStatements.add(statement())
                    }
                    eat<End>()
                    If(expr, Block(statements), Block(elseStatements), ifToken.line, ifToken.column)
                } else {
                    val elseBranch = statement()
                    If(expr, Block(statements), elseBranch, ifToken.line, ifToken.column)
                }
            } else {
                eat<End>()
                If(expr, Block(statements), null, ifToken.line, ifToken.column)
            }
        } else {
            val thenBranch = statement()
            if (match<Else>()) {
                return if (match<Newline>()) {
                    val elseStatements = mutableListOf<Stmt>()
                    while (!check<End>() && !isAtEnd()) {
                        elseStatements.add(statement())
                    }
                    eat<End>()
                    If(expr, thenBranch, Block(elseStatements), ifToken.line, ifToken.column)
                } else {
                    val elseBranch = statement()
                    If(expr, thenBranch, elseBranch, ifToken.line, ifToken.column)
                }
            } else If(expr, thenBranch, null, ifToken.line, ifToken.column)
        }
    }

    private fun whileStatement(): Stmt {
        val whileToken = advance()
        val expr = expression()
        return if(match<Newline>()) {
            val statements = mutableListOf<Stmt>()
            while (!check<End>() && !isAtEnd()) {
                statements.add(statement())
            }
            eat<End>()
            While(expr, Block(statements), whileToken.line, whileToken.column)
        } else {
            val body = statement()
            While(expr, body, whileToken.line, whileToken.column)
        }
    }

    private fun forStatement(): Stmt {
        val forToken = advance()
        val identifier = eat<Identifier>()
        eat<Comma>()
        val expr = expression()
        return if(match<Newline>()) {
            val statements = mutableListOf<Stmt>()
            while (!check<End>() && !isAtEnd()) {
                statements.add(statement())
            }
            eat<End>()
            For(identifier, expr, Block(statements), forToken.line, forToken.column)
        } else {
            val body = statement()
            For(identifier, expr, body, forToken.line, forToken.column)
        }
    }

    private fun returnStatement(): Stmt {
        val returnToken = advance()
        val value = if(!check<Newline>()) expression() else null
        return Return(value, returnToken.line, returnToken.column)
    }

    private fun breakStatement(): Stmt {
        val breakToken = advance()
        return Break(breakToken)
    }

    private fun continueStatement(): Stmt {
        val continueToken = advance()
        return Continue(continueToken)
    }

    private fun functionStatement(): Stmt {
        val fnToken = advance()
        val name = eat<Identifier>()
        val args = mutableListOf<Identifier>()
        if(!check<Newline>())
            do {
                args.add(eat())
            } while (match<Comma>())
        return if(match<Newline>()) {
            val statements = mutableListOf<Stmt>()
            while (!check<End>() && !isAtEnd()) {
                statements.add(statement())
            }
            eat<End>()
            Func(name, args, Block(statements), fnToken.line, fnToken.column)
        } else {
            match<Arrow>()
            val body = expression()
            Func(name, args, Return(body, body.line, body.column), fnToken.line, fnToken.column)
        }
    }

    private fun subStatement(): Stmt {
        val subToken = advance()
        val name = eat<Identifier>()
        val args = mutableListOf<Identifier>()
        do {
            args.add(eat())
        } while (match<Comma>())
        return if(match<Newline>()) {
            val statements = mutableListOf<Stmt>()
            while (!check<End>() && !isAtEnd()) {
                statements.add(statement())
            }
            eat<End>()
            Sub(name, args, Block(statements), subToken.line, subToken.column)
        } else {
            match<Arrow>()
            val body = statement()
            Sub(name, args, body, subToken.line, subToken.column)
        }
    }
}