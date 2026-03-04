#include "snow/frontend/lexer.h"

#include <cctype>
#include <unordered_map>

namespace snow::frontend {

namespace {

Token MakeToken(const TokenType type, const std::string& lexeme, const std::size_t line, const std::size_t column,
                const std::size_t end_line, const std::size_t end_column) {
  return Token{
      .type = type,
      .lexeme = lexeme,
      .range = snow::common::SourceRange{line, column, end_line, end_column},
  };
}

TokenType KeywordType(const std::string& lexeme) {
  static const std::unordered_map<std::string, TokenType> kKeywords = {
      {"import", TokenType::KeywordImport},
      {"as", TokenType::KeywordAs},
      {"fn", TokenType::KeywordFn},
      {"pub", TokenType::KeywordPub},
      {"internal", TokenType::KeywordInternal},
      {"private", TokenType::KeywordPrivate},
      {"return", TokenType::KeywordReturn},
      {"if", TokenType::KeywordIf},
      {"else", TokenType::KeywordElse},
      {"while", TokenType::KeywordWhile},
      {"break", TokenType::KeywordBreak},
      {"continue", TokenType::KeywordContinue},
      {"let", TokenType::KeywordLet},
  };
  const auto it = kKeywords.find(lexeme);
  if (it == kKeywords.end()) {
    return TokenType::Identifier;
  }
  return it->second;
}

}  // namespace

std::string ToString(const TokenType type) {
  switch (type) {
    case TokenType::Identifier:
      return "Identifier";
    case TokenType::Number:
      return "Number";
    case TokenType::KeywordImport:
      return "KeywordImport";
    case TokenType::KeywordAs:
      return "KeywordAs";
    case TokenType::KeywordFn:
      return "KeywordFn";
    case TokenType::KeywordPub:
      return "KeywordPub";
    case TokenType::KeywordInternal:
      return "KeywordInternal";
    case TokenType::KeywordPrivate:
      return "KeywordPrivate";
    case TokenType::KeywordReturn:
      return "KeywordReturn";
    case TokenType::KeywordIf:
      return "KeywordIf";
    case TokenType::KeywordElse:
      return "KeywordElse";
    case TokenType::KeywordWhile:
      return "KeywordWhile";
    case TokenType::KeywordBreak:
      return "KeywordBreak";
    case TokenType::KeywordContinue:
      return "KeywordContinue";
    case TokenType::KeywordLet:
      return "KeywordLet";
    case TokenType::Arrow:
      return "Arrow";
    case TokenType::Dot:
      return "Dot";
    case TokenType::Comma:
      return "Comma";
    case TokenType::Colon:
      return "Colon";
    case TokenType::Semicolon:
      return "Semicolon";
    case TokenType::Star:
      return "Star";
    case TokenType::Plus:
      return "Plus";
    case TokenType::Minus:
      return "Minus";
    case TokenType::Slash:
      return "Slash";
    case TokenType::Percent:
      return "Percent";
    case TokenType::Equal:
      return "Equal";
    case TokenType::EqualEqual:
      return "EqualEqual";
    case TokenType::Less:
      return "Less";
    case TokenType::LessEqual:
      return "LessEqual";
    case TokenType::Greater:
      return "Greater";
    case TokenType::GreaterEqual:
      return "GreaterEqual";
    case TokenType::Bang:
      return "Bang";
    case TokenType::BangEqual:
      return "BangEqual";
    case TokenType::LParen:
      return "LParen";
    case TokenType::RParen:
      return "RParen";
    case TokenType::LBrace:
      return "LBrace";
    case TokenType::RBrace:
      return "RBrace";
    case TokenType::EndOfFile:
      return "EndOfFile";
    case TokenType::Unknown:
      return "Unknown";
  }
  return "Unknown";
}

TokenStream Lexer::Tokenize(const snow::common::SourceFile& source, snow::common::DiagnosticEngine& diagnostics) const {
  TokenStream tokens;

  std::size_t i = 0;
  std::size_t line = 1;
  std::size_t column = 1;

  while (i < source.content.size()) {
    const char ch = source.content[i];

    if (ch == '\r') {
      ++i;
      continue;
    }

    if (ch == '\n') {
      ++i;
      ++line;
      column = 1;
      continue;
    }

    if (std::isspace(static_cast<unsigned char>(ch))) {
      ++i;
      ++column;
      continue;
    }

    if (ch == '/' && i + 1 < source.content.size() && source.content[i + 1] == '/') {
      i += 2;
      column += 2;
      while (i < source.content.size() && source.content[i] != '\n') {
        ++i;
        ++column;
      }
      continue;
    }

    const std::size_t start_line = line;
    const std::size_t start_col = column;

    if (std::isalpha(static_cast<unsigned char>(ch)) || ch == '_') {
      std::string lexeme;
      while (i < source.content.size()) {
        const char c = source.content[i];
        if (!std::isalnum(static_cast<unsigned char>(c)) && c != '_') {
          break;
        }
        lexeme.push_back(c);
        ++i;
        ++column;
      }
      tokens.push_back(MakeToken(KeywordType(lexeme), lexeme, start_line, start_col, line, column));
      continue;
    }

    if (std::isdigit(static_cast<unsigned char>(ch))) {
      std::string lexeme;
      while (i < source.content.size() && std::isdigit(static_cast<unsigned char>(source.content[i]))) {
        lexeme.push_back(source.content[i]);
        ++i;
        ++column;
      }
      tokens.push_back(MakeToken(TokenType::Number, lexeme, start_line, start_col, line, column));
      continue;
    }

    if (ch == '-' && i + 1 < source.content.size() && source.content[i + 1] == '>') {
      tokens.push_back(MakeToken(TokenType::Arrow, "->", start_line, start_col, line, column + 2));
      i += 2;
      column += 2;
      continue;
    }
    if (ch == '=' && i + 1 < source.content.size() && source.content[i + 1] == '=') {
      tokens.push_back(MakeToken(TokenType::EqualEqual, "==", start_line, start_col, line, column + 2));
      i += 2;
      column += 2;
      continue;
    }
    if (ch == '!' && i + 1 < source.content.size() && source.content[i + 1] == '=') {
      tokens.push_back(MakeToken(TokenType::BangEqual, "!=", start_line, start_col, line, column + 2));
      i += 2;
      column += 2;
      continue;
    }
    if (ch == '<' && i + 1 < source.content.size() && source.content[i + 1] == '=') {
      tokens.push_back(MakeToken(TokenType::LessEqual, "<=", start_line, start_col, line, column + 2));
      i += 2;
      column += 2;
      continue;
    }
    if (ch == '>' && i + 1 < source.content.size() && source.content[i + 1] == '=') {
      tokens.push_back(MakeToken(TokenType::GreaterEqual, ">=", start_line, start_col, line, column + 2));
      i += 2;
      column += 2;
      continue;
    }

    TokenType single_type = TokenType::Unknown;
    switch (ch) {
      case '.':
        single_type = TokenType::Dot;
        break;
      case ',':
        single_type = TokenType::Comma;
        break;
      case ':':
        single_type = TokenType::Colon;
        break;
      case ';':
        single_type = TokenType::Semicolon;
        break;
      case '*':
        single_type = TokenType::Star;
        break;
      case '+':
        single_type = TokenType::Plus;
        break;
      case '-':
        single_type = TokenType::Minus;
        break;
      case '/':
        single_type = TokenType::Slash;
        break;
      case '%':
        single_type = TokenType::Percent;
        break;
      case '=':
        single_type = TokenType::Equal;
        break;
      case '<':
        single_type = TokenType::Less;
        break;
      case '>':
        single_type = TokenType::Greater;
        break;
      case '!':
        single_type = TokenType::Bang;
        break;
      case '(':
        single_type = TokenType::LParen;
        break;
      case ')':
        single_type = TokenType::RParen;
        break;
      case '{':
        single_type = TokenType::LBrace;
        break;
      case '}':
        single_type = TokenType::RBrace;
        break;
      default:
        single_type = TokenType::Unknown;
        break;
    }

    if (single_type == TokenType::Unknown) {
      diagnostics.Error("E_LEX_UNKNOWN_CHAR", std::string("Unknown character: ") + ch, source.path,
                        {start_line, start_col, line, column + 1});
    } else {
      tokens.push_back(MakeToken(single_type, std::string(1, ch), start_line, start_col, line, column + 1));
    }

    ++i;
    ++column;
  }

  tokens.push_back(MakeToken(TokenType::EndOfFile, "", line, column, line, column));
  return tokens;
}

}  // namespace snow::frontend
