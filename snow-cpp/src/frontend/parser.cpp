#include "snow/frontend/parser.h"

#include <optional>
#include <sstream>
#include <iterator>
#include <utility>
#include <vector>

namespace snow::frontend {

namespace {

class Cursor {
 public:
  explicit Cursor(const TokenStream& tokens) : tokens_(tokens) {}

  const Token& Peek(const std::size_t offset = 0) const {
    const std::size_t idx = index_ + offset;
    if (idx >= tokens_.size()) {
      return tokens_.back();
    }
    return tokens_[idx];
  }

  const Token& Advance() {
    const Token& token = Peek();
    if (index_ < tokens_.size()) {
      ++index_;
    }
    return token;
  }

  [[nodiscard]] bool Match(const TokenType type) {
    if (Peek().type != type) {
      return false;
    }
    Advance();
    return true;
  }

  [[nodiscard]] bool AtEnd() const {
    return Peek().type == TokenType::EndOfFile;
  }

 private:
  const TokenStream& tokens_;
  std::size_t index_ = 0;
};

using ExprPtr = std::shared_ptr<Expr>;

std::string JoinPath(const std::vector<std::string>& segments) {
  std::ostringstream oss;
  for (std::size_t i = 0; i < segments.size(); ++i) {
    if (i > 0) {
      oss << ".";
    }
    oss << segments[i];
  }
  return oss.str();
}

Visibility ParseVisibility(Cursor& cursor) {
  if (cursor.Match(TokenType::KeywordPub)) {
    return Visibility::Public;
  }
  if (cursor.Match(TokenType::KeywordInternal)) {
    return Visibility::Internal;
  }
  if (cursor.Match(TokenType::KeywordPrivate)) {
    return Visibility::Private;
  }
  return Visibility::Private;
}

ExprPtr MakeNumberExpr(std::string value) {
  auto expr = std::make_shared<Expr>();
  expr->kind = Expr::Kind::Number;
  expr->value = std::move(value);
  return expr;
}

ExprPtr MakeIdentifierExpr(std::string value) {
  auto expr = std::make_shared<Expr>();
  expr->kind = Expr::Kind::Identifier;
  expr->value = std::move(value);
  return expr;
}

ExprPtr MakeCallExpr(std::string callee, std::vector<ExprPtr> args) {
  auto expr = std::make_shared<Expr>();
  expr->kind = Expr::Kind::Call;
  expr->value = std::move(callee);
  expr->args = std::move(args);
  return expr;
}

ExprPtr MakeBinaryExpr(BinaryOp op, ExprPtr lhs, ExprPtr rhs) {
  auto expr = std::make_shared<Expr>();
  expr->kind = Expr::Kind::Binary;
  expr->op = op;
  expr->lhs = std::move(lhs);
  expr->rhs = std::move(rhs);
  return expr;
}

std::optional<BinaryOp> TokenToBinaryOp(const TokenType type) {
  switch (type) {
    case TokenType::Plus:
      return BinaryOp::Add;
    case TokenType::Minus:
      return BinaryOp::Sub;
    case TokenType::Star:
      return BinaryOp::Mul;
    case TokenType::Slash:
      return BinaryOp::Div;
    case TokenType::Percent:
      return BinaryOp::Mod;
    case TokenType::EqualEqual:
      return BinaryOp::Eq;
    case TokenType::BangEqual:
      return BinaryOp::Ne;
    case TokenType::Less:
      return BinaryOp::Lt;
    case TokenType::Greater:
      return BinaryOp::Gt;
    case TokenType::LessEqual:
      return BinaryOp::Le;
    case TokenType::GreaterEqual:
      return BinaryOp::Ge;
    default:
      return std::nullopt;
  }
}

int PrecedenceFor(const TokenType type) {
  switch (type) {
    case TokenType::EqualEqual:
    case TokenType::BangEqual:
      return 1;
    case TokenType::Less:
    case TokenType::Greater:
    case TokenType::LessEqual:
    case TokenType::GreaterEqual:
      return 2;
    case TokenType::Plus:
    case TokenType::Minus:
      return 3;
    case TokenType::Star:
    case TokenType::Slash:
    case TokenType::Percent:
      return 4;
    default:
      return 0;
  }
}

std::string BinaryOpToString(const BinaryOp op) {
  switch (op) {
    case BinaryOp::Add:
      return "+";
    case BinaryOp::Sub:
      return "-";
    case BinaryOp::Mul:
      return "*";
    case BinaryOp::Div:
      return "/";
    case BinaryOp::Mod:
      return "%";
    case BinaryOp::Eq:
      return "==";
    case BinaryOp::Ne:
      return "!=";
    case BinaryOp::Lt:
      return "<";
    case BinaryOp::Gt:
      return ">";
    case BinaryOp::Le:
      return "<=";
    case BinaryOp::Ge:
      return ">=";
  }
  return "?";
}

std::string DumpExpr(const ExprPtr& expr) {
  if (!expr) {
    return "<none>";
  }
  switch (expr->kind) {
    case Expr::Kind::Number:
      return expr->value;
    case Expr::Kind::Identifier:
      return expr->value;
    case Expr::Kind::Call: {
      std::ostringstream oss;
      oss << expr->value << "(";
      for (std::size_t i = 0; i < expr->args.size(); ++i) {
        if (i > 0) {
          oss << ", ";
        }
        oss << DumpExpr(expr->args[i]);
      }
      oss << ")";
      return oss.str();
    }
    case Expr::Kind::Binary:
      return "(" + DumpExpr(expr->lhs) + " " + BinaryOpToString(expr->op) + " " + DumpExpr(expr->rhs) + ")";
  }
  return "<none>";
}

ExprPtr ParseExpression(Cursor& cursor, snow::common::DiagnosticEngine& diagnostics, const std::string& module_path,
                        int min_precedence);
std::optional<Statement> ParseStatement(Cursor& cursor, snow::common::DiagnosticEngine& diagnostics,
                                        const std::string& module_path);
std::vector<Statement> ParseBlock(Cursor& cursor, snow::common::DiagnosticEngine& diagnostics,
                                  const std::string& module_path);

void RecoverToStatementBoundary(Cursor& cursor) {
  while (!cursor.AtEnd()) {
    if (cursor.Match(TokenType::Semicolon)) {
      return;
    }
    if (cursor.Peek().type == TokenType::RBrace) {
      return;
    }
    cursor.Advance();
  }
}

bool ExpectToken(Cursor& cursor, const TokenType expected, const std::string& error_code, const std::string& message,
                 snow::common::DiagnosticEngine& diagnostics, const std::string& module_path) {
  if (cursor.Match(expected)) {
    return true;
  }
  diagnostics.Error(error_code, message, module_path, cursor.Peek().range);
  return false;
}

ExprPtr ParsePrimary(Cursor& cursor, snow::common::DiagnosticEngine& diagnostics, const std::string& module_path) {
  if (cursor.Peek().type == TokenType::Number) {
    return MakeNumberExpr(cursor.Advance().lexeme);
  }

  if (cursor.Peek().type == TokenType::Identifier) {
    const std::string ident = cursor.Advance().lexeme;

    if (!cursor.Match(TokenType::LParen)) {
      return MakeIdentifierExpr(ident);
    }

    std::vector<ExprPtr> args;
    if (cursor.Peek().type != TokenType::RParen) {
      while (!cursor.AtEnd()) {
        args.push_back(ParseExpression(cursor, diagnostics, module_path, 1));
        if (!cursor.Match(TokenType::Comma)) {
          break;
        }
      }
    }

    if (!cursor.Match(TokenType::RParen)) {
      diagnostics.Error("E_PARSE_CALL_RPAREN", "Unclosed call expression", module_path, cursor.Peek().range);
    }
    return MakeCallExpr(ident, std::move(args));
  }

  if (cursor.Match(TokenType::LParen)) {
    auto expr = ParseExpression(cursor, diagnostics, module_path, 1);
    if (!cursor.Match(TokenType::RParen)) {
      diagnostics.Error("E_PARSE_EXPR_RPAREN", "Expected ')' to close grouped expression", module_path,
                        cursor.Peek().range);
    }
    return expr;
  }

  diagnostics.Error("E_PARSE_EXPR_PRIMARY", "Expected expression", module_path, cursor.Peek().range);
  if (!cursor.AtEnd()) {
    cursor.Advance();
  }
  return MakeNumberExpr("0");
}

ExprPtr ParseExpression(Cursor& cursor, snow::common::DiagnosticEngine& diagnostics, const std::string& module_path,
                        const int min_precedence) {
  auto lhs = ParsePrimary(cursor, diagnostics, module_path);

  while (!cursor.AtEnd()) {
    const auto maybe_op = TokenToBinaryOp(cursor.Peek().type);
    if (!maybe_op.has_value()) {
      break;
    }

    const int precedence = PrecedenceFor(cursor.Peek().type);
    if (precedence < min_precedence) {
      break;
    }

    const BinaryOp op = maybe_op.value();
    cursor.Advance();

    auto rhs = ParseExpression(cursor, diagnostics, module_path, precedence + 1);
    lhs = MakeBinaryExpr(op, std::move(lhs), std::move(rhs));
  }

  return lhs;
}

std::vector<Statement> ParseBlock(Cursor& cursor, snow::common::DiagnosticEngine& diagnostics,
                                  const std::string& module_path) {
  std::vector<Statement> statements;
  if (!cursor.Match(TokenType::LBrace)) {
    diagnostics.Error("E_PARSE_BLOCK_LBRACE", "Expected '{' to start block", module_path, cursor.Peek().range);
    return statements;
  }

  while (!cursor.AtEnd() && cursor.Peek().type != TokenType::RBrace) {
    if (cursor.Peek().type == TokenType::LBrace) {
      auto nested = ParseBlock(cursor, diagnostics, module_path);
      statements.insert(statements.end(), std::make_move_iterator(nested.begin()), std::make_move_iterator(nested.end()));
      continue;
    }

    auto maybe_stmt = ParseStatement(cursor, diagnostics, module_path);
    if (maybe_stmt.has_value()) {
      statements.push_back(std::move(maybe_stmt.value()));
    }
  }

  if (!cursor.Match(TokenType::RBrace)) {
    diagnostics.Error("E_PARSE_BLOCK_RBRACE", "Expected '}' to close block", module_path, cursor.Peek().range);
  }
  return statements;
}

std::optional<Statement> ParseStatement(Cursor& cursor, snow::common::DiagnosticEngine& diagnostics,
                                        const std::string& module_path) {
  if (cursor.Match(TokenType::KeywordReturn)) {
    Statement stmt;
    stmt.kind = Statement::Kind::Return;
    if (cursor.Peek().type != TokenType::Semicolon) {
      stmt.expr = ParseExpression(cursor, diagnostics, module_path, 1);
    }
    if (!ExpectToken(cursor, TokenType::Semicolon, "E_PARSE_RETURN_SEMI", "Expected ';' after return statement",
                     diagnostics, module_path)) {
      RecoverToStatementBoundary(cursor);
    }
    return stmt;
  }

  if (cursor.Match(TokenType::KeywordLet)) {
    Statement stmt;
    stmt.kind = Statement::Kind::Let;

    if (cursor.Peek().type != TokenType::Identifier) {
      diagnostics.Error("E_PARSE_LET_NAME", "Expected variable name after 'let'", module_path, cursor.Peek().range);
      RecoverToStatementBoundary(cursor);
      return stmt;
    }
    stmt.name = cursor.Advance().lexeme;

    if (cursor.Match(TokenType::Colon) && cursor.Peek().type == TokenType::Identifier) {
      stmt.type_name = cursor.Advance().lexeme;
    }

    if (!ExpectToken(cursor, TokenType::Equal, "E_PARSE_LET_ASSIGN", "Expected '=' in let declaration", diagnostics,
                     module_path)) {
      RecoverToStatementBoundary(cursor);
      return stmt;
    }

    stmt.expr = ParseExpression(cursor, diagnostics, module_path, 1);
    if (!ExpectToken(cursor, TokenType::Semicolon, "E_PARSE_LET_SEMI", "Expected ';' after let declaration",
                     diagnostics, module_path)) {
      RecoverToStatementBoundary(cursor);
    }
    return stmt;
  }

  if (cursor.Match(TokenType::KeywordIf)) {
    Statement stmt;
    stmt.kind = Statement::Kind::If;
    stmt.expr = ParseExpression(cursor, diagnostics, module_path, 1);
    stmt.then_body = ParseBlock(cursor, diagnostics, module_path);
    if (cursor.Match(TokenType::KeywordElse)) {
      stmt.else_body = ParseBlock(cursor, diagnostics, module_path);
    }
    return stmt;
  }

  if (cursor.Match(TokenType::KeywordWhile)) {
    Statement stmt;
    stmt.kind = Statement::Kind::While;
    stmt.expr = ParseExpression(cursor, diagnostics, module_path, 1);
    stmt.body = ParseBlock(cursor, diagnostics, module_path);
    return stmt;
  }

  if (cursor.Match(TokenType::KeywordBreak)) {
    Statement stmt;
    stmt.kind = Statement::Kind::Break;
    if (!ExpectToken(cursor, TokenType::Semicolon, "E_PARSE_BREAK_SEMI", "Expected ';' after break", diagnostics,
                     module_path)) {
      RecoverToStatementBoundary(cursor);
    }
    return stmt;
  }

  if (cursor.Match(TokenType::KeywordContinue)) {
    Statement stmt;
    stmt.kind = Statement::Kind::Continue;
    if (!ExpectToken(cursor, TokenType::Semicolon, "E_PARSE_CONTINUE_SEMI", "Expected ';' after continue", diagnostics,
                     module_path)) {
      RecoverToStatementBoundary(cursor);
    }
    return stmt;
  }

  if (cursor.Peek().type == TokenType::Identifier && cursor.Peek(1).type == TokenType::Equal) {
    Statement stmt;
    stmt.kind = Statement::Kind::Assign;
    stmt.name = cursor.Advance().lexeme;
    (void)cursor.Advance();
    stmt.expr = ParseExpression(cursor, diagnostics, module_path, 1);
    if (!ExpectToken(cursor, TokenType::Semicolon, "E_PARSE_ASSIGN_SEMI", "Expected ';' after assignment",
                     diagnostics, module_path)) {
      RecoverToStatementBoundary(cursor);
    }
    return stmt;
  }

  if (cursor.Peek().type == TokenType::RBrace) {
    return std::nullopt;
  }

  Statement stmt;
  stmt.kind = Statement::Kind::Expr;
  stmt.expr = ParseExpression(cursor, diagnostics, module_path, 1);
  if (!ExpectToken(cursor, TokenType::Semicolon, "E_PARSE_STMT_SEMI", "Expected ';' after expression statement",
                   diagnostics, module_path)) {
    RecoverToStatementBoundary(cursor);
  }
  return stmt;
}

std::string Indent(const int spaces) {
  return std::string(static_cast<std::size_t>(spaces), ' ');
}

void DumpStatement(std::ostringstream& oss, const Statement& stmt, const int indent) {
  switch (stmt.kind) {
    case Statement::Kind::Return:
      oss << Indent(indent) << "return";
      if (stmt.expr) {
        oss << " " << DumpExpr(stmt.expr);
      }
      oss << "\n";
      return;
    case Statement::Kind::Expr:
      oss << Indent(indent) << "expr " << DumpExpr(stmt.expr) << "\n";
      return;
    case Statement::Kind::Assign:
      oss << Indent(indent) << stmt.name << " = " << DumpExpr(stmt.expr) << "\n";
      return;
    case Statement::Kind::Let:
      oss << Indent(indent) << "let " << stmt.name;
      if (!stmt.type_name.empty()) {
        oss << ": " << stmt.type_name;
      }
      if (stmt.expr) {
        oss << " = " << DumpExpr(stmt.expr);
      }
      oss << "\n";
      return;
    case Statement::Kind::Break:
      oss << Indent(indent) << "break\n";
      return;
    case Statement::Kind::Continue:
      oss << Indent(indent) << "continue\n";
      return;
    case Statement::Kind::If:
      oss << Indent(indent) << "if " << DumpExpr(stmt.expr) << "\n";
      oss << Indent(indent) << "{\n";
      for (const auto& then_stmt : stmt.then_body) {
        DumpStatement(oss, then_stmt, indent + 2);
      }
      oss << Indent(indent) << "}";
      if (!stmt.else_body.empty()) {
        oss << " else {\n";
        for (const auto& else_stmt : stmt.else_body) {
          DumpStatement(oss, else_stmt, indent + 2);
        }
        oss << Indent(indent) << "}";
      }
      oss << "\n";
      return;
    case Statement::Kind::While:
      oss << Indent(indent) << "while " << DumpExpr(stmt.expr) << "\n";
      oss << Indent(indent) << "{\n";
      for (const auto& body_stmt : stmt.body) {
        DumpStatement(oss, body_stmt, indent + 2);
      }
      oss << Indent(indent) << "}\n";
      return;
  }
}

}  // namespace

std::string ToString(const Visibility visibility) {
  switch (visibility) {
    case Visibility::Private:
      return "private";
    case Visibility::Internal:
      return "internal";
    case Visibility::Public:
      return "pub";
  }
  return "private";
}

std::string DumpAst(const AstModule& module) {
  std::ostringstream oss;
  oss << "module " << module.module_path << "\n";
  for (const auto& import : module.imports) {
    oss << "import " << JoinPath(import.path_segments);
    if (import.is_star) {
      oss << ".*";
    }
    if (!import.alias.empty()) {
      oss << " as " << import.alias;
    }
    oss << "\n";
  }
  for (const auto& function : module.functions) {
    oss << ToString(function.visibility) << " fn " << function.name << "(";
    for (std::size_t i = 0; i < function.params.size(); ++i) {
      if (i > 0) {
        oss << ", ";
      }
      oss << function.params[i].name << ": " << function.params[i].type;
    }
    oss << ") -> " << function.return_type << "\n";
    for (const auto& stmt : function.statements) {
      DumpStatement(oss, stmt, 2);
    }
  }
  return oss.str();
}

AstModule Parser::Parse(std::string module_path, const TokenStream& tokens,
                        snow::common::DiagnosticEngine& diagnostics) const {
  Cursor cursor(tokens);
  AstModule module;
  module.module_path = std::move(module_path);

  while (!cursor.AtEnd()) {
    if (cursor.Match(TokenType::KeywordImport)) {
      ImportDecl import;
      if (cursor.Peek().type != TokenType::Identifier) {
        diagnostics.Error("E_PARSE_IMPORT_PATH", "Expected module path after import", module.module_path,
                          cursor.Peek().range);
        cursor.Advance();
        continue;
      }
      import.path_segments.push_back(cursor.Advance().lexeme);
      while (cursor.Match(TokenType::Dot)) {
        if (cursor.Match(TokenType::Star)) {
          import.is_star = true;
          break;
        }
        if (cursor.Peek().type != TokenType::Identifier) {
          diagnostics.Error("E_PARSE_IMPORT_PATH", "Expected identifier in import path", module.module_path,
                            cursor.Peek().range);
          break;
        }
        import.path_segments.push_back(cursor.Advance().lexeme);
      }
      if (cursor.Match(TokenType::KeywordAs)) {
        if (cursor.Peek().type != TokenType::Identifier) {
          diagnostics.Error("E_PARSE_IMPORT_ALIAS", "Expected alias name after 'as'", module.module_path,
                            cursor.Peek().range);
        } else {
          import.alias = cursor.Advance().lexeme;
        }
      }
      (void)cursor.Match(TokenType::Semicolon);
      module.imports.push_back(std::move(import));
      continue;
    }

    const Visibility visibility = ParseVisibility(cursor);
    if (cursor.Match(TokenType::KeywordFn)) {
      FunctionDecl function;
      function.visibility = visibility;

      if (cursor.Peek().type != TokenType::Identifier) {
        diagnostics.Error("E_PARSE_FN_NAME", "Expected function name", module.module_path, cursor.Peek().range);
        cursor.Advance();
        continue;
      }
      function.name = cursor.Advance().lexeme;

      if (!cursor.Match(TokenType::LParen)) {
        diagnostics.Error("E_PARSE_FN_LPAREN", "Expected '(' after function name", module.module_path,
                          cursor.Peek().range);
      } else {
        while (cursor.Peek().type != TokenType::RParen && !cursor.AtEnd()) {
          ParamDecl param;
          if (cursor.Peek().type != TokenType::Identifier) {
            diagnostics.Error("E_PARSE_PARAM_NAME", "Expected parameter name", module.module_path,
                              cursor.Peek().range);
            break;
          }
          param.name = cursor.Advance().lexeme;
          if (!cursor.Match(TokenType::Colon)) {
            diagnostics.Error("E_PARSE_PARAM_COLON", "Expected ':' after parameter name", module.module_path,
                              cursor.Peek().range);
            break;
          }
          if (cursor.Peek().type != TokenType::Identifier) {
            diagnostics.Error("E_PARSE_PARAM_TYPE", "Expected parameter type", module.module_path,
                              cursor.Peek().range);
            break;
          }
          param.type = cursor.Advance().lexeme;
          function.params.push_back(std::move(param));
          if (!cursor.Match(TokenType::Comma)) {
            break;
          }
        }
        if (!cursor.Match(TokenType::RParen)) {
          diagnostics.Error("E_PARSE_FN_RPAREN", "Expected ')' after parameter list", module.module_path,
                            cursor.Peek().range);
        }
      }

      if (!cursor.Match(TokenType::Arrow)) {
        diagnostics.Error("E_PARSE_FN_ARROW", "Expected '->' return type marker", module.module_path,
                          cursor.Peek().range);
      }
      if (cursor.Peek().type != TokenType::Identifier) {
        diagnostics.Error("E_PARSE_FN_RET", "Expected return type", module.module_path, cursor.Peek().range);
      } else {
        function.return_type = cursor.Advance().lexeme;
      }

      if (cursor.Peek().type == TokenType::LBrace) {
        function.statements = ParseBlock(cursor, diagnostics, module.module_path);
      } else {
        (void)cursor.Match(TokenType::Semicolon);
      }

      if (function.return_type.empty()) {
        function.return_type = "i32";
      }
      module.functions.push_back(std::move(function));
      continue;
    }

    if (!cursor.AtEnd()) {
      diagnostics.Error("E_PARSE_TOPLEVEL", "Unexpected token at top-level", module.module_path, cursor.Peek().range);
      cursor.Advance();
    }
  }

  return module;
}

}  // namespace snow::frontend
