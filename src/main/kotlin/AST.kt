abstract class Expr(open val line: Int, open val column: Int) {
    interface Visitor<T> {
        fun visit(expr: Binary): T;
        fun visit(expr: Unary): T;
        fun visit(expr: Literal): T;
        fun visit(expr: Variable): T;
        fun visit(expr: ListLiteral): T;
        fun visit(expr: Index): T;
        fun visit(expr: MapLiteral): T;
        fun visit(expr: Call): T;
        fun visit(expr: Lambda): T;
        fun visit(expr: DoBlock): T;
    }

    abstract fun <T> accept(visitor: Visitor<T>): T;
}

abstract class Stmt(open val line: Int, open val column: Int) {
    interface Visitor<T> {
        fun visit(stmt: If): T;
        fun visit(stmt: While): T;
        fun visit(stmt: For): T;
        fun visit(stmt: Assign): T;
        fun visit(stmt: Func): T;
        fun visit(stmt: Sub): T;
        fun visit(stmt: Return): T;
        fun visit(stmt: Break): T;
        fun visit(stmt: Continue): T;
        fun visit(stmt: AssignIndex): T;
        fun visit(stmt: CallStmt): T;
        fun visit(stmt: Block): T;

        fun visit(stmt: Switch): T;
        fun visit(stmt: Select): T;
    }

    abstract fun <T> accept(visitor: Visitor<T>): T;
}

data class Binary(val left: Expr, val operator: Token, val right: Expr)
    : Expr(operator.line, operator.column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class Unary(val operator: Token, val right: Expr)
    : Expr(operator.line, operator.column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class Literal(val value: Token)
    : Expr(value.line, value.column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class Variable(val name: Token)
    : Expr(name.line, name.column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class ListLiteral(val elements: List<Expr>)
    : Expr(elements.first().line, elements.first().column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class DoBlock(val args: List<Token>, val body: Stmt, override val line: Int, override val column: Int)
    : Expr(line, column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class Lambda(val args: List<Token>, val body: Stmt, override val line: Int, override val column: Int)
    : Expr(line, column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class Index(val list: Expr, val index: Expr)
    : Expr(list.line, list.column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class MapLiteral(val pairs: List<Pair<Expr, Expr>>)
    : Expr(pairs.first().first.line, pairs.first().first.column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class Call(val callee: Expr, val arguments: List<Expr>, override val line: Int, override val column: Int)
    : Expr(line, column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class If(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?, override val line: Int, override val column: Int)
    : Stmt(line, column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class While(val condition: Expr, val body: Stmt, override val line: Int, override val column: Int)
    : Stmt(line, column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class For(val variable: Token, val iterable: Expr, val body: Stmt, override val line: Int, override val column: Int)
    : Stmt(line, column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class Assign(val name: Token, val value: Expr, override val line: Int, override val column: Int)
    : Stmt(line, column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class Func(val name: Token, val params: List<Token>, val body: Stmt, override val line: Int, override val column: Int)
    : Stmt(line, column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class Sub(val name: Token, val params: List<Token>, val body: Stmt, override val line: Int, override val column: Int)
    : Stmt(line, column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class Return(val value: Expr?, override val line: Int, override val column: Int)
    : Stmt(line, column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class Break(val keyword: Token)
    : Stmt(keyword.line, keyword.column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class Continue(val keyword: Token)
    : Stmt(keyword.line, keyword.column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class AssignIndex(val list: Expr, val index: Expr, val value: Expr, override val line: Int, override val column: Int)
    : Stmt(line, column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class CallStmt(val callee: Expr, val arguments: List<Expr>, override val line: Int, override val column: Int)
    : Stmt(line, column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class Block(val statements: List<Stmt>)
    : Stmt(statements.first().line, statements.first().column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class Switch(val value: Expr, val cases: List<Pair<Expr, Stmt>>, val default: Stmt?, override val line: Int, override val column: Int)
    : Stmt(line, column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}

data class Select(val cases: List<Pair<Expr, Stmt>>, val default: Stmt?, override val line: Int, override val column: Int)
    : Stmt(line, column) {
    override fun <T> accept(visitor: Visitor<T>): T =
        visitor.visit(this)
}