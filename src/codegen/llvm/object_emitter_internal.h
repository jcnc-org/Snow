#pragma once

#include <string>

#include "snow/codegen/lowering.h"

namespace snow::codegen::llvm_backend {

    ObjectEmitResult EmitObjectWithLlvmApi(const snow::sir::Module &module, const TargetConfig &target,
                                           snow::passes::OptLevel opt_level, const std::string &output_path);

} // namespace snow::codegen::llvm_backend
