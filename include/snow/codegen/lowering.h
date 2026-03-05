#pragma once

#include <string>

#include "snow/codegen/llvm/target_config.h"
#include "snow/passes/pass_manager.h"
#include "snow/sir/sir.h"

namespace snow::codegen {

    enum class BackendKind {
        RealLlvm,
    };

    struct LoweringResult {
        std::string llvm_ir;
        BackendKind backend = BackendKind::RealLlvm;
        bool native_ready = true;
        std::string error_code;
        std::string error_message;
    };

    struct ObjectEmitResult {
        bool success = false;
        std::string error_code;
        std::string error_message;
    };

    class LlvmLowering {
    public:
        LoweringResult Lower(const snow::sir::Module &module, const TargetConfig &target,
                             snow::passes::OptLevel opt_level) const;
        ObjectEmitResult EmitObject(const snow::sir::Module &module, const TargetConfig &target,
                                    snow::passes::OptLevel opt_level, const std::string &output_path) const;
    };

} // namespace snow::codegen
