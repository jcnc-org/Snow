#include "snow/passes/pass_manager.h"

namespace snow::passes {

PassResult PassManager::Run(const snow::sir::Module& input, const OptLevel level,
                            const snow::sir::ValidationLevel validation_level,
                            const snow::sir::SirValidator& validator,
                            snow::common::DiagnosticEngine& diagnostics) const {
  PassResult result;
  result.module = input;

  auto run_validation = [&]() {
    if (validation_level == snow::sir::ValidationLevel::Debug) {
      (void)validator.Validate(result.module, validation_level, diagnostics);
    }
  };

  if (level == OptLevel::O0) {
    result.executed_passes.push_back("Canonicalize");
    run_validation();
    if (validation_level == snow::sir::ValidationLevel::Release) {
      (void)validator.Validate(result.module, validation_level, diagnostics);
    }
    return result;
  }

  result.executed_passes.push_back("ConstantFold");
  run_validation();

  result.executed_passes.push_back("CfgSimplify");
  run_validation();

  result.executed_passes.push_back("CopyPropagation");
  run_validation();

  result.executed_passes.push_back("DeadCodeElimination");
  run_validation();

  result.executed_passes.push_back("Inline");
  run_validation();

  result.executed_passes.push_back("CfgSimplify");
  run_validation();

  result.executed_passes.push_back("DeadCodeElimination");
  run_validation();

  if (validation_level == snow::sir::ValidationLevel::Release) {
    (void)validator.Validate(result.module, validation_level, diagnostics);
  }

  return result;
}

}  // namespace snow::passes
