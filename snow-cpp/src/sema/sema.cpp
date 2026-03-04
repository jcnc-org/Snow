#include "snow/sema/sema.h"

#include <sstream>
#include <unordered_map>
#include <utility>

namespace snow::sema {

namespace {

struct ExprTypeResult {
  std::string type;
  bool known = false;
};

std::string JoinPath(const std::vector<std::string>& parts) {
  std::ostringstream oss;
  for (std::size_t i = 0; i < parts.size(); ++i) {
    if (i > 0) {
      oss << ".";
    }
    oss << parts[i];
  }
  return oss.str();
}

bool IsNumericType(const std::string& type) {
  return type == "i32" || type == "i64";
}

bool IsBooleanType(const std::string& type) {
  return type == "bool" || type == "i1";
}

bool IsComparisonOp(const snow::frontend::BinaryOp op) {
  switch (op) {
    case snow::frontend::BinaryOp::Eq:
    case snow::frontend::BinaryOp::Ne:
    case snow::frontend::BinaryOp::Lt:
    case snow::frontend::BinaryOp::Gt:
    case snow::frontend::BinaryOp::Le:
    case snow::frontend::BinaryOp::Ge:
      return true;
    default:
      return false;
  }
}

bool IsArithmeticOp(const snow::frontend::BinaryOp op) {
  switch (op) {
    case snow::frontend::BinaryOp::Add:
    case snow::frontend::BinaryOp::Sub:
    case snow::frontend::BinaryOp::Mul:
    case snow::frontend::BinaryOp::Div:
      return true;
    case snow::frontend::BinaryOp::Mod:
      return false;
    default:
      return false;
  }
}

ExprTypeResult InferExprType(const std::shared_ptr<snow::frontend::Expr>& expr,
                             const std::unordered_map<std::string, std::string>& symbol_types,
                             const std::unordered_map<std::string, std::string>& function_return_types,
                             const std::string& module_path, snow::common::DiagnosticEngine& diagnostics) {
  if (!expr) {
    return ExprTypeResult{.type = "unknown", .known = false};
  }

  switch (expr->kind) {
    case snow::frontend::Expr::Kind::Number:
      return ExprTypeResult{.type = "i32", .known = true};

    case snow::frontend::Expr::Kind::Identifier: {
      const auto it = symbol_types.find(expr->value);
      if (it == symbol_types.end()) {
        // Bootstrap stage: unresolved names may be functions/import members. Do not hard-fail yet.
        return ExprTypeResult{.type = "unknown", .known = false};
      }
      return ExprTypeResult{.type = it->second, .known = true};
    }

    case snow::frontend::Expr::Kind::Call: {
      for (const auto& arg : expr->args) {
        (void)InferExprType(arg, symbol_types, function_return_types, module_path, diagnostics);
      }
      const auto it = function_return_types.find(expr->value);
      if (it == function_return_types.end()) {
        return ExprTypeResult{.type = "unknown", .known = false};
      }
      return ExprTypeResult{.type = it->second, .known = true};
    }

    case snow::frontend::Expr::Kind::Binary: {
      const ExprTypeResult lhs = InferExprType(expr->lhs, symbol_types, function_return_types, module_path, diagnostics);
      const ExprTypeResult rhs = InferExprType(expr->rhs, symbol_types, function_return_types, module_path, diagnostics);

      if (expr->op == snow::frontend::BinaryOp::Mod) {
        diagnostics.Error("E_SEMA_UNSUPPORTED_OP", "operator '%' is not supported in Snow v1 MVP", module_path,
                          {0, 0, 0, 0});
        return ExprTypeResult{.type = "unknown", .known = false};
      }

      if (IsComparisonOp(expr->op)) {
        if (lhs.known && rhs.known && lhs.type != rhs.type) {
          diagnostics.Error("E_SEMA_EXPR_TYPE", "comparison operands have incompatible types", module_path,
                            {0, 0, 0, 0});
        }
        return ExprTypeResult{.type = "bool", .known = true};
      }

      if (IsArithmeticOp(expr->op)) {
        if (lhs.known && !IsNumericType(lhs.type)) {
          diagnostics.Error("E_SEMA_EXPR_TYPE", "left arithmetic operand is not numeric", module_path,
                            {0, 0, 0, 0});
        }
        if (rhs.known && !IsNumericType(rhs.type)) {
          diagnostics.Error("E_SEMA_EXPR_TYPE", "right arithmetic operand is not numeric", module_path,
                            {0, 0, 0, 0});
        }
        if (lhs.known && rhs.known) {
          if (lhs.type == "i64" || rhs.type == "i64") {
            return ExprTypeResult{.type = "i64", .known = true};
          }
          return ExprTypeResult{.type = "i32", .known = true};
        }
        if (lhs.known) {
          return lhs;
        }
        if (rhs.known) {
          return rhs;
        }
      }

      return ExprTypeResult{.type = "unknown", .known = false};
    }
  }

  return ExprTypeResult{.type = "unknown", .known = false};
}

bool IsReturnTypeCompatible(const std::string& expected, const std::string& actual) {
  if (expected == actual) {
    return true;
  }
  if (expected == "i64" && actual == "i32") {
    return true;
  }
  if (IsBooleanType(expected) && IsBooleanType(actual)) {
    return true;
  }
  return false;
}

}  // namespace

SemaModule SemanticAnalyzer::Analyze(const snow::frontend::AstModule& ast_module,
                                     snow::common::DiagnosticEngine& diagnostics) const {
  SemaModule sema;
  sema.ast = ast_module;

  std::unordered_map<std::string, std::string> import_owner;
  for (const auto& import : ast_module.imports) {
    ResolvedImport resolved;
    resolved.canonical_path = JoinPath(import.path_segments);
    resolved.alias = import.alias;
    resolved.is_star = import.is_star;

    if (import.is_star) {
      diagnostics.Warning("W_STAR_IMPORT_DISCOURAGED", "star import is discouraged", ast_module.module_path,
                          {0, 0, 0, 0}, "Use explicit import or alias import for stable name resolution");
      resolved.unqualified_name = "";
    } else if (!import.alias.empty()) {
      resolved.unqualified_name = import.alias;
    } else if (!import.path_segments.empty()) {
      resolved.unqualified_name = import.path_segments.back();
    }

    if (!resolved.unqualified_name.empty()) {
      const auto it = import_owner.find(resolved.unqualified_name);
      if (it != import_owner.end() && it->second != resolved.canonical_path) {
        diagnostics.Error(
            "AmbiguousSymbol",
            "Unqualified symbol conflict for import name '" + resolved.unqualified_name + "'", ast_module.module_path,
            {0, 0, 0, 0}, "Use alias import (as ...) or qualified module path");
      } else {
        import_owner.emplace(resolved.unqualified_name, resolved.canonical_path);
      }
    }

    sema.imports.push_back(std::move(resolved));
  }

  std::unordered_map<std::string, bool> symbol_seen;
  std::unordered_map<std::string, std::string> function_return_types;
  for (const auto& function : ast_module.functions) {
    function_return_types[function.name] = function.return_type.empty() ? "i32" : function.return_type;
  }

  for (const auto& function : ast_module.functions) {
    if (symbol_seen.contains(function.name)) {
      diagnostics.Error("E_SEMA_DUP_SYMBOL", "Duplicate symbol in module: " + function.name, ast_module.module_path,
                        {0, 0, 0, 0});
      continue;
    }
    symbol_seen[function.name] = true;
    sema.symbols.push_back(function.name);

    std::unordered_map<std::string, std::string> symbol_types;
    for (const auto& param : function.params) {
      symbol_types[param.name] = param.type;
    }

    if (function.return_expr) {
      const ExprTypeResult return_expr_type =
          InferExprType(function.return_expr, symbol_types, function_return_types, ast_module.module_path, diagnostics);
      if (return_expr_type.known &&
          !IsReturnTypeCompatible(function.return_type.empty() ? "i32" : function.return_type, return_expr_type.type)) {
        diagnostics.Error("E_SEMA_RET_TYPE",
                          "Return expression type '" + return_expr_type.type +
                              "' does not match function return type '" + function.return_type + "'",
                          ast_module.module_path, {0, 0, 0, 0});
      }
    }
  }

  return sema;
}

std::string DumpSema(const SemaModule& module) {
  std::ostringstream oss;
  oss << "sema module " << module.ast.module_path << "\n";
  oss << "symbols:\n";
  for (const auto& symbol : module.symbols) {
    oss << "  - " << symbol << "\n";
  }

  oss << "imports:\n";
  for (const auto& import : module.imports) {
    oss << "  - " << import.canonical_path;
    if (!import.alias.empty()) {
      oss << " as " << import.alias;
    }
    if (import.is_star) {
      oss << " (star)";
    }
    if (!import.unqualified_name.empty()) {
      oss << " unqualified=" << import.unqualified_name;
    }
    oss << "\n";
  }
  oss << "resolution-order: local -> current module -> imported modules\n";
  return oss.str();
}

}  // namespace snow::sema
