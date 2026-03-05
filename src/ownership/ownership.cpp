// Module: Ownership and lifetime fact extraction from sema AST.
// Invariant: move/drop facts are emitted in source order to keep diagnostics and lowering deterministic.

#include "snow/ownership/ownership.h"

#include <algorithm>
#include <sstream>
#include <unordered_map>
#include <unordered_set>
#include <utility>

namespace snow::ownership {

    namespace {

        struct SymbolState {
            std::string type_name;
            bool is_copy_type = false;
            bool moved = false;
            std::size_t fact_index = 0;
        };

        bool HasRange(const snow::common::SourceRange &range) {
            return range.line > 0 && range.column > 0 && range.end_line > 0 && range.end_column > 0;
        }

        snow::common::SourceRange NormalizeRange(const snow::common::SourceRange &range) {
            if (HasRange(range)) {
                return range;
            }
            return snow::common::SourceRange{1, 1, 1, 1};
        }

        bool IsCopyType(const std::string &type_name) {
            static const std::unordered_set<std::string> kCopyTypes = {
                    "bool", "i1", "i32", "i64", "f32", "f64",
            };
            return kCopyTypes.contains(type_name);
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
                default:
                    return false;
            }
        }

        std::string InferExprType(const std::shared_ptr<snow::frontend::Expr> &expr,
                                  const std::unordered_map<std::string, SymbolState> &symbols,
                                  const std::unordered_map<std::string, std::string> &function_return_types) {
            if (!expr) {
                return "i32";
            }

            switch (expr->kind) {
                case snow::frontend::Expr::Kind::Number:
                    return "i32";

                case snow::frontend::Expr::Kind::Identifier: {
                    const auto it = symbols.find(expr->value);
                    if (it != symbols.end()) {
                        return it->second.type_name;
                    }
                    return "unknown";
                }

                case snow::frontend::Expr::Kind::Call: {
                    const auto it = function_return_types.find(expr->value);
                    if (it != function_return_types.end()) {
                        return it->second;
                    }
                    return "unknown";
                }

                case snow::frontend::Expr::Kind::Binary:
                    if (IsComparisonOp(expr->op)) {
                        return "bool";
                    }
                    if (IsArithmeticOp(expr->op)) {
                        const std::string lhs = InferExprType(expr->lhs, symbols, function_return_types);
                        const std::string rhs = InferExprType(expr->rhs, symbols, function_return_types);
                        if (lhs == "i64" || rhs == "i64") {
                            return "i64";
                        }
                        return "i32";
                    }
                    return "unknown";
            }

            return "unknown";
        }

        void VisitExprOwnership(const std::shared_ptr<snow::frontend::Expr> &expr, const bool consume_non_copy,
                                std::unordered_map<std::string, SymbolState> &symbols,
                                const std::unordered_map<std::string, std::string> &function_return_types,
                                const std::string &diag_file, std::vector<OwnershipFact> &facts,
                                snow::common::DiagnosticEngine &diagnostics) {
            if (!expr) {
                return;
            }

            switch (expr->kind) {
                case snow::frontend::Expr::Kind::Number:
                    return;

                case snow::frontend::Expr::Kind::Identifier: {
                    const auto it = symbols.find(expr->value);
                    if (it == symbols.end() || it->second.is_copy_type) {
                        return;
                    }
                    SymbolState &state = symbols[expr->value];
                    if (state.moved) {
                        diagnostics.Error("E_OWNERSHIP_USE_AFTER_MOVE", "use after move: " + expr->value, diag_file,
                                          NormalizeRange(expr->range));
                        return;
                    }
                    if (consume_non_copy) {
                        state.moved = true;
                        if (state.fact_index < facts.size()) {
                            facts[state.fact_index].drop_at_exit = false;
                        }
                    }
                    return;
                }

                case snow::frontend::Expr::Kind::Call:
                    for (const auto &arg: expr->args) {
                        VisitExprOwnership(arg, true, symbols, function_return_types, diag_file, facts, diagnostics);
                    }
                    return;

                case snow::frontend::Expr::Kind::Binary:
                    VisitExprOwnership(expr->lhs, false, symbols, function_return_types, diag_file, facts, diagnostics);
                    VisitExprOwnership(expr->rhs, false, symbols, function_return_types, diag_file, facts, diagnostics);
                    return;
            }
        }

        void AnalyzeStatements(const std::vector<snow::frontend::Statement> &statements,
                               std::unordered_map<std::string, SymbolState> &symbols,
                               const std::unordered_map<std::string, std::string> &function_return_types,
                               const std::string &diag_file, std::vector<OwnershipFact> &facts,
                               snow::common::DiagnosticEngine &diagnostics);

        void AnalyzeIf(const snow::frontend::Statement &stmt, std::unordered_map<std::string, SymbolState> &symbols,
                       const std::unordered_map<std::string, std::string> &function_return_types,
                       const std::string &diag_file, std::vector<OwnershipFact> &facts,
                       snow::common::DiagnosticEngine &diagnostics) {
            VisitExprOwnership(stmt.expr, false, symbols, function_return_types, diag_file, facts, diagnostics);

            auto then_symbols = symbols;
            auto else_symbols = symbols;
            AnalyzeStatements(stmt.then_body, then_symbols, function_return_types, diag_file, facts, diagnostics);
            AnalyzeStatements(stmt.else_body, else_symbols, function_return_types, diag_file, facts, diagnostics);

            for (auto &kv: symbols) {
                const auto then_it = then_symbols.find(kv.first);
                const auto else_it = else_symbols.find(kv.first);
                const bool then_moved = then_it != then_symbols.end() ? then_it->second.moved : kv.second.moved;
                const bool else_moved = else_it != else_symbols.end() ? else_it->second.moved : kv.second.moved;
                kv.second.moved = then_moved || else_moved;
                if (kv.second.moved && kv.second.fact_index < facts.size()) {
                    facts[kv.second.fact_index].drop_at_exit = false;
                }
            }
        }

        void AnalyzeWhile(const snow::frontend::Statement &stmt, std::unordered_map<std::string, SymbolState> &symbols,
                          const std::unordered_map<std::string, std::string> &function_return_types,
                          const std::string &diag_file, std::vector<OwnershipFact> &facts,
                          snow::common::DiagnosticEngine &diagnostics) {
            VisitExprOwnership(stmt.expr, false, symbols, function_return_types, diag_file, facts, diagnostics);

            auto loop_symbols = symbols;
            AnalyzeStatements(stmt.body, loop_symbols, function_return_types, diag_file, facts, diagnostics);

            for (auto &kv: symbols) {
                const auto loop_it = loop_symbols.find(kv.first);
                if (loop_it != loop_symbols.end() && loop_it->second.moved) {
                    kv.second.moved = true;
                    if (kv.second.fact_index < facts.size()) {
                        facts[kv.second.fact_index].drop_at_exit = false;
                    }
                }
            }
        }

        void AnalyzeStatements(const std::vector<snow::frontend::Statement> &statements,
                               std::unordered_map<std::string, SymbolState> &symbols,
                               const std::unordered_map<std::string, std::string> &function_return_types,
                               const std::string &diag_file, std::vector<OwnershipFact> &facts,
                               snow::common::DiagnosticEngine &diagnostics) {
            for (const auto &stmt: statements) {
                switch (stmt.kind) {
                    case snow::frontend::Statement::Kind::Return:
                        VisitExprOwnership(stmt.expr, true, symbols, function_return_types, diag_file, facts,
                                           diagnostics);
                        break;

                    case snow::frontend::Statement::Kind::Expr:
                        VisitExprOwnership(stmt.expr, false, symbols, function_return_types, diag_file, facts,
                                           diagnostics);
                        break;

                    case snow::frontend::Statement::Kind::Assign:
                        VisitExprOwnership(stmt.expr, true, symbols, function_return_types, diag_file, facts,
                                           diagnostics);
                        if (!stmt.name.empty() && symbols.contains(stmt.name)) {
                            symbols[stmt.name].moved = false;
                            if (symbols[stmt.name].fact_index < facts.size() && !symbols[stmt.name].is_copy_type) {
                                facts[symbols[stmt.name].fact_index].drop_at_exit = true;
                            }
                        }
                        break;

                    case snow::frontend::Statement::Kind::Let: {
                        VisitExprOwnership(stmt.expr, true, symbols, function_return_types, diag_file, facts,
                                           diagnostics);
                        if (stmt.name.empty()) {
                            break;
                        }
                        const std::string type_name =
                                !stmt.type_name.empty() ? stmt.type_name
                                                        : InferExprType(stmt.expr, symbols, function_return_types);
                        const bool copy_type = IsCopyType(type_name);
                        const std::string key = stmt.name;
                        if (symbols.contains(key)) {
                            break;
                        }
                        const std::size_t fact_index = facts.size();
                        facts.push_back(OwnershipFact{
                                .symbol = "",
                                .name = stmt.name,
                                .type_name = type_name,
                                .is_copy_type = copy_type,
                                .drop_at_exit = !copy_type,
                                .declaration_index = fact_index,
                        });
                        symbols.emplace(key, SymbolState{
                                                     .type_name = type_name,
                                                     .is_copy_type = copy_type,
                                                     .moved = false,
                                                     .fact_index = fact_index,
                                             });
                        break;
                    }

                    case snow::frontend::Statement::Kind::If:
                        AnalyzeIf(stmt, symbols, function_return_types, diag_file, facts, diagnostics);
                        break;

                    case snow::frontend::Statement::Kind::While:
                        AnalyzeWhile(stmt, symbols, function_return_types, diag_file, facts, diagnostics);
                        break;

                    case snow::frontend::Statement::Kind::Break:
                    case snow::frontend::Statement::Kind::Continue:
                        break;
                }
            }
        }

    } // namespace

    OwnershipFacts OwnershipChecker::Check(const snow::sema::SemaModule &sema_module,
                                           snow::common::DiagnosticEngine &diagnostics) const {
        OwnershipFacts facts;
        const std::string diag_file =
                sema_module.ast.source_path.empty() ? sema_module.ast.module_path : sema_module.ast.source_path;

        std::unordered_map<std::string, std::string> function_return_types;
        for (const auto &function: sema_module.ast.functions) {
            function_return_types[function.name] = function.return_type.empty() ? "i32" : function.return_type;
        }
        for (const auto &external: sema_module.resolved_external_functions) {
            if (external.source_name.empty()) {
                continue;
            }
            if (!function_return_types.contains(external.source_name)) {
                function_return_types[external.source_name] =
                        external.return_type.empty() ? "i32" : external.return_type;
            }
        }

        for (const auto &function: sema_module.ast.functions) {
            if (function.return_type.empty()) {
                diagnostics.Error("E_OWNERSHIP_RET_TYPE", "Function missing return type: " + function.name, diag_file,
                                  NormalizeRange(function.range));
            }

            std::unordered_map<std::string, SymbolState> symbols;
            std::vector<OwnershipFact> fn_facts;

            for (const auto &param: function.params) {
                const bool copy_type = IsCopyType(param.type);
                const std::size_t fact_index = fn_facts.size();
                fn_facts.push_back(OwnershipFact{
                        .symbol = "",
                        .name = param.name,
                        .type_name = param.type,
                        .is_copy_type = copy_type,
                        .drop_at_exit = !copy_type,
                        .declaration_index = fact_index,
                });
                symbols[param.name] = SymbolState{
                        .type_name = param.type,
                        .is_copy_type = copy_type,
                        .moved = false,
                        .fact_index = fact_index,
                };
            }

            AnalyzeStatements(function.statements, symbols, function_return_types, diag_file, fn_facts, diagnostics);

            for (auto &fact: fn_facts) {
                fact.symbol = function.name + "::" + fact.name;
            }
            std::stable_sort(fn_facts.begin(), fn_facts.end(), [](const OwnershipFact &a, const OwnershipFact &b) {
                return a.declaration_index < b.declaration_index;
            });
            facts.facts.insert(facts.facts.end(), fn_facts.begin(), fn_facts.end());
        }

        return facts;
    }

    std::string DumpOwnership(const OwnershipFacts &facts) {
        std::ostringstream oss;
        oss << "ownership-facts\n";
        for (const auto &fact: facts.facts) {
            oss << "  - " << fact.symbol << " type=" << fact.type_name
                << " copy=" << (fact.is_copy_type ? "true" : "false")
                << " drop_at_exit=" << (fact.drop_at_exit ? "true" : "false") << "\n";
        }
        oss << "model: ownership + deterministic drop (no implicit GC)\n";
        return oss.str();
    }

} // namespace snow::ownership
