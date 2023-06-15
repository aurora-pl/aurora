import Token.*

object KillStage : Throwable()

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
        else {
            Error(peek().line, peek().column, "expected ${T::class.java.simpleName} but got ${peek().javaClass.simpleName} instead", source).print()
            throw KillStage
        }

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
        while(match<Newline>());
        return when (peek()) {
            is Token.If, is Token.Unless -> ifStatement()
            is Token.While, is Token.Until -> whileStatement()
            is Token.For -> forStatement()
            is Token.Return -> returnStatement()
            is Token.Break -> breakStatement()
            is Token.Continue -> continueStatement()
            is Token.Fn -> functionStatement()
            is Token.Sub -> subStatement()
            is Token.Select -> selectStatement()
            is Token.Switch -> switchStatement()
            is Identifier -> {
                val identifier = eat<Identifier>()
                when (peek()) {
                    is Colon -> {
                        var list: Expr = Variable(identifier)
                        var index: Expr? = null
                        var i = 0
                        while(check<Colon>()) {
                            eat<Colon>()
                            if(i > 0) {
                                list = Index(list, index!!)
                            }
                            index = if(match<Colon>()) {
                                Literal(eat<Identifier>().run { StringLiteral().new(literal, line, column) })
                            } else {
                                primary()
                            }
                            i++
                        }
                        eat<Token.Assign>()
                        val value: Expr = expression()
                        AssignIndex(list, index!!, value, index.line, index.column)
                    }
                    is Token.Assign -> {
                        eat<Token.Assign>()
                        val value = expression()
                        Assign(identifier, value, identifier.line, identifier.column)
                    }
                    else -> {
                        // put the identifier back
                        current--
                        callStmt()
                    }
                }
            }
            else -> {
                callStmt()
            }
        }.also {
            if (!match<Eof>())
                eat<Newline>()
        }
    }
    private fun callStmt(): Stmt {
        val expr = expression()
        val args = mutableListOf<Expr>()
        if(!check<Newline>()) {
            args.add(expression())
            while(match<Comma>()) {
                args.add(expression())
            }
        }
        return CallStmt(expr, args, expr.line, expr.column)
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
            is Fmt -> {
                val token = eat<Fmt>()
                if (!check<StringLiteral>()) {
                    Error(token.line, token.column, "expected string literal but got ${peek().javaClass.simpleName} instead", source).print()
                    throw KillStage
                }
                val string = eat<StringLiteral>()
                // turn "...{expression}..." into "..." + expression + "...", ignoring escaped braces, but keeping the braces around the expression
                val parts = (string.literal as String).split(Regex("(?<!\\\\)((?=\\{)|(?<=\\}))"))
                var left: Expr = Literal(StringLiteral().new("", string.line, string.column))
                for (part in parts) {
                    if (part.startsWith("{") && part.endsWith("}")) {
                        val lexer = Lexer(part.substring(1, part.length - 1))
                        val tokens = lexer.scanTokens().map { it.also { it.line = string.line; it.column = string.column + (string.literal as String).indexOf(part) + it.column} }
                        val parser = Parser(tokens)
                        val expr = parser.expression()
                        left = Binary(left, Plus().new(string.line, string.column + (string.literal as String).indexOf(part)), expr)
                    } else {
                        left = Binary(left, Plus().new(string.line, string.column + (string.literal as String).indexOf(part)), Literal(StringLiteral().new(part.replace("\\{", "{"), string.line, string.column + (string.literal as String).indexOf(part))))
                    }
                }
                left
            }
            is LParen -> {
                eat<LParen>()
                val expr = expression()
                eat<RParen>()
                expr
            }
            is LBrace -> {
                val (line, col) = eat<LBrace>().run { Pair(line, column) }
                val pairs = mutableListOf<Pair<Expr, Expr>>()
                if(!check<RBrace>()) {
                    pairs.add(Pair(primary().also { eat<Arrow>() }, expression()))
                    while(match<Comma>() || match<Newline>()) {
                        pairs.add(Pair(primary().also { eat<Arrow>() }, expression()))
                    }
                }
                eat<RBrace>()
                MapLiteral(pairs, line, col)
            }
            is LBracket -> {
                val (line, col) = eat<LBracket>().run { Pair(line, column) }
                val exprs = mutableListOf<Expr>()
                if(!check<RBracket>()) {
                    exprs.add(expression())
                    while(match<Comma>()) {
                        exprs.add(expression())
                    }
                }
                eat<RBracket>()
                ListLiteral(exprs, line, col)
            }
            is Identifier -> {
                val identifier = eat<Identifier>()
                Variable(identifier)
            }
            is Token.Lambda -> {
                val lambda = eat<Token.Lambda>()
                val args = mutableListOf<Identifier>()
                if(!check<Arrow>() && !check<Newline>()) {
                    args.add(eat<Identifier>())
                    while(match<Comma>()) {
                        args.add(eat<Identifier>())
                    }
                }
                if (match<Arrow>()) {
                    val body = expression()
                    Lambda(args, Return(body, body.line, body.column), lambda.line, lambda.column)
                } else {
                    eat<Newline>()
                    val body = mutableListOf<Stmt>()
                    while (!check<End>()) {
                        body.add(statement())
                    }
                    eat<End>()
                    Lambda(args, Block(body), lambda.line, lambda.column)
                }
            }
            is Do -> {
                val doToken = eat<Do>()
                val args = mutableListOf<Identifier>()
                if(!check<Arrow>() && !check<Newline>()) {
                    args.add(eat<Identifier>())
                    while(match<Comma>()) {
                        args.add(eat<Identifier>())
                    }
                }
                if (match<Arrow>()) {
                    val body = statement()
                    DoBlock(args, body, doToken.line, doToken.column)
                } else {
                    eat<Newline>()
                    val body = mutableListOf<Stmt>()
                    while (!check<End>()) {
                        body.add(statement())
                    }
                    eat<End>()
                    DoBlock(args, Block(body), doToken.line, doToken.column)
                }

            }
            else -> {
                Error(line, column, "expected expression but got ${peek().javaClass.simpleName} instead", source).print()
                throw KillStage
            }
        }
    }

    private fun call(): Expr {
        var expr = primary()
        var line = 0
        var column = 0
        while (peek().run {
            if (this is LParen || this is Colon) {
                line = this.line
                column = this.column
                true
            } else {
                false
            }
        }) {
            when(peek()) {
                is LParen -> {
                    val args = mutableListOf<Expr>()
                    eat<LParen>()
                    if(!check<RParen>()) {
                        args.add(expression())
                        while(match<Comma>()) {
                            args.add(expression())
                        }
                    }
                    eat<RParen>()
                    expr = Call(expr, args, line, column)
                }
                is Colon -> {
                    eat<Colon>()
                    expr = if(match<Colon>()) {
                        val list: Expr = expr
                        val index = eat<Identifier>()
                        Index(list, Literal(index))
                    } else {
                        val list: Expr = expr
                        val index = primary()
                        Index(list, index)
                    }
                }
                else -> {
                    Error(line, column, "expected '(' or ':' but got ${peek().javaClass.simpleName} instead", source).print()
                    throw KillStage
                }
            }
        }
        return expr
    }

    private fun unary(): Expr {
        if (check<Minus>()) {
            val operator = advance()
            val right = unary()
            return Unary(operator, right)
        }
        return call()
    }

    private fun not(): Expr {
        if (check<Not>()) {
            val operator = advance()
            val right = not()
            return Unary(operator, right)
        }
        return unary()
    }

    private fun multiplication(): Expr {
        var expr = not()
        while (check<Star>() || check<Slash>() || check<Dot>() || check<Arrow>()) {
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
        val expr = if (ifToken is Token.If) {
            expression()
        } else {
            Unary(Token.Not().new(ifToken.line, ifToken.column), expression())
        }
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
        val expr = if (whileToken is Token.While) {
            expression()
        } else {
            Unary(Token.Not().new(whileToken.line, whileToken.column), expression())
        }
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
        if(!check<Newline>()) {
            do {
                args.add(eat())
            } while (match<Comma>())
        }
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

    private fun selectStatement(): Stmt {
        val selectToken = advance()
        eat<Newline>()
        val cases = mutableListOf<Pair<Expr, Stmt>>()
        while (!check<End>() && !isAtEnd() && !check<Else>()) {
            eat<Case>()
            val expr = expression()
            if (match<Arrow>()) {
                val body = statement()
                cases.add(Pair(expr, body))
            } else {
                eat<Newline>()
                val statements = mutableListOf<Stmt>()
                while (!check<End>() && !check<Case>() && !check<Else>() && !isAtEnd()) {
                    statements.add(statement())
                }
                cases.add(Pair(expr, Block(statements)))
            }
        }
        val elseBranch = if (match<Else>()) {
            if (match<Arrow>()) {
                statement()
            } else {
                eat<Newline>()
                val statements = mutableListOf<Stmt>()
                while (!check<End>() && !isAtEnd()) {
                    statements.add(statement())
                }
                Block(statements)
            }
        } else null
        eat<End>()
        return Select(cases, elseBranch, selectToken.line, selectToken.column)
    }

    private fun switchStatement(): Stmt {
        val switchToken = advance()
        val expr = expression()
        eat<Newline>()
        val cases = mutableListOf<Pair<Expr, Stmt>>()
        while (!check<End>() && !isAtEnd() && !check<Else>()) {
            eat<Case>()
            val expr = expression()
            if (match<Arrow>()) {
                val body = statement()
                cases.add(Pair(expr, body))
            } else {
                eat<Newline>()
                val statements = mutableListOf<Stmt>()
                while (!check<End>() && !check<Case>() && !check<Else>() && !isAtEnd()) {
                    statements.add(statement())
                }
                cases.add(Pair(expr, Block(statements)))
            }
        }
        val elseBranch = if (match<Else>()) {
            if (match<Arrow>()) {
                statement()
            } else {
                eat<Newline>()
                val statements = mutableListOf<Stmt>()
                while (!check<End>() && !isAtEnd()) {
                    statements.add(statement())
                }
                Block(statements)
            }
        } else null
        eat<End>()
        return Switch(expr, cases, elseBranch, switchToken.line, switchToken.column)
    }
}