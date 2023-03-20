#[derive(Debug, Clone)]
pub enum BinOp {
    Add,
    Sub,
    Mul,
    Div,
    Mod,
    Less,
    LessEq,
    Greater,
    GreaterEq,
    Eq,
    NotEq,
    And,
    Or,
}

#[derive(Debug, Clone)]
pub enum UnaryOp {
    Neg,
    Not,
}

#[derive(Debug, Clone)]
pub enum Expr {
    Number(f64),
    Boolean(bool),
    String(String),
    Variable(String),
    BinOp(Box<Expr>, BinOp, Box<Expr>),
    UnaryOp(UnaryOp, Box<Expr>),
    List(Vec<Box<Expr>>),
    Call(Box<Expr>, Vec<Box<Expr>>),
    Index(Box<Expr>, Box<Expr>),
}

#[derive(Debug, Clone)]
pub enum AssignOp {
    Assign,
    PlusAssign,
    MinusAssign,
    StarAssign,
    SlashAssign,
    ModAssign,
}

#[derive(Debug, Clone)]
pub enum Stmt {
    If(Box<Expr>, Vec<Box<Stmt>>, Vec<Box<Stmt>>),
    While(Box<Expr>, Vec<Box<Stmt>>),
    For(String, Box<Expr>, Vec<Box<Stmt>>),
    Assign(String, AssignOp, Box<Expr>),
    Func(String, Vec<String>, Vec<Box<Stmt>>),
    Sub(String, Vec<String>, Vec<Box<Stmt>>),
    Return(Box<Expr>),
    Break,
    Continue,
    AssignIndex(String, Box<Expr>, Box<Expr>),
    Call(String, Vec<Box<Expr>>),
    Empty,
}
