// Module: AST dump rendering utilities.

#include "snow/frontend/parser.h"

#include <sstream>
#include <string>

#include "parser_internal.h"

namespace snow::frontend::detail {

    namespace {

        std::string Indent(const int spaces) { return std::string(static_cast<std::size_t>(spaces), ' '); }

    } // namespace

    void DumpStatement(std::ostringstream &oss, const Statement &stmt, const int indent) {
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
                for (const auto &then_stmt: stmt.then_body) {
                    DumpStatement(oss, then_stmt, indent + 2);
                }
                oss << Indent(indent) << "}";
                if (!stmt.else_body.empty()) {
                    oss << " else {\n";
                    for (const auto &else_stmt: stmt.else_body) {
                        DumpStatement(oss, else_stmt, indent + 2);
                    }
                    oss << Indent(indent) << "}";
                }
                oss << "\n";
                return;
            case Statement::Kind::While:
                oss << Indent(indent) << "while " << DumpExpr(stmt.expr) << "\n";
                oss << Indent(indent) << "{\n";
                for (const auto &body_stmt: stmt.body) {
                    DumpStatement(oss, body_stmt, indent + 2);
                }
                oss << Indent(indent) << "}\n";
                return;
        }
    }

} // namespace snow::frontend::detail

namespace snow::frontend {

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

    std::string DumpAst(const AstModule &module) {
        std::ostringstream oss;
        oss << "module " << module.module_path << "\n";
        for (const auto &import: module.imports) {
            oss << "import " << detail::JoinPath(import.path_segments);
            if (import.is_star) {
                oss << ".*";
            }
            if (!import.alias.empty()) {
                oss << " as " << import.alias;
            }
            oss << "\n";
        }
        for (const auto &function: module.functions) {
            oss << ToString(function.visibility) << " fn " << function.name << "(";
            for (std::size_t i = 0; i < function.params.size(); ++i) {
                if (i > 0) {
                    oss << ", ";
                }
                oss << function.params[i].name << ": " << function.params[i].type;
            }
            oss << ") -> " << function.return_type << "\n";
            for (const auto &stmt: function.statements) {
                detail::DumpStatement(oss, stmt, 2);
            }
        }
        return oss.str();
    }

} // namespace snow::frontend
