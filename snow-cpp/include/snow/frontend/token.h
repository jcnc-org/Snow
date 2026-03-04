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
  Less,
  Greater,
  Bang,
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

}  // namespace snow::frontend
