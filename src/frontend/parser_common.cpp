// Module: Shared parser primitives and token-level helpers.

#include "parser_internal.h"

namespace snow::frontend::detail {

    std::string JoinPath(const std::vector<std::string> &segments) {
        std::ostringstream oss;
        for (std::size_t i = 0; i < segments.size(); ++i) {
            if (i > 0) {
                oss << ".";
            }
            oss << segments[i];
        }
        return oss.str();
    }

    snow::common::SourceRange MergeRange(const snow::common::SourceRange &lhs, const snow::common::SourceRange &rhs) {
        snow::common::SourceRange out = lhs;
        out.end_line = rhs.end_line;
        out.end_column = rhs.end_column;
        return out;
    }

    snow::common::SourceRange UnknownRange() { return snow::common::SourceRange{1, 1, 1, 1}; }

    Visibility ParseVisibility(Cursor &cursor) {
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

    void RecoverToStatementBoundary(Cursor &cursor) {
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

    bool ExpectToken(Cursor &cursor, const TokenType expected, const std::string &error_code,
                     const std::string &message, snow::common::DiagnosticEngine &diagnostics,
                     const std::string &module_path) {
        if (cursor.Match(expected)) {
            return true;
        }
        diagnostics.Error(error_code, message, module_path, cursor.Peek().range);
        return false;
    }

} // namespace snow::frontend::detail
