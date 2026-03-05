#pragma once

#include <memory>
#include <optional>
#include <sstream>
#include <string>
#include <vector>

#include "snow/common/diagnostic_engine.h"
#include "snow/frontend/ast.h"
#include "snow/frontend/token.h"

namespace snow::frontend::detail {

    class Cursor {
    public:
        explicit Cursor(const TokenStream &tokens) : tokens_(tokens) {}

        const Token &Peek(std::size_t offset = 0) const {
            const std::size_t idx = index_ + offset;
            if (idx >= tokens_.size()) {
                return tokens_.back();
            }
            return tokens_[idx];
        }

        const Token &Advance() {
            const Token &token = Peek();
            if (index_ < tokens_.size()) {
                ++index_;
            }
            return token;
        }

        [[nodiscard]] bool Match(TokenType type) {
            if (Peek().type != type) {
                return false;
            }
            Advance();
            return true;
        }

        [[nodiscard]] bool AtEnd() const { return Peek().type == TokenType::EndOfFile; }

    private:
        const TokenStream &tokens_;
        std::size_t index_ = 0;
    };

    using ExprPtr = std::shared_ptr<Expr>;

    std::string JoinPath(const std::vector<std::string> &segments);
    snow::common::SourceRange MergeRange(const snow::common::SourceRange &lhs, const snow::common::SourceRange &rhs);
    snow::common::SourceRange UnknownRange();
    Visibility ParseVisibility(Cursor &cursor);

    void RecoverToStatementBoundary(Cursor &cursor);
    bool ExpectToken(Cursor &cursor, TokenType expected, const std::string &error_code, const std::string &message,
                     snow::common::DiagnosticEngine &diagnostics, const std::string &module_path);

    std::string BinaryOpToString(BinaryOp op);
    std::string DumpExpr(const ExprPtr &expr);
    ExprPtr ParsePrimary(Cursor &cursor, snow::common::DiagnosticEngine &diagnostics, const std::string &module_path);
    ExprPtr ParseExpression(Cursor &cursor, snow::common::DiagnosticEngine &diagnostics, const std::string &module_path,
                            int min_precedence);

    std::optional<Statement> ParseStatement(Cursor &cursor, snow::common::DiagnosticEngine &diagnostics,
                                            const std::string &module_path);
    std::vector<Statement> ParseBlock(Cursor &cursor, snow::common::DiagnosticEngine &diagnostics,
                                      const std::string &module_path);

    void DumpStatement(std::ostringstream &oss, const Statement &stmt, int indent);

    AstModule ParseModule(std::string module_path, const TokenStream &tokens,
                          snow::common::DiagnosticEngine &diagnostics, std::string source_path);

} // namespace snow::frontend::detail
