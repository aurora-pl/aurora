use std::borrow::{Borrow};
use crate::ast::{AssignOp, BinOp, Expr, Stmt, UnaryOp};

static mut DECLARED_VARS: Vec<String> = Vec::new();

fn bin_op_visitor(op: &BinOp) -> String {
    match op {
        BinOp::Add => "+".to_string(),
        BinOp::Sub => "-".to_string(),
        BinOp::Mul => "*".to_string(),
        BinOp::Div => "/".to_string(),
        BinOp::Mod => "%".to_string(),
        BinOp::Less => "<".to_string(),
        BinOp::LessEq => "<=".to_string(),
        BinOp::Greater => ">".to_string(),
        BinOp::GreaterEq => ">=".to_string(),
        BinOp::Eq => "==".to_string(),
        BinOp::NotEq => "!=".to_string(),
        BinOp::And => "&&".to_string(),
        BinOp::Or => "||".to_string(),
    }
}

fn unary_op_visitor(op: &UnaryOp) -> String {
    match op {
        UnaryOp::Neg => "-".to_string(),
        UnaryOp::Not => "!".to_string(),
    }
}

fn expr_visitor(expr: &Box<Expr>) -> String {
    match expr.borrow() {
        Expr::Number(n) => {
            format!("{:.8}", n)
        }
        Expr::Boolean(b) => {
            format!("AuroraObject({})", b)
        }
        Expr::String(s) => {
            format!("AuroraObject(\"{}\")", s)
        }
        Expr::Variable(v) => {
            format!("{}", v)
        }
        Expr::BinOp(l, o,r) => {
            format!("({} {} {})", expr_visitor(l), bin_op_visitor(o), expr_visitor(r))
        }
        Expr::UnaryOp(o, e) => {
            format!("({} {})", unary_op_visitor(o), expr_visitor(e))
        }
        Expr::List(l) => {
            let mut s = String::new();
            for e in l {
                match e.borrow() {
                    Expr::Number(n) => {
                        s.push_str(&format!("AuroraObject({:.8})", *n));
                    }
                    _ => {
                        s.push_str(&expr_visitor(e));
                    }
                }
                s.push_str(", ");
            }
            s.pop();
            s.pop();
            format!("AuroraObject({{ {} }})", s)
        }
        Expr::Call(e, c) => {
            let mut s = String::new();
            s.push_str("{");
            for ex in c {
                match ex.borrow() {
                    Expr::Number(n) => {
                        s.push_str(&format!("AuroraObject({:.8})", *n));
                    }
                    _ => {
                        s.push_str(&expr_visitor(ex));
                    }
                }
                s.push_str(", ");
            }
            s.pop();
            s.pop();
            s.push_str("}");
            format!("{}({}, std::move((AuroraObject[{}]){}), true)", expr_visitor(e), c.len(), c.len(), s)
        }
        Expr::Index(e, i) => {
            format!("{}[{}]", expr_visitor(e), expr_visitor(i))
        }
    }
}

pub(crate) fn stmt_visitor(stmt: &Box<Stmt>) -> String {
    match stmt.borrow() {
        Stmt::If(e, t, f) => unsafe {
            let mut s = String::new();
            s.push_str(&format!("if ({}) {{", expr_visitor(e)));
            let prev_declared_vars = DECLARED_VARS.clone();
            for st in t {
                s.push_str(&stmt_visitor(st));
            }
            DECLARED_VARS = prev_declared_vars;
            s.push_str("}");
            if f.len() > 0 {
                s.push_str(" else {");
                let prev_declared_vars = DECLARED_VARS.clone();
                for st in f {
                    s.push_str(&stmt_visitor(st));
                }
                DECLARED_VARS = prev_declared_vars;
                s.push_str("}");
            }
            s
        }
        Stmt::While(e, b) => unsafe {
            let mut s = String::new();
            s.push_str(&format!("while ({}) {{", expr_visitor(e)));
            let prev_declared_vars = DECLARED_VARS.clone();
            for st in b {
                s.push_str(&stmt_visitor(st));
            }
            DECLARED_VARS = prev_declared_vars;
            s.push_str("}");
            s
        }
        Stmt::For(v, e, b) => unsafe {
            let mut s = String::new();
            s.push_str(&format!("for (auto& {} : {}) {{", v, expr_visitor(e)));
            let prev_declared_vars = DECLARED_VARS.clone();
            for st in b {
                s.push_str(&stmt_visitor(st));
            }
            DECLARED_VARS = prev_declared_vars;
            s.push_str("}");
            s
        }
        Stmt::Assign(v, o, e) => unsafe {
            let mut s = String::new();
            if DECLARED_VARS.contains(v) {
                s.push_str(&format!("{} ", v));
                match o {
                    AssignOp::Assign => s.push_str("="),
                    AssignOp::PlusAssign => s.push_str("+="),
                    AssignOp::MinusAssign => s.push_str("-="),
                    AssignOp::StarAssign => s.push_str("*="),
                    AssignOp::SlashAssign => s.push_str("/="),
                    AssignOp::ModAssign => s.push_str("%="),
                }
                s.push_str(&format!(" {};", expr_visitor(e)));
            } else {
                s.push_str(&format!("auto {} = {};", v, expr_visitor(e)));
                DECLARED_VARS.push(v.to_string());
            }
            s
        }
        Stmt::Func(v, a, b) => unsafe {
            let mut s = String::new();
            s.push_str(&format!("auto _CAPTURE_{} = [&](const AuroraObject& {}, int _FN_ARG_COUNT, AuroraObject* _FN_ARGS) -> AuroraObject {{", v, v));
            s.push_str(&format!("if (_FN_ARG_COUNT != {}) error(\"incorrect number of args\"); ", a.len()));
            let prev_declared_vars = DECLARED_VARS.clone();
            for (i, v) in a.iter().enumerate() {
                DECLARED_VARS.push(v.to_string());
                s.push_str(&format!("auto& {} = _FN_ARGS[{}];", v, i));
            }
            for st in b {
                s.push_str(&stmt_visitor(st));
            }
            DECLARED_VARS = prev_declared_vars;
            DECLARED_VARS.push(v.to_string());
            s.push_str("error(\"function must return a value\");");
            s.push_str("};");
            s.push_str(&format!("auto _PTR_WRAP_{} = [](void* _LAMBDA_PTR, int _FN_ARG_COUNT, AuroraObject* _FN_ARGS, AuroraObject& _SELF) -> AuroraObject {{", v));
            s.push_str(&format!("return (*(decltype(_CAPTURE_{})*) _LAMBDA_PTR)(_SELF, _FN_ARG_COUNT, _FN_ARGS);", v));
            s.push_str("};");
            s.push_str(&format!("auto {} = AuroraObject(AuroraFunc(_PTR_WRAP_{}), &_CAPTURE_{});", v, v, v));
            s
        }
        Stmt::Sub(v, a, b) => unsafe {
            let mut s = String::new();
            s.push_str(&format!("auto _CAPTURE_{} = [&](const AuroraObject& {}, int _FN_ARG_COUNT, AuroraObject* _FN_ARGS) -> void {{", v, v));
            s.push_str(&format!("if (_FN_ARG_COUNT != {}) error(\"incorrect number of args\"); ", a.len()));
            let prev_declared_vars = DECLARED_VARS.clone();
            for (i, v) in a.iter().enumerate() {
                DECLARED_VARS.push(v.to_string());
                s.push_str(&format!("auto& {} = _FN_ARGS[{}];", v, i));
            }
            for st in b {
                s.push_str(&stmt_visitor(st));
            }
            DECLARED_VARS = prev_declared_vars;
            DECLARED_VARS.push(v.to_string());
            s.push_str("};");
            s.push_str(&format!("auto _PTR_WRAP_{} = [](void* _LAMBDA_PTR, int _FN_ARG_COUNT, AuroraObject* _FN_ARGS, AuroraObject& _SELF) -> void {{", v));
            s.push_str(&format!("(*(decltype(_CAPTURE_{})*) _LAMBDA_PTR)(_SELF, _FN_ARG_COUNT, _FN_ARGS);", v));
            s.push_str("};");
            s.push_str(&format!("auto {} = AuroraObject(AuroraSub(_PTR_WRAP_{}), &_CAPTURE_{});", v, v, v));
            s
        }
        Stmt::Return(e) => {
            format!("return {};", expr_visitor(e))
        }
        Stmt::Break => {
            "break;".to_string()
        }
        Stmt::Continue => {
            "continue;".to_string()
        }
        Stmt::AssignIndex(v, i, e) => {
            match i.borrow() {
                Expr::Number(n) => {
                    format!("{}.set({}, AuroraObject({}));", v, *n, expr_visitor(e))
                }
                _ => {
                    format!("{}.set({}, {});", v, expr_visitor(i), expr_visitor(e))
                }
            }
        }
        Stmt::Call(e, c) => {
            let mut s = String::new();
            s.push_str("{");
            for ex in c {
                match ex.borrow() {
                    Expr::Number(n) => {
                        s.push_str(&format!("AuroraObject({:.8})", *n));
                    }
                    _ => {
                        s.push_str(&expr_visitor(ex));
                    }
                }
                s.push_str(", ");
            }
            s.pop();
            s.pop();
            s.push_str("}");
            format!("{}({}, std::move((AuroraObject[{}]){}), false);", e, c.len(), c.len(), s)
        }
        Stmt::Empty => {
            "".to_string()
        }
    }
}