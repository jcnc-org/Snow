// Module: Expression parsing and expression debug rendering.

#include "parser_internal.h"

#include <utility>

namespace snow::frontend::detail {

    namespace {

        ExprPtr MakeNumberExpr(std::string value, const snow::common::SourceRange range) {
            auto expr = std::make_shared<Expr>();
            expr->kind = Expr::Kind::Number;
            expr->value = std::move(value);
            expr->range = range;
            return expr;
        }

        ExprPtr MakeIdentifierExpr(std::string value, const snow::common::SourceRange range) {
            auto expr = std::make_shared<Expr>();
            expr->kind = Expr::Kind::Identifier;
            expr->value = std::move(value);
            expr->range = range;
            return expr;
        }

        ExprPtr MakeCallExpr(std::string callee, std::vector<ExprPtr> args, const snow::common::SourceRange range) {
            auto expr = std::make_shared<Expr>();
            expr->kind = Expr::Kind::Call;
            expr->value = std::move(callee);
            expr->args = std::move(args);
            expr->range = range;
            return expr;
        }

        ExprPtr MakeBinaryExpr(BinaryOp op, ExprPtr lhs, ExprPtr rhs) {
            auto expr = std::make_shared<Expr>();
            expr->kind = Expr::Kind::Binary;
            expr->op = op;
            expr->range = lhs ? MergeRange(lhs->range, rhs ? rhs->range : lhs->range) : UnknownRange();
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

    } // namespace

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

    std::string DumpExpr(const ExprPtr &expr) {
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

    ExprPtr ParsePrimary(Cursor &cursor, snow::common::DiagnosticEngine &diagnostics, const std::string &module_path) {
        if (cursor.Peek().type == TokenType::Number) {
            const auto token = cursor.Advance();
            return MakeNumberExpr(token.lexeme, token.range);
        }

        if (cursor.Peek().type == TokenType::Identifier) {
            const auto ident_token = cursor.Advance();
            const std::string ident = ident_token.lexeme;

            if (!cursor.Match(TokenType::LParen)) {
                return MakeIdentifierExpr(ident, ident_token.range);
            }

            std::vector<ExprPtr> args;
            snow::common::SourceRange call_end_range = ident_token.range;
            if (cursor.Peek().type != TokenType::RParen) {
                while (!cursor.AtEnd()) {
                    auto arg = ParseExpression(cursor, diagnostics, module_path, 1);
                    if (arg) {
                        call_end_range = arg->range;
                    }
                    args.push_back(std::move(arg));
                    if (!cursor.Match(TokenType::Comma)) {
                        break;
                    }
                }
            }

            if (cursor.Peek().type != TokenType::RParen) {
                diagnostics.Error("E_PARSE_CALL_RPAREN", "Unclosed call expression", module_path, cursor.Peek().range);
            } else {
                call_end_range = cursor.Advance().range;
            }
            return MakeCallExpr(ident, std::move(args), MergeRange(ident_token.range, call_end_range));
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
            const auto bad = cursor.Advance();
            return MakeNumberExpr("0", bad.range);
        }
        return MakeNumberExpr("0", UnknownRange());
    }

    ExprPtr ParseExpression(Cursor &cursor, snow::common::DiagnosticEngine &diagnostics, const std::string &module_path,
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

} // namespace snow::frontend::detail
