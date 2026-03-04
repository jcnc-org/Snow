#pragma once

#include <memory>
#include <string>
#include <vector>

namespace snow::frontend {

enum class Visibility {
  Private,
  Internal,
  Public,
};

struct ImportDecl {
  std::vector<std::string> path_segments;
  std::string alias;
  bool is_star = false;
};

struct ParamDecl {
  std::string name;
  std::string type;
};

enum class BinaryOp {
  Add,
  Sub,
  Mul,
  Div,
  Mod,
  Eq,
  Ne,
  Lt,
  Gt,
  Le,
  Ge,
};

struct Expr {
  enum class Kind {
    Number,
    Identifier,
    Call,
    Binary,
  };

  Kind kind = Kind::Number;
  std::string value;
  BinaryOp op = BinaryOp::Add;
  std::shared_ptr<Expr> lhs;
  std::shared_ptr<Expr> rhs;
  std::vector<std::shared_ptr<Expr>> args;
};

struct IfExpr {
  std::shared_ptr<Expr> condition;
  std::shared_ptr<Expr> then_expr;
  std::shared_ptr<Expr> else_expr;
};

struct FunctionDecl {
  Visibility visibility = Visibility::Private;
  std::string name;
  std::vector<ParamDecl> params;
  std::string return_type;
  std::shared_ptr<Expr> return_expr;
  std::shared_ptr<IfExpr> if_expr;
  std::shared_ptr<Expr> while_condition;
};

struct AstModule {
  std::string module_path;
  std::vector<ImportDecl> imports;
  std::vector<FunctionDecl> functions;
};

std::string ToString(Visibility visibility);
std::string DumpAst(const AstModule& module);

}  // namespace snow::frontend
