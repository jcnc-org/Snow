#include "snow/driver/driver.h"

#include <fstream>
#include <optional>
#include <sstream>
#include <string>

#include "snow/codegen/lowering.h"
#include "snow/common/source_file.h"
#include "snow/frontend/lexer.h"
#include "snow/frontend/parser.h"
#include "snow/ownership/ownership.h"
#include "snow/passes/pass_manager.h"
#include "snow/sema/sema.h"
#include "snow/sir/sir_builder.h"
#include "snow/sir/validator.h"

namespace snow::driver {

namespace {

std::optional<std::string> ReadFile(const std::string& path) {
  std::ifstream in(path, std::ios::in | std::ios::binary);
  if (!in) {
    return std::nullopt;
  }
  std::ostringstream oss;
  oss << in.rdbuf();
  return oss.str();
}

std::string DumpTokens(const frontend::TokenStream& tokens) {
  std::ostringstream oss;
  for (const auto& token : tokens) {
    oss << frontend::ToString(token.type) << "('" << token.lexeme << "') @" << token.range.line << ":"
        << token.range.column << "\n";
  }
  return oss.str();
}

}  // namespace

std::string RenderDiagnostics(const snow::common::DiagnosticEngine& diagnostics) {
  std::ostringstream oss;
  for (const auto& diagnostic : diagnostics.Diagnostics()) {
    oss << snow::common::FormatDiagnostic(diagnostic);
  }
  return oss.str();
}

CompileResult Driver::Compile(const CompileRequest& request) const {
  CompileResult result;

  const auto source_text = ReadFile(request.input_path);
  if (!source_text.has_value()) {
    result.diagnostics.Error("E_DRIVER_INPUT", "Cannot read input file", request.input_path, {0, 0, 0, 0});
    return result;
  }

  const snow::common::SourceFile source{request.input_path, source_text.value()};

  frontend::Lexer lexer;
  const auto tokens = lexer.Tokenize(source, result.diagnostics);
  if (request.emit.tokens) {
    result.token_dump = DumpTokens(tokens);
  }

  frontend::Parser parser;
  auto ast = parser.Parse(request.input_path, tokens, result.diagnostics);
  if (request.emit.ast) {
    result.ast_dump = frontend::DumpAst(ast);
  }

  sema::SemanticAnalyzer sema;
  auto sema_module = sema.Analyze(ast, result.diagnostics);
  if (request.emit.sema) {
    result.sema_dump = sema::DumpSema(sema_module);
  }

  ownership::OwnershipChecker ownership_checker;
  auto ownership_facts = ownership_checker.Check(sema_module, result.diagnostics);
  result.ownership_dump = ownership::DumpOwnership(ownership_facts);

  sir::SirBuilder builder;
  auto sir_module = builder.Build(sema_module, ownership_facts);

  sir::SirValidator validator;
#ifndef NDEBUG
  constexpr auto kValidationLevel = sir::ValidationLevel::Debug;
#else
  constexpr auto kValidationLevel = sir::ValidationLevel::Release;
#endif

  (void)validator.Validate(sir_module, kValidationLevel, result.diagnostics);

  passes::PassManager pass_manager;
  auto pass_result = pass_manager.Run(sir_module, request.opt_level, kValidationLevel, validator, result.diagnostics);

  if (request.emit.sir) {
    result.sir_dump = sir::DumpSir(pass_result.module);
  }
  if (request.emit.cfg) {
    result.cfg_dump = sir::DumpCfg(pass_result.module);
  }

  if (!result.diagnostics.HasErrors()) {
    codegen::LlvmLowering lowering;
    codegen::TargetConfig target{
        .triple = request.target_triple.empty() ? "host" : request.target_triple,
    };
    const auto llvm_result = lowering.Lower(pass_result.module, target, request.opt_level);
    if (request.emit.llvm) {
      result.llvm_dump = llvm_result.llvm_ir;
    }
  }

  result.success = !result.diagnostics.HasErrors();
  return result;
}

}  // namespace snow::driver
