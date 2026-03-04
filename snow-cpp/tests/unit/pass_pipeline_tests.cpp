#include <iostream>
#include <string>

#include "snow/common/diagnostic_engine.h"
#include "snow/passes/pass_manager.h"
#include "snow/sir/sir.h"
#include "snow/sir/validator.h"

namespace {

bool Fail(const std::string& test_name, const std::string& message) {
  std::cerr << "[FAIL] " << test_name << ": " << message << "\n";
  return false;
}

snow::sir::Function* FindFunctionByOriginalName(snow::sir::Module& module, const std::string& original_name) {
  for (auto& function : module.functions) {
    if (function.original_name == original_name) {
      return &function;
    }
  }
  return nullptr;
}

bool TestPassManagerCopyPropagation() {
  const std::string test_name = "TestPassManagerCopyPropagation";

  snow::sir::Module module;
  module.module_path = "tests.copyprop";

  snow::sir::Function function;
  function.name = "_snow_tests_copyprop_main_deadbeef";
  function.original_name = "main";
  function.return_type = "i32";
  function.linkage = snow::sir::Linkage::External;

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
      .type = "i32",
      .opcode = snow::sir::Opcode::Store,
      .operands = {"41", "%1"},
      .is_terminator = false,
  });
  entry.instructions.push_back(snow::sir::Instruction{
      .result = std::string("%2"),
      .type = "i32",
      .opcode = snow::sir::Opcode::Load,
      .operands = {"%1"},
      .is_terminator = false,
  });
  entry.instructions.push_back(snow::sir::Instruction{
      .result = std::string("%3"),
      .type = "i32",
      .opcode = snow::sir::Opcode::Add,
      .operands = {"%2", "1"},
      .is_terminator = false,
  });
  entry.instructions.push_back(snow::sir::Instruction{
      .result = std::nullopt,
      .type = "i32",
      .opcode = snow::sir::Opcode::Ret,
      .operands = {"%3"},
      .is_terminator = true,
  });

  function.blocks.push_back(std::move(entry));
  module.functions.push_back(std::move(function));

  snow::common::DiagnosticEngine diagnostics;
  snow::sir::SirValidator validator;
  snow::passes::PassManager pass_manager;
  auto result =
      pass_manager.Run(module, snow::passes::OptLevel::O2, snow::sir::ValidationLevel::Debug, validator, diagnostics);

  if (diagnostics.HasErrors()) {
    return Fail(test_name, "unexpected diagnostics");
  }

  auto* optimized = FindFunctionByOriginalName(result.module, "main");
  if (optimized == nullptr || optimized->blocks.empty()) {
    return Fail(test_name, "optimized main function missing");
  }

  bool saw_load = false;
  bool saw_add_with_literal = false;
  for (const auto& block : optimized->blocks) {
    for (const auto& instr : block.instructions) {
      if (instr.opcode == snow::sir::Opcode::Load) {
        saw_load = true;
      }
      if (instr.opcode == snow::sir::Opcode::Add && instr.operands.size() == 2 && instr.operands[0] == "41") {
        saw_add_with_literal = true;
      }
    }
  }

  if (saw_load) {
    return Fail(test_name, "expected load to be removed by copy propagation");
  }
  if (!saw_add_with_literal) {
    return Fail(test_name, "expected add to use propagated literal operand");
  }
  return true;
}

bool TestPassManagerInline() {
  const std::string test_name = "TestPassManagerInline";

  snow::sir::Module module;
  module.module_path = "tests.inline";

  snow::sir::Function one;
  one.name = "_snow_tests_inline_one_deadbeef";
  one.original_name = "one";
  one.return_type = "i32";
  one.linkage = snow::sir::Linkage::Private;

  snow::sir::BasicBlock one_entry;
  one_entry.label = "entry";
  one_entry.instructions.push_back(snow::sir::Instruction{
      .result = std::string("%1"),
      .type = "i32",
      .opcode = snow::sir::Opcode::Add,
      .operands = {"3", "4"},
      .is_terminator = false,
  });
  one_entry.instructions.push_back(snow::sir::Instruction{
      .result = std::nullopt,
      .type = "void",
      .opcode = snow::sir::Opcode::Br,
      .operands = {"fn_return"},
      .is_terminator = true,
  });

  snow::sir::BasicBlock one_ret;
  one_ret.label = "fn_return";
  one_ret.instructions.push_back(snow::sir::Instruction{
      .result = std::nullopt,
      .type = "i32",
      .opcode = snow::sir::Opcode::Ret,
      .operands = {"%1"},
      .is_terminator = true,
  });

  one.blocks.push_back(std::move(one_entry));
  one.blocks.push_back(std::move(one_ret));

  snow::sir::Function main_fn;
  main_fn.name = "_snow_tests_inline_main_deadbeef";
  main_fn.original_name = "main";
  main_fn.return_type = "i32";
  main_fn.linkage = snow::sir::Linkage::External;

  snow::sir::BasicBlock main_entry;
  main_entry.label = "entry";
  main_entry.instructions.push_back(snow::sir::Instruction{
      .result = std::string("%1"),
      .type = "i32",
      .opcode = snow::sir::Opcode::Call,
      .operands = {"_snow_tests_inline_one_deadbeef"},
      .is_terminator = false,
  });
  main_entry.instructions.push_back(snow::sir::Instruction{
      .result = std::nullopt,
      .type = "void",
      .opcode = snow::sir::Opcode::Br,
      .operands = {"fn_return"},
      .is_terminator = true,
  });

  snow::sir::BasicBlock main_ret;
  main_ret.label = "fn_return";
  main_ret.instructions.push_back(snow::sir::Instruction{
      .result = std::nullopt,
      .type = "i32",
      .opcode = snow::sir::Opcode::Ret,
      .operands = {"%1"},
      .is_terminator = true,
  });

  main_fn.blocks.push_back(std::move(main_entry));
  main_fn.blocks.push_back(std::move(main_ret));

  module.functions.push_back(std::move(one));
  module.functions.push_back(std::move(main_fn));

  snow::common::DiagnosticEngine diagnostics;
  snow::sir::SirValidator validator;
  snow::passes::PassManager pass_manager;
  auto result =
      pass_manager.Run(module, snow::passes::OptLevel::O2, snow::sir::ValidationLevel::Debug, validator, diagnostics);

  if (diagnostics.HasErrors()) {
    return Fail(test_name, "unexpected diagnostics");
  }

  auto* optimized_main = FindFunctionByOriginalName(result.module, "main");
  if (optimized_main == nullptr) {
    return Fail(test_name, "optimized main missing");
  }

  bool saw_call = false;
  std::string ret_operand;
  for (const auto& block : optimized_main->blocks) {
    for (const auto& instr : block.instructions) {
      if (instr.opcode == snow::sir::Opcode::Call) {
        saw_call = true;
      }
      if (instr.opcode == snow::sir::Opcode::Ret && !instr.operands.empty()) {
        ret_operand = instr.operands[0];
      }
    }
  }

  if (saw_call) {
    return Fail(test_name, "expected call to be removed by inline pass");
  }
  if (ret_operand != "7") {
    return Fail(test_name, "expected constant-folded inlined return value 7 in caller");
  }
  return true;
}

}  // namespace

int main() {
  int failed = 0;
  failed += TestPassManagerCopyPropagation() ? 0 : 1;
  failed += TestPassManagerInline() ? 0 : 1;

  if (failed == 0) {
    std::cout << "[PASS] snow-pass-pipeline-tests\n";
    return 0;
  }

  std::cerr << "[FAIL] snow-pass-pipeline-tests failed=" << failed << "\n";
  return 1;
}
