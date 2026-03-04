#pragma once

#include <string>

#include "snow/passes/pass_manager.h"
#include "snow/sir/sir.h"

namespace snow::codegen {

struct TargetConfig {
  std::string triple;
  bool executable_entry_wrapper = false;
};

struct LoweringResult {
  std::string llvm_ir;
  bool used_real_llvm = false;
};

class LlvmLowering {
 public:
  LoweringResult Lower(const snow::sir::Module& module, const TargetConfig& target,
                       snow::passes::OptLevel opt_level) const;
};

}  // namespace snow::codegen
