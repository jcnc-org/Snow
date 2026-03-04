#include "snow/common/diagnostic_engine.h"

#include <sstream>
#include <utility>

namespace snow::common {

std::string ToString(const Severity severity) {
  switch (severity) {
    case Severity::Error:
      return "error";
    case Severity::Warning:
      return "warning";
    case Severity::Note:
      return "note";
    case Severity::InternalError:
      return "internal-error";
  }
  return "unknown";
}

std::string FormatDiagnostic(const Diagnostic& diagnostic) {
  std::ostringstream oss;
  oss << ToString(diagnostic.severity) << "[" << diagnostic.code << "]: " << diagnostic.message << "\n";
  if (!diagnostic.file.empty()) {
    oss << " --> " << diagnostic.file << ":" << diagnostic.range.line << ":" << diagnostic.range.column << "\n";
  }
  if (diagnostic.suggestion.has_value()) {
    oss << " suggestion: " << diagnostic.suggestion.value() << "\n";
  }
  return oss.str();
}

void DiagnosticEngine::Add(const Diagnostic& diagnostic) {
  diagnostics_.push_back(diagnostic);
}

void DiagnosticEngine::Error(std::string code, std::string message, std::string file, const SourceRange range,
                             std::optional<std::string> suggestion) {
  diagnostics_.push_back(Diagnostic{
      .code = std::move(code),
      .severity = Severity::Error,
      .message = std::move(message),
      .file = std::move(file),
      .range = range,
      .suggestion = std::move(suggestion),
  });
}

void DiagnosticEngine::Warning(std::string code, std::string message, std::string file, const SourceRange range,
                               std::optional<std::string> suggestion) {
  diagnostics_.push_back(Diagnostic{
      .code = std::move(code),
      .severity = Severity::Warning,
      .message = std::move(message),
      .file = std::move(file),
      .range = range,
      .suggestion = std::move(suggestion),
  });
}

bool DiagnosticEngine::HasErrors() const {
  for (const auto& diagnostic : diagnostics_) {
    if (diagnostic.severity == Severity::Error || diagnostic.severity == Severity::InternalError) {
      return true;
    }
  }
  return false;
}

const std::vector<Diagnostic>& DiagnosticEngine::Diagnostics() const {
  return diagnostics_;
}

}  // namespace snow::common
