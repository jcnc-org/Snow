// Module: Semantic analysis for symbol/type/import/visibility checks.
// Contract: diagnostics are deterministic for a fixed AST and resolution context.

#include "snow/sema/sema.h"

#include <algorithm>
#include <sstream>
#include <unordered_map>
#include <unordered_set>
#include <utility>

namespace snow::sema {

    namespace {

        struct ExprTypeResult {
            std::string type;
            bool known = false;
        };

        struct StatementContext {
            const std::unordered_map<std::string, std::string> &function_return_types;
            const std::unordered_map<std::string, std::vector<std::string>> &function_param_types;
            const std::unordered_set<std::string> &ambiguous_function_names;
            std::string expected_return_type;
            std::string diag_file;
        };

        std::string JoinPath(const std::vector<std::string> &parts) {
            std::ostringstream oss;
            for (std::size_t i = 0; i < parts.size(); ++i) {
                if (i > 0) {
                    oss << ".";
                }
                oss << parts[i];
            }
            return oss.str();
        }

        bool HasRange(const snow::common::SourceRange &range) {
            return range.line > 0 && range.column > 0 && range.end_line > 0 && range.end_column > 0;
        }

        snow::common::SourceRange NormalizeRange(const snow::common::SourceRange &range) {
            if (HasRange(range)) {
                return range;
            }
            return snow::common::SourceRange{1, 1, 1, 1};
        }

        bool IsNumericType(const std::string &type) { return type == "i32" || type == "i64"; }

        bool IsBooleanType(const std::string &type) { return type == "bool" || type == "i1"; }

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

        bool IsTypeCompatible(const std::string &expected, const std::string &actual) {
            if (expected == actual) {
                return true;
            }
            if (IsBooleanType(expected) && IsBooleanType(actual)) {
                return true;
            }
            return false;
        }

        bool IsImportedByStar(const std::string &module_path, const std::string &prefix) {
            if (module_path == prefix) {
                return true;
            }
            if (module_path.size() <= prefix.size()) {
                return false;
            }
            if (module_path.compare(0, prefix.size(), prefix) != 0) {
                return false;
            }
            return module_path[prefix.size()] == '.';
        }

        std::string JoinModules(const std::vector<snow::common::FunctionSignature> &signatures) {
            std::vector<std::string> modules;
            modules.reserve(signatures.size());
            for (const auto &signature: signatures) {
                modules.push_back(signature.module_path);
            }
            std::sort(modules.begin(), modules.end());
            modules.erase(std::unique(modules.begin(), modules.end()), modules.end());

            std::ostringstream oss;
            for (std::size_t i = 0; i < modules.size(); ++i) {
                if (i > 0) {
                    oss << ", ";
                }
                oss << modules[i];
            }
            return oss.str();
        }

        ExprTypeResult
        InferExprType(const std::shared_ptr<snow::frontend::Expr> &expr,
                      const std::unordered_map<std::string, std::string> &symbol_types,
                      const std::unordered_map<std::string, std::string> &function_return_types,
                      const std::unordered_map<std::string, std::vector<std::string>> &function_param_types,
                      const std::unordered_set<std::string> &ambiguous_function_names, const std::string &diag_file,
                      snow::common::DiagnosticEngine &diagnostics) {
            if (!expr) {
                return ExprTypeResult{.type = "unknown", .known = false};
            }

            switch (expr->kind) {
                case snow::frontend::Expr::Kind::Number:
                    return ExprTypeResult{.type = "i32", .known = true};

                case snow::frontend::Expr::Kind::Identifier: {
                    const auto it = symbol_types.find(expr->value);
                    if (it == symbol_types.end()) {
                        return ExprTypeResult{.type = "unknown", .known = false};
                    }
                    return ExprTypeResult{.type = it->second, .known = true};
                }

                case snow::frontend::Expr::Kind::Call: {
                    if (ambiguous_function_names.contains(expr->value)) {
                        diagnostics.Error("AmbiguousSymbol",
                                          "Call target '" + expr->value + "' is ambiguous across imported modules",
                                          diag_file, NormalizeRange(expr->range),
                                          "Use alias import (as ...) or qualified module path");
                    }

                    const auto sig_it = function_param_types.find(expr->value);
                    if (sig_it == function_param_types.end() && !ambiguous_function_names.contains(expr->value)) {
                        diagnostics.Error("E_SEMA_CALL_UNDEFINED", "Call target is not defined: " + expr->value,
                                          diag_file, NormalizeRange(expr->range));
                    } else if (sig_it != function_param_types.end() && sig_it->second.size() != expr->args.size()) {
                        diagnostics.Error("E_SEMA_CALL_ARITY",
                                          "Call argument count mismatch for function '" + expr->value + "'", diag_file,
                                          NormalizeRange(expr->range));
                    }

                    for (const auto &arg: expr->args) {
                        (void) InferExprType(arg, symbol_types, function_return_types, function_param_types,
                                             ambiguous_function_names, diag_file, diagnostics);
                    }
                    if (sig_it != function_param_types.end()) {
                        const std::size_t count = std::min(sig_it->second.size(), expr->args.size());
                        for (std::size_t i = 0; i < count; ++i) {
                            const ExprTypeResult arg_type = InferExprType(
                                    expr->args[i], symbol_types, function_return_types, function_param_types,
                                    ambiguous_function_names, diag_file, diagnostics);
                            if (arg_type.known && !IsTypeCompatible(sig_it->second[i], arg_type.type)) {
                                diagnostics.Error("E_SEMA_CALL_ARG_TYPE",
                                                  "Call argument type mismatch at index " + std::to_string(i) +
                                                          " for function '" + expr->value + "'",
                                                  diag_file,
                                                  expr->args[i] ? NormalizeRange(expr->args[i]->range)
                                                                : NormalizeRange(expr->range));
                            }
                        }
                    }
                    const auto it = function_return_types.find(expr->value);
                    if (it == function_return_types.end()) {
                        return ExprTypeResult{.type = "unknown", .known = false};
                    }
                    return ExprTypeResult{.type = it->second, .known = true};
                }

                case snow::frontend::Expr::Kind::Binary: {
                    const ExprTypeResult lhs =
                            InferExprType(expr->lhs, symbol_types, function_return_types, function_param_types,
                                          ambiguous_function_names, diag_file, diagnostics);
                    const ExprTypeResult rhs =
                            InferExprType(expr->rhs, symbol_types, function_return_types, function_param_types,
                                          ambiguous_function_names, diag_file, diagnostics);

                    if (expr->op == snow::frontend::BinaryOp::Mod) {
                        diagnostics.Error("E_SEMA_UNSUPPORTED_OP", "operator '%' is not supported in Snow v1 MVP",
                                          diag_file, NormalizeRange(expr->range));
                        return ExprTypeResult{.type = "unknown", .known = false};
                    }

                    if (IsComparisonOp(expr->op)) {
                        if (lhs.known && rhs.known && lhs.type != rhs.type) {
                            diagnostics.Error("E_SEMA_EXPR_TYPE", "comparison operands have incompatible types",
                                              diag_file, NormalizeRange(expr->range));
                        }
                        return ExprTypeResult{.type = "bool", .known = true};
                    }

                    if (IsArithmeticOp(expr->op)) {
                        if (lhs.known && !IsNumericType(lhs.type)) {
                            diagnostics.Error("E_SEMA_EXPR_TYPE", "left arithmetic operand is not numeric", diag_file,
                                              expr->lhs ? NormalizeRange(expr->lhs->range)
                                                        : NormalizeRange(expr->range));
                        }
                        if (rhs.known && !IsNumericType(rhs.type)) {
                            diagnostics.Error("E_SEMA_EXPR_TYPE", "right arithmetic operand is not numeric", diag_file,
                                              expr->rhs ? NormalizeRange(expr->rhs->range)
                                                        : NormalizeRange(expr->range));
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

        void AnalyzeStatements(const std::vector<snow::frontend::Statement> &statements,
                               std::unordered_map<std::string, std::string> &symbol_types,
                               const StatementContext &context, const bool inside_loop,
                               snow::common::DiagnosticEngine &diagnostics) {
            for (const auto &stmt: statements) {
                const snow::common::SourceRange stmt_range = NormalizeRange(stmt.range);
                switch (stmt.kind) {
                    case snow::frontend::Statement::Kind::Return: {
                        if (!stmt.expr) {
                            diagnostics.Error("E_SEMA_RET_MISSING", "return statement requires a value in Snow v1",
                                              context.diag_file, stmt_range);
                            break;
                        }
                        const ExprTypeResult return_expr_type = InferExprType(
                                stmt.expr, symbol_types, context.function_return_types, context.function_param_types,
                                context.ambiguous_function_names, context.diag_file, diagnostics);
                        if (return_expr_type.known &&
                            !IsTypeCompatible(context.expected_return_type, return_expr_type.type)) {
                            diagnostics.Error("E_SEMA_RET_TYPE",
                                              "Return expression type '" + return_expr_type.type +
                                                      "' does not match function return type '" +
                                                      context.expected_return_type + "'",
                                              context.diag_file, NormalizeRange(stmt.expr->range));
                        }
                        break;
                    }

                    case snow::frontend::Statement::Kind::Expr:
                        (void) InferExprType(stmt.expr, symbol_types, context.function_return_types,
                                             context.function_param_types, context.ambiguous_function_names,
                                             context.diag_file, diagnostics);
                        break;

                    case snow::frontend::Statement::Kind::Assign: {
                        if (stmt.name.empty() || !symbol_types.contains(stmt.name)) {
                            diagnostics.Error("E_SEMA_ASSIGN_UNDEFINED",
                                              "Assignment target is not defined: " + stmt.name, context.diag_file,
                                              stmt_range);
                            break;
                        }
                        const ExprTypeResult assigned = InferExprType(
                                stmt.expr, symbol_types, context.function_return_types, context.function_param_types,
                                context.ambiguous_function_names, context.diag_file, diagnostics);
                        if (assigned.known && !IsTypeCompatible(symbol_types[stmt.name], assigned.type)) {
                            diagnostics.Error("E_SEMA_ASSIGN_TYPE",
                                              "Cannot assign value of type '" + assigned.type + "' to '" + stmt.name +
                                                      "' of type '" + symbol_types[stmt.name] + "'",
                                              context.diag_file, stmt_range);
                        }
                        break;
                    }

                    case snow::frontend::Statement::Kind::Let: {
                        if (stmt.name.empty()) {
                            diagnostics.Error("E_SEMA_LET_NAME", "let statement missing variable name",
                                              context.diag_file, stmt_range);
                            break;
                        }
                        if (symbol_types.contains(stmt.name)) {
                            diagnostics.Error("E_SEMA_DUP_LOCAL", "Duplicate local symbol: " + stmt.name,
                                              context.diag_file, stmt_range);
                            break;
                        }
                        const ExprTypeResult init_type = InferExprType(
                                stmt.expr, symbol_types, context.function_return_types, context.function_param_types,
                                context.ambiguous_function_names, context.diag_file, diagnostics);
                        if (!stmt.type_name.empty()) {
                            if (init_type.known && !IsTypeCompatible(stmt.type_name, init_type.type)) {
                                diagnostics.Error("E_SEMA_LET_TYPE",
                                                  "Initializer type '" + init_type.type +
                                                          "' does not match declared let type '" + stmt.type_name + "'",
                                                  context.diag_file, stmt_range);
                            }
                            symbol_types[stmt.name] = stmt.type_name;
                        } else {
                            symbol_types[stmt.name] = init_type.known ? init_type.type : "i32";
                        }
                        break;
                    }

                    case snow::frontend::Statement::Kind::If: {
                        const ExprTypeResult cond_type = InferExprType(
                                stmt.expr, symbol_types, context.function_return_types, context.function_param_types,
                                context.ambiguous_function_names, context.diag_file, diagnostics);
                        if (cond_type.known && !IsBooleanType(cond_type.type)) {
                            diagnostics.Error("E_SEMA_IF_COND_TYPE", "if condition must be bool/i1", context.diag_file,
                                              stmt_range);
                        }

                        auto then_symbols = symbol_types;
                        AnalyzeStatements(stmt.then_body, then_symbols, context, inside_loop, diagnostics);
                        auto else_symbols = symbol_types;
                        AnalyzeStatements(stmt.else_body, else_symbols, context, inside_loop, diagnostics);
                        break;
                    }

                    case snow::frontend::Statement::Kind::While: {
                        const ExprTypeResult cond_type = InferExprType(
                                stmt.expr, symbol_types, context.function_return_types, context.function_param_types,
                                context.ambiguous_function_names, context.diag_file, diagnostics);
                        if (cond_type.known && !IsBooleanType(cond_type.type)) {
                            diagnostics.Error("E_SEMA_WHILE_COND_TYPE", "while condition must be bool/i1",
                                              context.diag_file, stmt_range);
                        }
                        auto loop_symbols = symbol_types;
                        AnalyzeStatements(stmt.body, loop_symbols, context, true, diagnostics);
                        break;
                    }

                    case snow::frontend::Statement::Kind::Break:
                        if (!inside_loop) {
                            diagnostics.Error("E_SEMA_BREAK_OUTSIDE_LOOP", "break can only appear inside while loop",
                                              context.diag_file, stmt_range);
                        }
                        break;

                    case snow::frontend::Statement::Kind::Continue:
                        if (!inside_loop) {
                            diagnostics.Error("E_SEMA_CONTINUE_OUTSIDE_LOOP",
                                              "continue can only appear inside while loop", context.diag_file,
                                              stmt_range);
                        }
                        break;
                }
            }
        }

    } // namespace

    SemaModule
    SemanticAnalyzer::Analyze(const snow::frontend::AstModule &ast_module, snow::common::DiagnosticEngine &diagnostics,
                              const std::vector<snow::common::FunctionSignature> &available_functions) const {
        SemaModule sema;
        sema.ast = ast_module;
        const std::string diag_file = ast_module.source_path.empty() ? ast_module.module_path : ast_module.source_path;

        std::unordered_map<std::string, std::string> import_owner;
        std::unordered_map<std::string, snow::common::SourceRange> import_name_ranges;
        std::unordered_set<std::string> imported_modules;
        std::vector<std::string> star_import_prefixes;
        for (const auto &import: ast_module.imports) {
            ResolvedImport resolved;
            resolved.canonical_path = JoinPath(import.path_segments);
            resolved.alias = import.alias;
            resolved.is_star = import.is_star;

            if (import.is_star) {
                diagnostics.Warning("W_STAR_IMPORT_DISCOURAGED", "star import is discouraged", diag_file,
                                    NormalizeRange(import.range),
                                    "Use explicit import or alias import for stable name resolution");
                resolved.unqualified_name = "";
                if (!resolved.canonical_path.empty()) {
                    star_import_prefixes.push_back(resolved.canonical_path);
                }
            } else if (!import.alias.empty()) {
                resolved.unqualified_name = import.alias;
            } else if (!import.path_segments.empty()) {
                resolved.unqualified_name = import.path_segments.back();
            }

            if (!resolved.canonical_path.empty() && !import.is_star) {
                imported_modules.insert(resolved.canonical_path);
            }

            if (!resolved.unqualified_name.empty()) {
                const auto it = import_owner.find(resolved.unqualified_name);
                if (it != import_owner.end() && it->second != resolved.canonical_path) {
                    diagnostics.Error("AmbiguousSymbol",
                                      "Unqualified symbol conflict for import name '" + resolved.unqualified_name + "'",
                                      diag_file, NormalizeRange(import.range),
                                      "Use alias import (as ...) or qualified module path");
                } else {
                    import_owner.emplace(resolved.unqualified_name, resolved.canonical_path);
                    import_name_ranges.emplace(resolved.unqualified_name, NormalizeRange(import.range));
                }
            }

            sema.imports.push_back(std::move(resolved));
        }

        std::unordered_map<std::string, bool> symbol_seen;
        std::unordered_map<std::string, std::string> function_return_types;
        std::unordered_map<std::string, std::vector<std::string>> function_param_types;
        for (const auto &function: ast_module.functions) {
            function_return_types[function.name] = function.return_type.empty() ? "i32" : function.return_type;
            std::vector<std::string> params;
            params.reserve(function.params.size());
            for (const auto &param: function.params) {
                params.push_back(param.type);
            }
            function_param_types[function.name] = std::move(params);
        }

        std::unordered_map<std::string, std::vector<snow::common::FunctionSignature>> imported_candidates;
        for (const auto &signature: available_functions) {
            if (signature.module_path.empty() || signature.source_name.empty()) {
                continue;
            }
            if (signature.module_path == ast_module.module_path) {
                continue;
            }

            bool is_visible_import = imported_modules.contains(signature.module_path);
            if (!is_visible_import) {
                for (const auto &prefix: star_import_prefixes) {
                    if (IsImportedByStar(signature.module_path, prefix)) {
                        is_visible_import = true;
                        break;
                    }
                }
            }
            if (!is_visible_import) {
                continue;
            }

            imported_candidates[signature.source_name].push_back(signature);
        }

        std::unordered_set<std::string> ambiguous_function_names;
        std::vector<std::string> imported_names;
        imported_names.reserve(imported_candidates.size());
        for (const auto &[name, _]: imported_candidates) {
            imported_names.push_back(name);
        }
        std::sort(imported_names.begin(), imported_names.end());

        for (const auto &name: imported_names) {
            auto candidates = imported_candidates[name];
            std::sort(candidates.begin(), candidates.end(), [](const auto &lhs, const auto &rhs) {
                if (lhs.module_path != rhs.module_path) {
                    return lhs.module_path < rhs.module_path;
                }
                return lhs.mangled_name < rhs.mangled_name;
            });
            candidates.erase(std::unique(candidates.begin(), candidates.end(),
                                         [](const auto &lhs, const auto &rhs) {
                                             return lhs.module_path == rhs.module_path &&
                                                    lhs.mangled_name == rhs.mangled_name;
                                         }),
                             candidates.end());

            if (candidates.size() > 1) {
                ambiguous_function_names.insert(name);
                const auto range_it = import_name_ranges.find(name);
                const auto range =
                        range_it == import_name_ranges.end() ? snow::common::SourceRange{1, 1, 1, 1} : range_it->second;
                diagnostics.Error("AmbiguousSymbol", "Ambiguous imported function name '" + name + "'", diag_file,
                                  range, "Conflicting modules: " + JoinModules(candidates));
                continue;
            }

            const auto &resolved = candidates.front();
            if (function_return_types.contains(name)) {
                continue;
            }

            function_return_types[name] = resolved.return_type.empty() ? "i32" : resolved.return_type;
            function_param_types[name] = resolved.param_types;
            sema.resolved_external_functions.push_back(resolved);
        }

        std::sort(sema.resolved_external_functions.begin(), sema.resolved_external_functions.end(),
                  [](const auto &lhs, const auto &rhs) {
                      if (lhs.source_name != rhs.source_name) {
                          return lhs.source_name < rhs.source_name;
                      }
                      if (lhs.module_path != rhs.module_path) {
                          return lhs.module_path < rhs.module_path;
                      }
                      return lhs.mangled_name < rhs.mangled_name;
                  });

        for (const auto &function: ast_module.functions) {
            if (symbol_seen.contains(function.name)) {
                diagnostics.Error("E_SEMA_DUP_SYMBOL", "Duplicate symbol in module: " + function.name, diag_file,
                                  NormalizeRange(function.range));
                continue;
            }
            symbol_seen[function.name] = true;
            sema.symbols.push_back(function.name);

            std::unordered_map<std::string, std::string> symbol_types;
            for (const auto &param: function.params) {
                symbol_types[param.name] = param.type;
            }

            StatementContext context{
                    .function_return_types = function_return_types,
                    .function_param_types = function_param_types,
                    .ambiguous_function_names = ambiguous_function_names,
                    .expected_return_type = function.return_type.empty() ? "i32" : function.return_type,
                    .diag_file = diag_file,
            };
            AnalyzeStatements(function.statements, symbol_types, context, false, diagnostics);
        }

        return sema;
    }

    SemaModule SemanticAnalyzer::Analyze(const snow::frontend::AstModule &ast_module,
                                         snow::common::DiagnosticEngine &diagnostics) const {
        static const std::vector<snow::common::FunctionSignature> kEmptyFunctions;
        return Analyze(ast_module, diagnostics, kEmptyFunctions);
    }

    std::string DumpSema(const SemaModule &module) {
        std::ostringstream oss;
        oss << "sema module " << module.ast.module_path << "\n";
        oss << "symbols:\n";
        for (const auto &symbol: module.symbols) {
            oss << "  - " << symbol << "\n";
        }

        oss << "imports:\n";
        for (const auto &import: module.imports) {
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
        oss << "external-functions:\n";
        for (const auto &function: module.resolved_external_functions) {
            oss << "  - " << function.source_name << " => " << function.mangled_name << " from " << function.module_path
                << "\n";
        }
        oss << "resolution-order: local -> current module -> imported modules\n";
        return oss.str();
    }

} // namespace snow::sema
