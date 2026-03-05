#pragma once

#include <optional>
#include <string>

#include "snow/codegen/lowering.h"

namespace snow::codegen::llvm_backend {

    std::optional<std::string> FirstUnsupportedOpcode(const snow::sir::Module &module);
    std::string LowerTextual(const snow::sir::Module &module, const TargetConfig &target,
                             snow::passes::OptLevel opt_level);

} // namespace snow::codegen::llvm_backend
