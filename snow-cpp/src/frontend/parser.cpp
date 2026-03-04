#include "snow/frontend/parser.h"

#include <sstream>
#include <utility>

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
    oss << ") -> " << function.return_type;
    if (function.return_literal.has_value()) {
      oss << " ; return-literal=" << function.return_literal.value();
    }
    oss << "\n";
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

      if (cursor.Match(TokenType::LBrace)) {
        int depth = 1;
        while (!cursor.AtEnd() && depth > 0) {
          if (depth == 1 && cursor.Peek().type == TokenType::Identifier && cursor.Peek().lexeme == "return") {
            cursor.Advance();
            if (cursor.Peek().type == TokenType::Number && !function.return_literal.has_value()) {
              function.return_literal = cursor.Advance().lexeme;
            }
            continue;
          }
          if (cursor.Match(TokenType::LBrace)) {
            ++depth;
            continue;
          }
          if (cursor.Match(TokenType::RBrace)) {
            --depth;
            continue;
          }
          cursor.Advance();
        }
        if (depth != 0) {
          diagnostics.Error("E_PARSE_FN_BODY", "Unclosed function body", module.module_path, cursor.Peek().range);
        }
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
      diagnostics.Error("E_PARSE_TOPLEVEL", "Unexpected token at top-level", module.module_path,
                        cursor.Peek().range);
      cursor.Advance();
    }
  }

  return module;
}

}  // namespace snow::frontend
