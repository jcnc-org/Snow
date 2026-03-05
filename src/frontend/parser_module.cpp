// Module: Top-level module parser orchestration.

#include "parser_internal.h"

#include <utility>

namespace snow::frontend::detail {

    AstModule ParseModule(std::string module_path, const TokenStream &tokens,
                          snow::common::DiagnosticEngine &diagnostics, std::string source_path) {
        Cursor cursor(tokens);
        AstModule module;
        module.module_path = std::move(module_path);
        module.source_path = source_path.empty() ? module.module_path : std::move(source_path);
        const std::string &diag_file = module.source_path;

        while (!cursor.AtEnd()) {
            if (cursor.Peek().type == TokenType::KeywordImport) {
                const auto import_start = cursor.Advance();
                ImportDecl import;
                import.range = import_start.range;
                if (cursor.Peek().type != TokenType::Identifier) {
                    diagnostics.Error("E_PARSE_IMPORT_PATH", "Expected module path after import", diag_file,
                                      cursor.Peek().range);
                    cursor.Advance();
                    continue;
                }
                auto segment = cursor.Advance();
                import.path_segments.push_back(segment.lexeme);
                import.range = MergeRange(import.range, segment.range);
                while (cursor.Match(TokenType::Dot)) {
                    if (cursor.Match(TokenType::Star)) {
                        import.is_star = true;
                        import.range = MergeRange(import.range, cursor.Peek().range);
                        break;
                    }
                    if (cursor.Peek().type != TokenType::Identifier) {
                        diagnostics.Error("E_PARSE_IMPORT_PATH", "Expected identifier in import path", diag_file,
                                          cursor.Peek().range);
                        break;
                    }
                    auto next = cursor.Advance();
                    import.path_segments.push_back(next.lexeme);
                    import.range = MergeRange(import.range, next.range);
                }
                if (cursor.Match(TokenType::KeywordAs)) {
                    if (cursor.Peek().type != TokenType::Identifier) {
                        diagnostics.Error("E_PARSE_IMPORT_ALIAS", "Expected alias name after 'as'", diag_file,
                                          cursor.Peek().range);
                    } else {
                        auto alias = cursor.Advance();
                        import.alias = alias.lexeme;
                        import.range = MergeRange(import.range, alias.range);
                    }
                }
                if (cursor.Peek().type == TokenType::Semicolon) {
                    import.range = MergeRange(import.range, cursor.Peek().range);
                }
                (void) cursor.Match(TokenType::Semicolon);
                module.imports.push_back(std::move(import));
                continue;
            }

            const auto item_start = cursor.Peek().range;
            const Visibility visibility = ParseVisibility(cursor);
            if (cursor.Match(TokenType::KeywordFn)) {
                FunctionDecl function;
                function.visibility = visibility;
                function.range = item_start;

                if (cursor.Peek().type != TokenType::Identifier) {
                    diagnostics.Error("E_PARSE_FN_NAME", "Expected function name", diag_file, cursor.Peek().range);
                    cursor.Advance();
                    continue;
                }
                auto fn_name = cursor.Advance();
                function.name = fn_name.lexeme;
                function.range = MergeRange(function.range, fn_name.range);

                if (!cursor.Match(TokenType::LParen)) {
                    diagnostics.Error("E_PARSE_FN_LPAREN", "Expected '(' after function name", diag_file,
                                      cursor.Peek().range);
                } else {
                    while (cursor.Peek().type != TokenType::RParen && !cursor.AtEnd()) {
                        ParamDecl param;
                        if (cursor.Peek().type != TokenType::Identifier) {
                            diagnostics.Error("E_PARSE_PARAM_NAME", "Expected parameter name", diag_file,
                                              cursor.Peek().range);
                            break;
                        }
                        param.name = cursor.Advance().lexeme;
                        if (!cursor.Match(TokenType::Colon)) {
                            diagnostics.Error("E_PARSE_PARAM_COLON", "Expected ':' after parameter name", diag_file,
                                              cursor.Peek().range);
                            break;
                        }
                        if (cursor.Peek().type != TokenType::Identifier) {
                            diagnostics.Error("E_PARSE_PARAM_TYPE", "Expected parameter type", diag_file,
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
                        diagnostics.Error("E_PARSE_FN_RPAREN", "Expected ')' after parameter list", diag_file,
                                          cursor.Peek().range);
                    }
                }

                if (!cursor.Match(TokenType::Arrow)) {
                    diagnostics.Error("E_PARSE_FN_ARROW", "Expected '->' return type marker", diag_file,
                                      cursor.Peek().range);
                }
                if (cursor.Peek().type != TokenType::Identifier) {
                    diagnostics.Error("E_PARSE_FN_RET", "Expected return type", diag_file, cursor.Peek().range);
                } else {
                    auto ret_token = cursor.Advance();
                    function.return_type = ret_token.lexeme;
                    function.range = MergeRange(function.range, ret_token.range);
                }

                if (cursor.Peek().type == TokenType::LBrace) {
                    function.statements = ParseBlock(cursor, diagnostics, diag_file);
                    if (!function.statements.empty()) {
                        function.range = MergeRange(function.range, function.statements.back().range);
                    }
                } else {
                    if (cursor.Peek().type == TokenType::Semicolon) {
                        function.range = MergeRange(function.range, cursor.Peek().range);
                    }
                    (void) cursor.Match(TokenType::Semicolon);
                }

                if (function.return_type.empty()) {
                    function.return_type = "i32";
                }
                module.functions.push_back(std::move(function));
                continue;
            }

            if (!cursor.AtEnd()) {
                diagnostics.Error("E_PARSE_TOPLEVEL", "Unexpected token at top-level", diag_file, cursor.Peek().range);
                cursor.Advance();
            }
        }

        return module;
    }

} // namespace snow::frontend::detail
