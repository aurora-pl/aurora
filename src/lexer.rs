use std::collections::HashMap;
use std::iter::Peekable;
use std::str::CharIndices;
use std::string::ParseError;
use regex::Error;

pub type Spanned<Tok, Loc, Error> = Result<(Loc, Tok, Loc), Error>;

#[derive(Debug, PartialEq, Clone)]
pub enum Tok {
    // Keywords
    If,
    Else,
    While,
    For,
    Fn,
    Sub,
    Return,
    Break,
    Continue,
    End,
    // Operators
    Plus,
    Minus,
    Star,
    Slash,
    Mod,
    Less,
    LessEq,
    Greater,
    GreaterEq,
    Eq,
    NotEq,
    And,
    Or,
    Not,
    // Punctuation
    LParen,
    RParen,
    LBrace,
    RBrace,
    Comma,
    Colon,
    Newline,
    Assign,
    PlusAssign,
    MinusAssign,
    StarAssign,
    SlashAssign,
    ModAssign,
    Arrow,
    // Literals
    Number(f64),
    Boolean(bool),
    String(String),
    Identifier(String),
    // Special
    Eof,
}

pub struct Lexer<'input> {
    chars: Peekable<CharIndices<'input>>,
    keywords: HashMap<&'static str, Tok>,
}

#[derive(Debug, PartialEq, Clone)]
pub enum LexicalError {
    UnrecognizedToken(usize),
    UnterminatedString(usize),
}

impl<'input> Lexer<'input> {
    pub fn new(input: &'input str) -> Self {
        Lexer {
            chars: input.char_indices().peekable(),
            keywords: {
                let mut m = HashMap::new();
                m.insert("if", Tok::If);
                m.insert("else", Tok::Else);
                m.insert("while", Tok::While);
                m.insert("for", Tok::For);
                m.insert("fn", Tok::Fn);
                m.insert("sub", Tok::Sub);
                m.insert("return", Tok::Return);
                m.insert("break", Tok::Break);
                m.insert("continue", Tok::Continue);
                m.insert("end", Tok::End);
                m.insert("true", Tok::Boolean(true));
                m.insert("false", Tok::Boolean(false));
                m.insert("and", Tok::And);
                m.insert("or", Tok::Or);
                m.insert("not", Tok::Not);
                m
            }
        }
    }
}

impl <'input> Iterator for Lexer<'input> {
    type Item = Spanned<Tok, usize, LexicalError>;

    fn next(&mut self) -> Option<Self::Item> {
        loop {
            match self.chars.next() {
                Some((_, ' ')) => continue,
                Some((_, '\t')) => continue,
                Some((_, '\r')) => continue,
                Some((start, '\n')) => {
                    loop {
                        match self.chars.peek() {
                            Some(&(_, '\n')) => { self.chars.next(); },
                            Some(&(_, '\r')) => { self.chars.next(); },
                            _ => break,
                        }
                    }
                    if start == 0 {
                        continue;
                    }
                    return Some(Ok((start, Tok::Newline, start + 1)))
                }

                Some((start, '+')) => {
                    return match self.chars.peek() {
                        Some((_, '=')) => { self.chars.next(); Some(Ok((start, Tok::PlusAssign, start + 2))) },
                        _ => Some(Ok(( start, Tok::Plus, start + 1))),
                    }
                },
                Some((start, '-')) => {
                    return match self.chars.peek() {
                        Some((_, '=')) => { self.chars.next(); Some(Ok((start, Tok::MinusAssign, start + 2))) },
                        Some((_, '>')) => { self.chars.next(); Some(Ok((start, Tok::Arrow, start + 2))) },
                        _ => Some(Ok((start, Tok::Minus, start + 1))),
                    }
                },
                Some((start, '*')) => {
                    return match self.chars.peek() {
                        Some((_, '=')) => { self.chars.next(); Some(Ok((start, Tok::StarAssign, start + 2))) },
                        _ => Some(Ok((start, Tok::Star, start + 1))),
                    }
                },
                Some((start, '/')) => {
                    return match self.chars.peek() {
                        Some((_, '=')) => { self.chars.next(); Some(Ok((start, Tok::SlashAssign, start + 2))) },
                        _ => Some(Ok((start, Tok::Slash, start + 1))),
                    }
                },
                Some((start, '%')) => {
                    return match self.chars.peek() {
                        Some((_, '=')) => { self.chars.next(); Some(Ok((start, Tok::ModAssign, start + 2))) },
                        _ => Some(Ok((start, Tok::Mod, start + 1))),
                    }
                },
                Some((start, '<')) => {
                    return match self.chars.peek() {
                        Some((_, '=')) => { self.chars.next(); Some(Ok((start, Tok::LessEq, start + 2))) },
                        _ => Some(Ok((start, Tok::Less, start + 1))),
                    }
                }
                Some((start, '>')) => {
                    return match self.chars.peek() {
                        Some((_, '=')) => { self.chars.next(); Some(Ok((start, Tok::GreaterEq, start + 2))) },
                        _ => Some(Ok((start, Tok::Greater, start + 1))),
                    }
                }
                Some((start, '=')) => {
                    return match self.chars.peek() {
                        Some((_, '=')) => { self.chars.next(); Some(Ok((start, Tok::Eq, start + 2))) },
                        _ => Some(Ok((start, Tok::Assign, start + 1))),
                    }
                }
                Some((start, '!')) => {
                    return match self.chars.peek() {
                        Some((_, '=')) => { self.chars.next(); Some(Ok((start, Tok::NotEq, start + 2))) },
                        _ => Some(Err(LexicalError::UnrecognizedToken(start))),
                    }
                }
                Some((start, '(')) => return Some(Ok((start, Tok::LParen, start + 1))),
                Some((start, ')')) => return Some(Ok((start, Tok::RParen, start + 1))),
                Some((start, '{')) => return Some(Ok((start, Tok::LBrace, start + 1))),
                Some((start, '}')) => return Some(Ok((start, Tok::RBrace, start + 1))),
                Some((start, ',')) => return Some(Ok((start, Tok::Comma, start + 1))),
                Some((start, ':')) => return Some(Ok((start, Tok::Colon, start + 1))),
                Some((start, '"')) => {
                    let mut string = String::new();
                    loop {
                        match self.chars.next() {
                            Some((_, '"')) => break,
                            Some((_, char)) => string.push(char),
                            None => return Some(Err(LexicalError::UnterminatedString(start))),
                        }
                    }
                    let len = string.len();
                    return Some(Ok((start, Tok::String(string), start + len + 2)));
                }
                Some((start, char)) => {
                    return if char.is_alphabetic() || char == '_' {
                        let mut ident = String::new();
                        ident.push(char);
                        loop {
                            match self.chars.peek() {
                                Some((_, char)) if char.is_alphanumeric() || *char == '_' => ident.push(self.chars.next().unwrap().1),
                                _ => break,
                            }
                        }
                        let len = ident.len();
                        if let Some(keyword) = self.keywords.get(&ident[..]) {
                            Some(Ok((start, keyword.clone(), start + len)))
                        } else {
                            Some(Ok((start, Tok::Identifier(ident), start + len)))
                        }
                    } else if char.is_digit(10) {
                        let mut number = String::new();
                        number.push(char);
                        loop {
                            match self.chars.peek() {
                                Some((_, char)) if char.is_digit(10) => number.push(self.chars.next().unwrap().1),
                                _ => break,
                            }
                        }
                        Some(Ok((start, Tok::Number(number.parse().unwrap()), start + number.len())))
                    } else {
                        Some(Err(LexicalError::UnrecognizedToken(start)))
                    }
                }
                None => return None,
            }
        }
    }
}