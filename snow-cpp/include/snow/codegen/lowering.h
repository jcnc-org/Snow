#pragma once

#include <string>

#include "snow/passes/pass_manager.h"
#include "snow/sir/sir.h"

namespace snow::codegen {

struct TargetConfig {
  std::string triple;
  bool executable_entry_wrapper = false;
};

enum class BackendKind {
  RealLlvm,
};

struct LoweringResult {
  std::string llvm_ir;
  BackendKind backend = BackendKind::RealLlvm;
  bool native_ready = true;
};

struct ObjectEmitResult {
  bool success = false;
  std::string error_message;
};

class LlvmLowering {
 public:
  LoweringResult Lower(const snow::sir::Module& module, const TargetConfig& target,
                       snow::passes::OptLevel opt_level) const;
  ObjectEmitResult EmitObject(const snow::sir::Module& module, const TargetConfig& target,
                              snow::passes::OptLevel opt_level, const std::string& output_path) const;
};

}  // namespace snow::codegen
