// Module: Statement and block parsing.

#include "parser_internal.h"

#include <iterator>
#include <utility>

namespace snow::frontend::detail {

    std::vector<Statement> ParseBlock(Cursor &cursor, snow::common::DiagnosticEngine &diagnostics,
                                      const std::string &module_path) {
        std::vector<Statement> statements;
        if (!cursor.Match(TokenType::LBrace)) {
            diagnostics.Error("E_PARSE_BLOCK_LBRACE", "Expected '{' to start block", module_path, cursor.Peek().range);
            return statements;
        }

        while (!cursor.AtEnd() && cursor.Peek().type != TokenType::RBrace) {
            if (cursor.Peek().type == TokenType::LBrace) {
                auto nested = ParseBlock(cursor, diagnostics, module_path);
                statements.insert(statements.end(), std::make_move_iterator(nested.begin()),
                                  std::make_move_iterator(nested.end()));
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

    std::optional<Statement> ParseStatement(Cursor &cursor, snow::common::DiagnosticEngine &diagnostics,
                                            const std::string &module_path) {
        if (cursor.Peek().type == TokenType::KeywordReturn) {
            const auto start = cursor.Advance();
            Statement stmt;
            stmt.kind = Statement::Kind::Return;
            if (cursor.Peek().type != TokenType::Semicolon) {
                stmt.expr = ParseExpression(cursor, diagnostics, module_path, 1);
            }
            snow::common::SourceRange end_range = stmt.expr ? stmt.expr->range : start.range;
            if (cursor.Peek().type == TokenType::Semicolon) {
                end_range = cursor.Peek().range;
            }
            if (!ExpectToken(cursor, TokenType::Semicolon, "E_PARSE_RETURN_SEMI", "Expected ';' after return statement",
                             diagnostics, module_path)) {
                RecoverToStatementBoundary(cursor);
            }
            stmt.range = MergeRange(start.range, end_range);
            return stmt;
        }

        if (cursor.Peek().type == TokenType::KeywordLet) {
            const auto start = cursor.Advance();
            Statement stmt;
            stmt.kind = Statement::Kind::Let;

            if (cursor.Peek().type != TokenType::Identifier) {
                diagnostics.Error("E_PARSE_LET_NAME", "Expected variable name after 'let'", module_path,
                                  cursor.Peek().range);
                RecoverToStatementBoundary(cursor);
                stmt.range = start.range;
                return stmt;
            }
            stmt.name = cursor.Advance().lexeme;

            if (cursor.Match(TokenType::Colon) && cursor.Peek().type == TokenType::Identifier) {
                stmt.type_name = cursor.Advance().lexeme;
            }

            if (!ExpectToken(cursor, TokenType::Equal, "E_PARSE_LET_ASSIGN", "Expected '=' in let declaration",
                             diagnostics, module_path)) {
                RecoverToStatementBoundary(cursor);
                stmt.range = start.range;
                return stmt;
            }

            stmt.expr = ParseExpression(cursor, diagnostics, module_path, 1);
            snow::common::SourceRange end_range = stmt.expr ? stmt.expr->range : start.range;
            if (cursor.Peek().type == TokenType::Semicolon) {
                end_range = cursor.Peek().range;
            }
            if (!ExpectToken(cursor, TokenType::Semicolon, "E_PARSE_LET_SEMI", "Expected ';' after let declaration",
                             diagnostics, module_path)) {
                RecoverToStatementBoundary(cursor);
            }
            stmt.range = MergeRange(start.range, end_range);
            return stmt;
        }

        if (cursor.Peek().type == TokenType::KeywordIf) {
            const auto start = cursor.Advance();
            Statement stmt;
            stmt.kind = Statement::Kind::If;
            stmt.expr = ParseExpression(cursor, diagnostics, module_path, 1);
            stmt.then_body = ParseBlock(cursor, diagnostics, module_path);
            if (cursor.Match(TokenType::KeywordElse)) {
                stmt.else_body = ParseBlock(cursor, diagnostics, module_path);
            }
            snow::common::SourceRange end_range = stmt.expr ? stmt.expr->range : start.range;
            if (!stmt.else_body.empty()) {
                end_range = stmt.else_body.back().range;
            } else if (!stmt.then_body.empty()) {
                end_range = stmt.then_body.back().range;
            }
            stmt.range = MergeRange(start.range, end_range);
            return stmt;
        }

        if (cursor.Peek().type == TokenType::KeywordWhile) {
            const auto start = cursor.Advance();
            Statement stmt;
            stmt.kind = Statement::Kind::While;
            stmt.expr = ParseExpression(cursor, diagnostics, module_path, 1);
            stmt.body = ParseBlock(cursor, diagnostics, module_path);
            snow::common::SourceRange end_range = stmt.expr ? stmt.expr->range : start.range;
            if (!stmt.body.empty()) {
                end_range = stmt.body.back().range;
            }
            stmt.range = MergeRange(start.range, end_range);
            return stmt;
        }

        if (cursor.Peek().type == TokenType::KeywordBreak) {
            const auto start = cursor.Advance();
            Statement stmt;
            stmt.kind = Statement::Kind::Break;
            snow::common::SourceRange end_range = start.range;
            if (cursor.Peek().type == TokenType::Semicolon) {
                end_range = cursor.Peek().range;
            }
            if (!ExpectToken(cursor, TokenType::Semicolon, "E_PARSE_BREAK_SEMI", "Expected ';' after break",
                             diagnostics, module_path)) {
                RecoverToStatementBoundary(cursor);
            }
            stmt.range = MergeRange(start.range, end_range);
            return stmt;
        }

        if (cursor.Peek().type == TokenType::KeywordContinue) {
            const auto start = cursor.Advance();
            Statement stmt;
            stmt.kind = Statement::Kind::Continue;
            snow::common::SourceRange end_range = start.range;
            if (cursor.Peek().type == TokenType::Semicolon) {
                end_range = cursor.Peek().range;
            }
            if (!ExpectToken(cursor, TokenType::Semicolon, "E_PARSE_CONTINUE_SEMI", "Expected ';' after continue",
                             diagnostics, module_path)) {
                RecoverToStatementBoundary(cursor);
            }
            stmt.range = MergeRange(start.range, end_range);
            return stmt;
        }

        if (cursor.Peek().type == TokenType::Identifier && cursor.Peek(1).type == TokenType::Equal) {
            const auto start = cursor.Advance();
            Statement stmt;
            stmt.kind = Statement::Kind::Assign;
            stmt.name = start.lexeme;
            (void) cursor.Advance();
            stmt.expr = ParseExpression(cursor, diagnostics, module_path, 1);
            snow::common::SourceRange end_range = stmt.expr ? stmt.expr->range : start.range;
            if (cursor.Peek().type == TokenType::Semicolon) {
                end_range = cursor.Peek().range;
            }
            if (!ExpectToken(cursor, TokenType::Semicolon, "E_PARSE_ASSIGN_SEMI", "Expected ';' after assignment",
                             diagnostics, module_path)) {
                RecoverToStatementBoundary(cursor);
            }
            stmt.range = MergeRange(start.range, end_range);
            return stmt;
        }

        if (cursor.Peek().type == TokenType::RBrace) {
            return std::nullopt;
        }

        Statement stmt;
        stmt.kind = Statement::Kind::Expr;
        const auto start = cursor.Peek();
        stmt.expr = ParseExpression(cursor, diagnostics, module_path, 1);
        snow::common::SourceRange end_range = stmt.expr ? stmt.expr->range : start.range;
        if (cursor.Peek().type == TokenType::Semicolon) {
            end_range = cursor.Peek().range;
        }
        if (!ExpectToken(cursor, TokenType::Semicolon, "E_PARSE_STMT_SEMI", "Expected ';' after expression statement",
                         diagnostics, module_path)) {
            RecoverToStatementBoundary(cursor);
        }
        stmt.range = MergeRange(start.range, end_range);
        return stmt;
    }

} // namespace snow::frontend::detail
