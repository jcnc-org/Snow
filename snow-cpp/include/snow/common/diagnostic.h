#pragma once

#include <optional>
#include <string>
#include <vector>

namespace snow::common {

enum class Severity {
  Error,
  Warning,
  Note,
  InternalError,
};

struct SourceRange {
  std::size_t line = 0;
  std::size_t column = 0;
  std::size_t end_line = 0;
  std::size_t end_column = 0;
};

struct Diagnostic {
  std::string code;
  Severity severity = Severity::Error;
  std::string message;
  std::string file;
  SourceRange range;
  std::optional<std::string> suggestion;
};

std::string ToString(Severity severity);
std::string FormatDiagnostic(const Diagnostic& diagnostic);

}  // namespace snow::common
