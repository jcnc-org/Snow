#include <filesystem>
#include <fstream>
#include <iostream>
#include <string>

#include "snow/common/diagnostic_engine.h"
#include "snow/common/manifest.h"
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

}  // namespace

int main() {
  int failed = 0;
  failed += TestManifestParse() ? 0 : 1;
  failed += TestSirValidatorValidModule() ? 0 : 1;
  failed += TestSirValidatorDoubleDrop() ? 0 : 1;
  failed += TestSirValidatorMissingTerminator() ? 0 : 1;

  if (failed == 0) {
    std::cout << "[PASS] snow-unit-tests\n";
    return 0;
  }

  std::cerr << "[FAIL] snow-unit-tests failed=" << failed << "\n";
  return 1;
}
