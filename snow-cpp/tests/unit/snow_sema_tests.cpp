#include <iostream>
#include <string>

#include "snow/common/diagnostic_engine.h"
#include "snow/common/source_file.h"
#include "snow/frontend/lexer.h"
#include "snow/frontend/parser.h"
#include "snow/sema/sema.h"

namespace {

bool Fail(const std::string& test_name, const std::string& message) {
  std::cerr << "[FAIL] " << test_name << ": " << message << "\n";
  return false;
}

bool ContainsCode(const snow::common::DiagnosticEngine& diagnostics, const std::string& code) {
  for (const auto& diagnostic : diagnostics.Diagnostics()) {
    if (diagnostic.code == code) {
      return true;
    }
  }
  return false;
}

bool TestSemaAssignUndefined() {
  const std::string test_name = "TestSemaAssignUndefined";
  const snow::common::SourceFile source{
      "unit_assign_undef.snow",
      "pub fn main() -> i32 { x = 1; return 0; }",
  };

  snow::common::DiagnosticEngine diagnostics;
  snow::frontend::Lexer lexer;
  snow::frontend::Parser parser;
  snow::sema::SemanticAnalyzer sema;

  const auto tokens = lexer.Tokenize(source, diagnostics);
  const auto ast = parser.Parse("unit.assign_undef", tokens, diagnostics);
  (void)sema.Analyze(ast, diagnostics);

  if (!ContainsCode(diagnostics, "E_SEMA_ASSIGN_UNDEFINED")) {
    return Fail(test_name, "expected E_SEMA_ASSIGN_UNDEFINED");
  }
  return true;
}

bool TestSemaAssignTypeMismatch() {
  const std::string test_name = "TestSemaAssignTypeMismatch";
  const snow::common::SourceFile source{
      "unit_assign_type.snow",
      "pub fn main() -> i32 { let x = 1; x = 1 < 2; return x; }",
  };

  snow::common::DiagnosticEngine diagnostics;
  snow::frontend::Lexer lexer;
  snow::frontend::Parser parser;
  snow::sema::SemanticAnalyzer sema;

  const auto tokens = lexer.Tokenize(source, diagnostics);
  const auto ast = parser.Parse("unit.assign_type", tokens, diagnostics);
  (void)sema.Analyze(ast, diagnostics);

  if (!ContainsCode(diagnostics, "E_SEMA_ASSIGN_TYPE")) {
    return Fail(test_name, "expected E_SEMA_ASSIGN_TYPE");
  }
  return true;
}

bool TestSemaLetTypeMismatch() {
  const std::string test_name = "TestSemaLetTypeMismatch";
  const snow::common::SourceFile source{
      "unit_let_type_bad.snow",
      "pub fn main() -> i32 { let x: i32 = 1 < 2; return 0; }",
  };

  snow::common::DiagnosticEngine diagnostics;
  snow::frontend::Lexer lexer;
  snow::frontend::Parser parser;
  snow::sema::SemanticAnalyzer sema;

  const auto tokens = lexer.Tokenize(source, diagnostics);
  const auto ast = parser.Parse("unit.let_type_bad", tokens, diagnostics);
  (void)sema.Analyze(ast, diagnostics);

  if (!ContainsCode(diagnostics, "E_SEMA_LET_TYPE")) {
    return Fail(test_name, "expected E_SEMA_LET_TYPE");
  }
  return true;
}

}  // namespace

int main() {
  int failed = 0;
  failed += TestSemaAssignUndefined() ? 0 : 1;
  failed += TestSemaAssignTypeMismatch() ? 0 : 1;
  failed += TestSemaLetTypeMismatch() ? 0 : 1;

  if (failed == 0) {
    std::cout << "[PASS] snow-sema-tests\n";
    return 0;
  }

  std::cerr << "[FAIL] snow-sema-tests failed=" << failed << "\n";
  return 1;
}
