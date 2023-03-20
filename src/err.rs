use std::fmt;
use std::fmt::{Debug, Display};
use colored::*;

pub(crate) enum ErrKind {
    Syntax,
    Semantics,
    Internal,
}

pub(crate) struct Pos {
    line: usize,
    col: usize,
}

impl Pos {
    pub(crate) fn new(line: usize, col: usize) -> Self {
        Self { line, col }
    }
}

impl fmt::Display for Pos {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "line {}, col {}", self.line, self.col)
    }
}

pub(crate) struct Err {
    kind: ErrKind,
    pos: Pos,
    message: String,
}

impl Err {
    pub(crate) fn new(kind: ErrKind, pos: Pos, message: String) -> Self {
        Self { kind, pos, message }
    }

    fn syntax(pos: Pos, message: String) -> Self {
        Self::new(ErrKind::Syntax, pos, message)
    }

    fn semantics(pos: Pos, message: String) -> Self {
        Self::new(ErrKind::Semantics, pos, message)
    }

    fn internal(pos: Pos, message: String) -> Self {
        Self::new(ErrKind::Internal, pos, message)
    }
}

impl fmt::Display for Err {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        match self.kind {
            ErrKind::Syntax => {
                <ColoredString as Display>::fmt(&"Syntax Error: ".bright_red(), f)?;
            }
            ErrKind::Semantics => write!(f, "{}", "Error: ".bright_red())?,
            ErrKind::Internal => write!(f, "{}", "Internal error: ".red())?,
        }
        write!(f, "{}: {}", self.pos, self.message)
    }
}