use std::io::{Read, Write};
use lalrpop_util::{lalrpop_mod, ParseError};
use crate::lexer::{Lexer, LexicalError};
lalrpop_mod!(pub aurora);

pub mod ast;
pub mod lexer;
pub mod tree_walk;
mod err;

fn usize_to_line_col(code: &str, pos: usize) -> (usize, usize) {
    let mut line = 1;
    let mut col = 1;
    for (i, c) in code.chars().enumerate() {
        if i == pos {
            break;
        }
        if c == '\r' {
            continue;
        }
        if c == '\n' {
            line += 1;
            col = 1;
        } else {
            col += 1;
        }
    }
    (line, col)
}

fn get_line(code: &str, line: usize) -> String {
    match code.lines().nth(line - 1) {
        Some(l) => l.to_string(),
        None => "".to_string(),
    }
}

const CC_INVOCATION: &str = "-std=c++17 -Ofast";

fn help() {
    println!("Usage: aurora [options] [file]");
    println!("Options:");
    println!("  -h, --help\t\tDisplay this help message");
    println!("  -v, --version\t\tDisplay the version of Aurora");
    println!("  -o, --output\t\tSpecify the output file");
    println!("  -c, --compiler\t\tSpecify the compiler to use");
    println!("  -a, --args\t\tSpecify arguments to pass to the compiler");
}

fn parse_args() -> (bool, bool, String, String, String, String) {
    let mut args = std::env::args();
    let mut help = false;
    let mut version = false;
    let mut output = String::new();
    let mut compiler = String::new();
    let mut compiler_args = String::new();
    let mut file = String::new();
    args.next();
    while let Some(arg) = args.next() {
        match arg.as_str() {
            "-h" | "--help" => {
                help = true;
            }
            "-v" | "--version" => {
                version = true;
            }
            "-o" | "--output" => {
                output = args.next().unwrap();
            }
            "-c" | "--compiler" => {
                compiler = args.next().unwrap();
            }
            "-a" | "--args" => {
                compiler_args = args.next().unwrap();
            }
            _ => {
                file = arg;
            }
        }
    }
    (help, version, output, compiler, compiler_args, file)
}

fn main() {
    let (help_, version, output_, compiler, compiler_args, file) = parse_args();
    if help_ {
        help();
        return;
    }
    if version {
        println!("Aurora v{}", env!("CARGO_PKG_VERSION"));
        return;
    }
    let mut code = String::new();
    if file == "-" {
        std::io::stdin().read_to_string(&mut code).unwrap();
    } else {
        let mut file = std::fs::File::open(file).unwrap();
        file.read_to_string(&mut code).unwrap();
    }
    let code = code.as_str();
    let expr = aurora::ProgramParser::new()
        .parse(Lexer::new(code));
    match expr {
        Ok(_) => {
            let mut output = String::new();
            output.push_str("#include <AuroraRuntime.h>\n");
            output.push_str("int main() {" );

            for e in expr.unwrap() {
                output.push_str(&*tree_walk::stmt_visitor(&e));
            }

            output.push_str("}");
            let mut temp_file = tempfile::Builder::new()
                .suffix(".cpp")
                .tempfile().unwrap();
            temp_file.write(output.as_bytes()).unwrap();
            let path = temp_file.path();
            let path = path.to_str().unwrap();
            let mut cmd = std::process::Command::new(if compiler == "" { "c++" } else { compiler.as_str() });
            if compiler_args != "" {
                for arg in compiler_args.split_whitespace() {
                    cmd.arg(arg);
                }
            } else {
                for arg in CC_INVOCATION.split_whitespace() {
                    cmd.arg(arg);
                }
            }
            if output_ != "" {
                cmd.arg("-o").arg(output_);
            }
            cmd.arg(path);
            let output = cmd.output().unwrap();
            if !output.status.success() {
                println!("{}", String::from_utf8(output.stderr).unwrap());
            }
        },
        Err(e) => {
            match e {
                ParseError::InvalidToken { location } => {
                    let (line, col) = usize_to_line_col(code, location);
                    println!("{}", err::Err::new(err::ErrKind::Syntax, err::Pos::new(line, col), "Invalid token".to_string()));
                    let code = code.replace("\n", "⤶\n");
                    let code = code.as_str();
                    println!("{}", get_line(code, line));
                    for _ in 0..col - 1 {
                        print!(" ");
                    }
                    println!("^");
                },
                ParseError::UnrecognizedEOF { location, expected } => {
                    let (line, col) = usize_to_line_col(code, location);
                    println!("{}", err::Err::new(err::ErrKind::Syntax, err::Pos::new(line, col), format!("Unexpected end of file, expected {:?}", expected)));
                    let code = code.replace("\n", "⤶\n");
                    let code = code.as_str();
                    println!("{}", get_line(code, line));
                    for _ in 0..col - 1 {
                        print!(" ");
                    }
                    println!("^");
                },
                ParseError::UnrecognizedToken { token: (l, t, r), expected } => {
                    let (line, col) = usize_to_line_col(code, l);
                    println!("{}", err::Err::new(err::ErrKind::Syntax, err::Pos::new(line, col), format!("Unexpected token {:?}, expected {:?}", t, expected)));
                    let code = code.replace("\n", "⤶\n");
                    let code = code.as_str();
                    println!("{}", get_line(code, line));
                    for _ in 0..col - 1 {
                        print!(" ");
                    }
                    println!("^");
                },
                ParseError::ExtraToken { token: (l, t, r) } => {
                    let (line, col) = usize_to_line_col(code, l);
                    println!("{}", err::Err::new(err::ErrKind::Syntax, err::Pos::new(line, col), format!("Unexpected token {:?}", t)));
                    let code = code.replace("\n", "⤶\n");
                    let code = code.as_str();
                    println!("{}", get_line(code, line));
                    for _ in 0..col - 1 {
                        print!(" ");
                    }
                    println!("^");
                },
                ParseError::User { error } => {
                    let e = error as LexicalError;
                    match e {
                        LexicalError::UnrecognizedToken(pos) => {
                            let (line, col) = usize_to_line_col(code, pos);
                            println!("{}", err::Err::new(err::ErrKind::Syntax, err::Pos::new(line, col), "Unrecognized token".to_string()));
                            let code = code.replace("\n", "⤶\n");
                            let code = code.as_str();
                            println!("{}", get_line(code, line));
                            for _ in 0..col - 1 {
                                print!(" ");
                            }
                            println!("^");
                        }
                        LexicalError::UnterminatedString(pos) => {
                            let (line, col) = usize_to_line_col(code, pos);
                            println!("{}", err::Err::new(err::ErrKind::Syntax, err::Pos::new(line, col), "Unterminated string".to_string()));
                            let code = code.replace("\n", "⤶\n");
                            let code = code.as_str();
                            println!("{}", get_line(code, line));
                            for _ in 0..col - 1 {
                                print!(" ");
                            }
                            println!("^");
                        }
                    }
                }
            }
        }
    }
}
