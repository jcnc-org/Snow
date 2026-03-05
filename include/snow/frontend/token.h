#pragma once

#include <string>
#include <vector>

#include "snow/common/diagnostic.h"

namespace snow::frontend {

    enum class TokenType {
        Identifier,
        Number,
        KeywordImport,
        KeywordAs,
        KeywordFn,
        KeywordPub,
        KeywordInternal,
        KeywordPrivate,
        KeywordReturn,
        KeywordIf,
        KeywordElse,
        KeywordWhile,
        KeywordBreak,
        KeywordContinue,
        KeywordLet,
        Arrow,
        Dot,
        Comma,
        Colon,
        Semicolon,
        Star,
        Plus,
        Minus,
        Slash,
        Percent,
        Equal,
        EqualEqual,
        Less,
        LessEqual,
        Greater,
        GreaterEqual,
        Bang,
        BangEqual,
        LParen,
        RParen,
        LBrace,
        RBrace,
        EndOfFile,
        Unknown,
    };

    struct Token {
        TokenType type = TokenType::Unknown;
        std::string lexeme;
        snow::common::SourceRange range;
    };

    std::string ToString(TokenType type);
    using TokenStream = std::vector<Token>;

} // namespace snow::frontend
