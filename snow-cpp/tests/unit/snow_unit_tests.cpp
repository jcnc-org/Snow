#include <filesystem>
#include <fstream>
#include <iostream>
#include <string>

#include "snow/common/diagnostic_engine.h"
#include "snow/common/manifest.h"
#include "snow/common/source_file.h"
#include "snow/frontend/lexer.h"
#include "snow/ownership/ownership.h"
#include "snow/passes/pass_manager.h"
#include "snow/frontend/parser.h"
#include "snow/sema/sema.h"
#include "snow/sir/validator.h"

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

bool TestManifestParse() {
  const std::string test_name = "TestManifestParse";
  const std::filesystem::path tmp = std::filesystem::temp_directory_path() / "snow_manifest_unit.toml";

  {
    std::ofstream out(tmp);
    if (!out) {
      return Fail(test_name, "cannot create temporary manifest");
    }
    out << "[package]\n";
    out << "name = \"demo\"\n";
    out << "version = \"0.2.0\"\n";
    out << "edition = \"v2\"\n";
    out << "main = \"src/app.snow\"\n\n";
    out << "[build]\n";
    out << "target = \"x86_64-unknown-linux-gnu\"\n\n";
    out << "[dependencies]\n";
    out << "math = \"1.0\"\n";
  }

  snow::common::SnowManifest manifest;
  std::string error;
  const bool ok = snow::common::ParseSnowToml(tmp.string(), manifest, error);
  std::error_code ec;
  std::filesystem::remove(tmp, ec);

  if (!ok) {
    return Fail(test_name, "ParseSnowToml returned false: " + error);
  }
  if (manifest.name != "demo") {
    return Fail(test_name, "unexpected package name");
  }
  if (manifest.main != "src/app.snow") {
    return Fail(test_name, "unexpected package main");
  }
  if (manifest.target != "x86_64-unknown-linux-gnu") {
    return Fail(test_name, "unexpected build target");
  }
  if (!manifest.dependencies.contains("math")) {
    return Fail(test_name, "dependency not parsed");
  }
  return true;
}

bool TestSirValidatorValidModule() {
  const std::string test_name = "TestSirValidatorValidModule";
  snow::sir::Module module;
  module.module_path = "tests.valid";

  snow::sir::Function function;
  function.name = "_snow_tests_valid_main_deadbeef";
  function.return_type = "i32";
  function.linkage = snow::sir::Linkage::External;
  function.original_name = "main";

  snow::sir::BasicBlock entry;
  entry.label = "entry";
  entry.instructions.push_back(snow::sir::Instruction{
      .result = std::string("%1"),
      .type = "i32",
      .opcode = snow::sir::Opcode::Add,
      .operands = {"0", "0"},
      .is_terminator = false,
  });
  entry.instructions.push_back(snow::sir::Instruction{
      .result = std::nullopt,
      .type = "i32",
      .opcode = snow::sir::Opcode::Ret,
      .operands = {"%1"},
      .is_terminator = true,
  });
  function.blocks.push_back(std::move(entry));
  module.functions.push_back(std::move(function));

  snow::common::DiagnosticEngine diagnostics;
  snow::sir::SirValidator validator;
  const auto report = validator.Validate(module, snow::sir::ValidationLevel::Debug, diagnostics);

  if (!report.ok) {
    return Fail(test_name, "expected report.ok");
  }
  if (diagnostics.HasErrors()) {
    return Fail(test_name, "unexpected diagnostics for valid module");
  }
  return true;
}

bool TestSirValidatorDoubleDrop() {
  const std::string test_name = "TestSirValidatorDoubleDrop";
  snow::sir::Module module;
  module.module_path = "tests.double_drop";

  snow::sir::Function function;
  function.name = "_snow_tests_double_drop_main_deadbeef";
  function.return_type = "i32";
  function.linkage = snow::sir::Linkage::External;
  function.original_name = "main";

  snow::sir::BasicBlock entry;
  entry.label = "entry";
  entry.instructions.push_back(snow::sir::Instruction{
      .result = std::nullopt,
      .type = "ptr",
      .opcode = snow::sir::Opcode::Drop,
      .operands = {"x"},
      .is_terminator = false,
  });
  entry.instructions.push_back(snow::sir::Instruction{
      .result = std::nullopt,
      .type = "ptr",
      .opcode = snow::sir::Opcode::Drop,
      .operands = {"x"},
      .is_terminator = false,
  });
  entry.instructions.push_back(snow::sir::Instruction{
      .result = std::nullopt,
      .type = "i32",
      .opcode = snow::sir::Opcode::Ret,
      .operands = {"0"},
      .is_terminator = true,
  });
  function.blocks.push_back(std::move(entry));
  module.functions.push_back(std::move(function));

  snow::common::DiagnosticEngine diagnostics;
  snow::sir::SirValidator validator;
  const auto report = validator.Validate(module, snow::sir::ValidationLevel::Debug, diagnostics);

  if (report.ok) {
    return Fail(test_name, "expected report.ok=false for double drop");
  }
  if (!ContainsCode(diagnostics, "E_SIR_DOUBLE_DROP")) {
    return Fail(test_name, "expected E_SIR_DOUBLE_DROP");
  }
  return true;
}

bool TestSirValidatorMissingTerminator() {
  const std::string test_name = "TestSirValidatorMissingTerminator";
  snow::sir::Module module;
  module.module_path = "tests.missing_term";

  snow::sir::Function function;
  function.name = "_snow_tests_missing_term_main_deadbeef";
  function.return_type = "i32";
  function.linkage = snow::sir::Linkage::External;
  function.original_name = "main";

  snow::sir::BasicBlock entry;
  entry.label = "entry";
  entry.instructions.push_back(snow::sir::Instruction{
      .result = std::string("%1"),
      .type = "i32",
      .opcode = snow::sir::Opcode::Add,
      .operands = {"0", "0"},
      .is_terminator = false,
  });
  function.blocks.push_back(std::move(entry));
  module.functions.push_back(std::move(function));

  snow::common::DiagnosticEngine diagnostics;
  snow::sir::SirValidator validator;
  const auto report = validator.Validate(module, snow::sir::ValidationLevel::Debug, diagnostics);

  if (report.ok) {
    return Fail(test_name, "expected report.ok=false for missing terminator");
  }
  if (!ContainsCode(diagnostics, "E_SIR_TERM_COUNT")) {
    return Fail(test_name, "expected E_SIR_TERM_COUNT");
  }
  return true;
}

bool TestSirValidatorStoreTypeMismatch() {
  const std::string test_name = "TestSirValidatorStoreTypeMismatch";
  snow::sir::Module module;
  module.module_path = "tests.store_mismatch";

  snow::sir::Function function;
  function.name = "_snow_tests_store_mismatch_main_deadbeef";
  function.return_type = "i32";
  function.linkage = snow::sir::Linkage::External;
  function.original_name = "main";

  snow::sir::BasicBlock entry;
  entry.label = "entry";
  entry.instructions.push_back(snow::sir::Instruction{
      .result = std::string("%1"),
      .type = "ptr",
      .opcode = snow::sir::Opcode::Alloc,
      .operands = {"i32"},
      .is_terminator = false,
  });
  entry.instructions.push_back(snow::sir::Instruction{
      .result = std::nullopt,
      .type = "bool",
      .opcode = snow::sir::Opcode::Store,
      .operands = {"true", "%1"},
      .is_terminator = false,
  });
  entry.instructions.push_back(snow::sir::Instruction{
      .result = std::nullopt,
      .type = "i32",
      .opcode = snow::sir::Opcode::Ret,
      .operands = {"0"},
      .is_terminator = true,
  });
  function.blocks.push_back(std::move(entry));
  module.functions.push_back(std::move(function));

  snow::common::DiagnosticEngine diagnostics;
  snow::sir::SirValidator validator;
  const auto report = validator.Validate(module, snow::sir::ValidationLevel::Debug, diagnostics);

  if (report.ok) {
    return Fail(test_name, "expected report.ok=false for store type mismatch");
  }
  if (!ContainsCode(diagnostics, "E_SIR_STORE_TYPE")) {
    return Fail(test_name, "expected E_SIR_STORE_TYPE");
  }
  return true;
}

bool TestPassManagerConstantFold() {
  const std::string test_name = "TestPassManagerConstantFold";

  snow::sir::Module module;
  module.module_path = "tests.pass_fold";

  snow::sir::Function function;
  function.name = "_snow_tests_pass_fold_main_deadbeef";
  function.original_name = "main";
  function.return_type = "i32";
  function.linkage = snow::sir::Linkage::External;

  snow::sir::BasicBlock entry;
  entry.label = "entry";
  entry.instructions.push_back(snow::sir::Instruction{
      .result = std::string("%1"),
      .type = "i32",
      .opcode = snow::sir::Opcode::Add,
      .operands = {"1", "2"},
      .is_terminator = false,
  });
  entry.instructions.push_back(snow::sir::Instruction{
      .result = std::nullopt,
      .type = "i32",
      .opcode = snow::sir::Opcode::Ret,
      .operands = {"%1"},
      .is_terminator = true,
  });
  function.blocks.push_back(std::move(entry));
  module.functions.push_back(std::move(function));

  snow::common::DiagnosticEngine diagnostics;
  snow::sir::SirValidator validator;
  snow::passes::PassManager pass_manager;
  const auto result = pass_manager.Run(module, snow::passes::OptLevel::O2, snow::sir::ValidationLevel::Debug, validator,
                                       diagnostics);

  if (diagnostics.HasErrors()) {
    return Fail(test_name, "unexpected diagnostics");
  }
  if (result.module.functions.empty() || result.module.functions.front().blocks.empty()) {
    return Fail(test_name, "optimized module missing function/block");
  }
  const auto& instructions = result.module.functions.front().blocks.front().instructions;
  if (instructions.size() != 1 || instructions.front().opcode != snow::sir::Opcode::Ret ||
      instructions.front().operands.empty() || instructions.front().operands.front() != "3") {
    return Fail(test_name, "expected folded ret 3 without add instruction");
  }
  return true;
}

bool TestPassManagerCfgSimplify() {
  const std::string test_name = "TestPassManagerCfgSimplify";

  snow::sir::Module module;
  module.module_path = "tests.pass_cfg";

  snow::sir::Function function;
  function.name = "_snow_tests_pass_cfg_main_deadbeef";
  function.original_name = "main";
  function.return_type = "i32";
  function.linkage = snow::sir::Linkage::External;

  snow::sir::BasicBlock entry;
  entry.label = "entry";
  entry.instructions.push_back(snow::sir::Instruction{
      .result = std::nullopt,
      .type = "void",
      .opcode = snow::sir::Opcode::CondBr,
      .operands = {"true", "then_block", "else_block"},
      .is_terminator = true,
  });

  snow::sir::BasicBlock then_block;
  then_block.label = "then_block";
  then_block.instructions.push_back(snow::sir::Instruction{
      .result = std::nullopt,
      .type = "i32",
      .opcode = snow::sir::Opcode::Ret,
      .operands = {"1"},
      .is_terminator = true,
  });

  snow::sir::BasicBlock else_block;
  else_block.label = "else_block";
  else_block.instructions.push_back(snow::sir::Instruction{
      .result = std::nullopt,
      .type = "i32",
      .opcode = snow::sir::Opcode::Ret,
      .operands = {"2"},
      .is_terminator = true,
  });

  function.blocks.push_back(std::move(entry));
  function.blocks.push_back(std::move(then_block));
  function.blocks.push_back(std::move(else_block));
  module.functions.push_back(std::move(function));

  snow::common::DiagnosticEngine diagnostics;
  snow::sir::SirValidator validator;
  snow::passes::PassManager pass_manager;
  const auto result = pass_manager.Run(module, snow::passes::OptLevel::O2, snow::sir::ValidationLevel::Debug, validator,
                                       diagnostics);

  if (diagnostics.HasErrors()) {
    return Fail(test_name, "unexpected diagnostics");
  }
  if (result.module.functions.empty()) {
    return Fail(test_name, "optimized module missing function");
  }
  const auto& blocks = result.module.functions.front().blocks;
  if (blocks.size() != 2) {
    return Fail(test_name, "expected else block removed by cfg simplify");
  }
  if (blocks.front().instructions.empty() || blocks.front().instructions.back().opcode != snow::sir::Opcode::Br) {
    return Fail(test_name, "expected entry cond_br simplified to br");
  }
  return true;
}

bool TestParserExpressionPrecedence() {
  const std::string test_name = "TestParserExpressionPrecedence";
  const snow::common::SourceFile source{
      "unit_expr.snow",
      "pub fn main() -> i32 { return 1 + 2 * 3; }",
  };

  snow::common::DiagnosticEngine diagnostics;
  snow::frontend::Lexer lexer;
  snow::frontend::Parser parser;

  const auto tokens = lexer.Tokenize(source, diagnostics);
  const auto ast = parser.Parse("unit.expr", tokens, diagnostics);

  if (diagnostics.HasErrors()) {
    return Fail(test_name, "unexpected parse diagnostics");
  }
  if (ast.functions.empty()) {
    return Fail(test_name, "expected parsed function");
  }
  const auto& fn = ast.functions.front();
  if (fn.statements.empty()) {
    return Fail(test_name, "expected function statements");
  }
  if (fn.statements.front().kind != snow::frontend::Statement::Kind::Return) {
    return Fail(test_name, "expected first statement to be return");
  }
  const auto return_expr = fn.statements.front().expr;
  if (!return_expr) {
    return Fail(test_name, "expected return expression");
  }
  if (return_expr->kind != snow::frontend::Expr::Kind::Binary || return_expr->op != snow::frontend::BinaryOp::Add) {
    return Fail(test_name, "top-level return expression should be add");
  }
  if (!return_expr->rhs || return_expr->rhs->kind != snow::frontend::Expr::Kind::Binary ||
      return_expr->rhs->op != snow::frontend::BinaryOp::Mul) {
    return Fail(test_name, "rhs of add should be mul expression");
  }

  return true;
}

bool TestSemaReturnTypeMismatch() {
  const std::string test_name = "TestSemaReturnTypeMismatch";
  const snow::common::SourceFile source{
      "unit_sema.snow",
      "pub fn main() -> i32 { return 1 < 2; }",
  };

  snow::common::DiagnosticEngine diagnostics;
  snow::frontend::Lexer lexer;
  snow::frontend::Parser parser;
  snow::sema::SemanticAnalyzer sema;

  const auto tokens = lexer.Tokenize(source, diagnostics);
  const auto ast = parser.Parse("unit.sema", tokens, diagnostics);
  (void)sema.Analyze(ast, diagnostics);

  if (!ContainsCode(diagnostics, "E_SEMA_RET_TYPE")) {
    return Fail(test_name, "expected E_SEMA_RET_TYPE");
  }
  return true;
}

bool TestParserControlFlowForms() {
  const std::string test_name = "TestParserControlFlowForms";
  const snow::common::SourceFile source{
      "unit_cfg.snow",
      "pub fn main() -> i32 { while 1 < 0 { } if 1 < 2 { return 10; } else { return 20; } }",
  };

  snow::common::DiagnosticEngine diagnostics;
  snow::frontend::Lexer lexer;
  snow::frontend::Parser parser;

  const auto tokens = lexer.Tokenize(source, diagnostics);
  const auto ast = parser.Parse("unit.cfg", tokens, diagnostics);

  if (diagnostics.HasErrors()) {
    return Fail(test_name, "unexpected parse diagnostics");
  }
  if (ast.functions.empty()) {
    return Fail(test_name, "expected parsed function");
  }
  const auto& fn = ast.functions.front();
  if (fn.statements.size() != 2) {
    return Fail(test_name, "expected while + if statements");
  }
  if (fn.statements[0].kind != snow::frontend::Statement::Kind::While || !fn.statements[0].expr) {
    return Fail(test_name, "expected while statement with condition");
  }
  if (fn.statements[1].kind != snow::frontend::Statement::Kind::If || !fn.statements[1].expr) {
    return Fail(test_name, "expected if statement with condition");
  }
  if (fn.statements[1].then_body.empty() || fn.statements[1].else_body.empty()) {
    return Fail(test_name, "expected parsed if branches");
  }
  return true;
}

bool TestSemaIfConditionTypeMismatch() {
  const std::string test_name = "TestSemaIfConditionTypeMismatch";
  const snow::common::SourceFile source{
      "unit_if_bad.snow",
      "pub fn main() -> i32 { if 1 { return 10; } else { return 20; } }",
  };

  snow::common::DiagnosticEngine diagnostics;
  snow::frontend::Lexer lexer;
  snow::frontend::Parser parser;
  snow::sema::SemanticAnalyzer sema;

  const auto tokens = lexer.Tokenize(source, diagnostics);
  const auto ast = parser.Parse("unit.if_bad", tokens, diagnostics);
  (void)sema.Analyze(ast, diagnostics);

  if (!ContainsCode(diagnostics, "E_SEMA_IF_COND_TYPE")) {
    return Fail(test_name, "expected E_SEMA_IF_COND_TYPE");
  }
  return true;
}

bool TestSemaWhileConditionTypeMismatch() {
  const std::string test_name = "TestSemaWhileConditionTypeMismatch";
  const snow::common::SourceFile source{
      "unit_while_bad.snow",
      "pub fn main() -> i32 { while 1 { } return 0; }",
  };

  snow::common::DiagnosticEngine diagnostics;
  snow::frontend::Lexer lexer;
  snow::frontend::Parser parser;
  snow::sema::SemanticAnalyzer sema;

  const auto tokens = lexer.Tokenize(source, diagnostics);
  const auto ast = parser.Parse("unit.while_bad", tokens, diagnostics);
  (void)sema.Analyze(ast, diagnostics);

  if (!ContainsCode(diagnostics, "E_SEMA_WHILE_COND_TYPE")) {
    return Fail(test_name, "expected E_SEMA_WHILE_COND_TYPE");
  }
  return true;
}

bool TestParserWhileBreakFlag() {
  const std::string test_name = "TestParserWhileBreakFlag";
  const snow::common::SourceFile source{
      "unit_while_break.snow",
      "pub fn main() -> i32 { while 1 < 2 { break; } return 9; }",
  };

  snow::common::DiagnosticEngine diagnostics;
  snow::frontend::Lexer lexer;
  snow::frontend::Parser parser;

  const auto tokens = lexer.Tokenize(source, diagnostics);
  const auto ast = parser.Parse("unit.while_break", tokens, diagnostics);
  if (diagnostics.HasErrors()) {
    return Fail(test_name, "unexpected parse diagnostics");
  }
  if (ast.functions.empty()) {
    return Fail(test_name, "expected parsed function");
  }
  const auto& statements = ast.functions.front().statements;
  if (statements.empty()) {
    return Fail(test_name, "expected while statement");
  }
  if (statements.front().kind != snow::frontend::Statement::Kind::While) {
    return Fail(test_name, "expected first statement to be while");
  }
  if (statements.front().body.empty() || statements.front().body.front().kind != snow::frontend::Statement::Kind::Break) {
    return Fail(test_name, "expected break statement inside while body");
  }
  return true;
}

bool TestParserLetAndContinue() {
  const std::string test_name = "TestParserLetAndContinue";
  const snow::common::SourceFile source{
      "unit_let_continue.snow",
      "pub fn main() -> i32 { let x = 1 + 2; while x < 10 { continue; } return x; }",
  };

  snow::common::DiagnosticEngine diagnostics;
  snow::frontend::Lexer lexer;
  snow::frontend::Parser parser;

  const auto tokens = lexer.Tokenize(source, diagnostics);
  const auto ast = parser.Parse("unit.let_continue", tokens, diagnostics);
  if (diagnostics.HasErrors()) {
    return Fail(test_name, "unexpected parse diagnostics");
  }
  if (ast.functions.empty()) {
    return Fail(test_name, "expected parsed function");
  }
  const auto& statements = ast.functions.front().statements;
  if (statements.size() < 3) {
    return Fail(test_name, "expected let/while/return statement sequence");
  }
  if (statements[0].kind != snow::frontend::Statement::Kind::Let || statements[0].name != "x") {
    return Fail(test_name, "expected leading let x statement");
  }
  if (statements[1].kind != snow::frontend::Statement::Kind::While || statements[1].body.empty()) {
    return Fail(test_name, "expected while statement with body");
  }
  if (statements[1].body.front().kind != snow::frontend::Statement::Kind::Continue) {
    return Fail(test_name, "expected continue statement inside loop body");
  }
  if (statements[2].kind != snow::frontend::Statement::Kind::Return) {
    return Fail(test_name, "expected trailing return statement");
  }
  return true;
}

bool TestParserAssignmentStatement() {
  const std::string test_name = "TestParserAssignmentStatement";
  const snow::common::SourceFile source{
      "unit_assign.snow",
      "pub fn main() -> i32 { let x = 1; x = x + 2; return x; }",
  };

  snow::common::DiagnosticEngine diagnostics;
  snow::frontend::Lexer lexer;
  snow::frontend::Parser parser;

  const auto tokens = lexer.Tokenize(source, diagnostics);
  const auto ast = parser.Parse("unit.assign", tokens, diagnostics);
  if (diagnostics.HasErrors()) {
    return Fail(test_name, "unexpected parse diagnostics");
  }
  if (ast.functions.empty()) {
    return Fail(test_name, "expected parsed function");
  }
  const auto& statements = ast.functions.front().statements;
  if (statements.size() < 3) {
    return Fail(test_name, "expected let/assign/return sequence");
  }
  if (statements[0].kind != snow::frontend::Statement::Kind::Let) {
    return Fail(test_name, "expected first statement to be let");
  }
  if (statements[1].kind != snow::frontend::Statement::Kind::Assign || statements[1].name != "x") {
    return Fail(test_name, "expected assignment statement to x");
  }
  if (statements[2].kind != snow::frontend::Statement::Kind::Return) {
    return Fail(test_name, "expected final return statement");
  }
  return true;
}

bool TestParserTypedLetStatement() {
  const std::string test_name = "TestParserTypedLetStatement";
  const snow::common::SourceFile source{
      "unit_typed_let.snow",
      "pub fn main() -> i32 { let x: i32 = 1; return x; }",
  };

  snow::common::DiagnosticEngine diagnostics;
  snow::frontend::Lexer lexer;
  snow::frontend::Parser parser;

  const auto tokens = lexer.Tokenize(source, diagnostics);
  const auto ast = parser.Parse("unit.typed_let", tokens, diagnostics);
  if (diagnostics.HasErrors()) {
    return Fail(test_name, "unexpected parse diagnostics");
  }
  if (ast.functions.empty() || ast.functions.front().statements.empty()) {
    return Fail(test_name, "expected parsed typed let statement");
  }
  const auto& stmt = ast.functions.front().statements.front();
  if (stmt.kind != snow::frontend::Statement::Kind::Let) {
    return Fail(test_name, "expected let statement");
  }
  if (stmt.type_name != "i32") {
    return Fail(test_name, "expected let type annotation i32");
  }
  return true;
}

bool TestSemaBreakOutsideLoop() {
  const std::string test_name = "TestSemaBreakOutsideLoop";
  const snow::common::SourceFile source{
      "unit_break_bad.snow",
      "pub fn main() -> i32 { break; return 0; }",
  };

  snow::common::DiagnosticEngine diagnostics;
  snow::frontend::Lexer lexer;
  snow::frontend::Parser parser;
  snow::sema::SemanticAnalyzer sema;

  const auto tokens = lexer.Tokenize(source, diagnostics);
  const auto ast = parser.Parse("unit.break_bad", tokens, diagnostics);
  (void)sema.Analyze(ast, diagnostics);

  if (!ContainsCode(diagnostics, "E_SEMA_BREAK_OUTSIDE_LOOP")) {
    return Fail(test_name, "expected E_SEMA_BREAK_OUTSIDE_LOOP");
  }
  return true;
}

bool TestOwnershipUseAfterMove() {
  const std::string test_name = "TestOwnershipUseAfterMove";
  const snow::common::SourceFile source{
      "unit_ownership_move.snow",
      "private fn consume(a: Box) -> i32 { return 0; } pub fn main(x: Box) -> i32 { consume(x); consume(x); return 0; }",
  };

  snow::common::DiagnosticEngine diagnostics;
  snow::frontend::Lexer lexer;
  snow::frontend::Parser parser;
  snow::sema::SemanticAnalyzer sema;
  snow::ownership::OwnershipChecker ownership;

  const auto tokens = lexer.Tokenize(source, diagnostics);
  const auto ast = parser.Parse("unit.ownership_move", tokens, diagnostics);
  const auto sema_module = sema.Analyze(ast, diagnostics);
  (void)ownership.Check(sema_module, diagnostics);

  if (!ContainsCode(diagnostics, "E_OWNERSHIP_USE_AFTER_MOVE")) {
    return Fail(test_name, "expected E_OWNERSHIP_USE_AFTER_MOVE");
  }
  return true;
}

bool TestOwnershipMoveSuppressesDrop() {
  const std::string test_name = "TestOwnershipMoveSuppressesDrop";
  const snow::common::SourceFile source{
      "unit_ownership_drop.snow",
      "private fn consume(a: Box) -> i32 { return 0; } pub fn main(x: Box) -> i32 { consume(x); return 0; }",
  };

  snow::common::DiagnosticEngine diagnostics;
  snow::frontend::Lexer lexer;
  snow::frontend::Parser parser;
  snow::sema::SemanticAnalyzer sema;
  snow::ownership::OwnershipChecker ownership;

  const auto tokens = lexer.Tokenize(source, diagnostics);
  const auto ast = parser.Parse("unit.ownership_drop", tokens, diagnostics);
  const auto sema_module = sema.Analyze(ast, diagnostics);
  const auto facts = ownership.Check(sema_module, diagnostics);

  bool found_main_x = false;
  for (const auto& fact : facts.facts) {
    if (fact.symbol == "main::x") {
      found_main_x = true;
      if (fact.drop_at_exit) {
        return Fail(test_name, "expected moved ownership fact to disable drop_at_exit");
      }
    }
  }
  if (!found_main_x) {
    return Fail(test_name, "expected ownership fact for main::x");
  }
  return true;
}

bool TestSemaContinueOutsideLoop() {
  const std::string test_name = "TestSemaContinueOutsideLoop";
  const snow::common::SourceFile source{
      "unit_continue_bad.snow",
      "pub fn main() -> i32 { continue; return 0; }",
  };

  snow::common::DiagnosticEngine diagnostics;
  snow::frontend::Lexer lexer;
  snow::frontend::Parser parser;
  snow::sema::SemanticAnalyzer sema;

  const auto tokens = lexer.Tokenize(source, diagnostics);
  const auto ast = parser.Parse("unit.continue_bad", tokens, diagnostics);
  (void)sema.Analyze(ast, diagnostics);

  if (!ContainsCode(diagnostics, "E_SEMA_CONTINUE_OUTSIDE_LOOP")) {
    return Fail(test_name, "expected E_SEMA_CONTINUE_OUTSIDE_LOOP");
  }
  return true;
}

}  // namespace

int main() {
  int failed = 0;
  failed += TestManifestParse() ? 0 : 1;
  failed += TestSirValidatorValidModule() ? 0 : 1;
  failed += TestSirValidatorDoubleDrop() ? 0 : 1;
  failed += TestSirValidatorMissingTerminator() ? 0 : 1;
  failed += TestSirValidatorStoreTypeMismatch() ? 0 : 1;
  failed += TestPassManagerConstantFold() ? 0 : 1;
  failed += TestPassManagerCfgSimplify() ? 0 : 1;
  failed += TestParserExpressionPrecedence() ? 0 : 1;
  failed += TestSemaReturnTypeMismatch() ? 0 : 1;
  failed += TestParserControlFlowForms() ? 0 : 1;
  failed += TestSemaIfConditionTypeMismatch() ? 0 : 1;
  failed += TestSemaWhileConditionTypeMismatch() ? 0 : 1;
  failed += TestParserWhileBreakFlag() ? 0 : 1;
  failed += TestParserLetAndContinue() ? 0 : 1;
  failed += TestParserAssignmentStatement() ? 0 : 1;
  failed += TestParserTypedLetStatement() ? 0 : 1;
  failed += TestSemaBreakOutsideLoop() ? 0 : 1;
  failed += TestOwnershipUseAfterMove() ? 0 : 1;
  failed += TestOwnershipMoveSuppressesDrop() ? 0 : 1;
  failed += TestSemaContinueOutsideLoop() ? 0 : 1;

  if (failed == 0) {
    std::cout << "[PASS] snow-unit-tests\n";
    return 0;
  }

  std::cerr << "[FAIL] snow-unit-tests failed=" << failed << "\n";
  return 1;
}
