// Module: Public LLVM lowering facade.

#include "snow/codegen/lowering.h"

#if SNOW_ENABLE_LLVM
#include <llvm/AsmParser/Parser.h>
#include <llvm/IR/LLVMContext.h>
#include <llvm/IR/Module.h>
#include <llvm/Support/SourceMgr.h>
#endif

#include <memory>
#include <string>

#include "llvm/object_emitter_internal.h"
#include "llvm/textual_lowering_internal.h"

namespace snow::codegen {

    LoweringResult LlvmLowering::Lower(const snow::sir::Module &module, const TargetConfig &target,
                                       const snow::passes::OptLevel opt_level) const {
        LoweringResult result;
        result.backend = BackendKind::RealLlvm;
        result.native_ready = true;
        result.error_code.clear();
        result.error_message.clear();

        if (const auto unsupported = llvm_backend::FirstUnsupportedOpcode(module); unsupported.has_value()) {
            result.native_ready = false;
            result.error_code = "E_BACKEND_UNSUPPORTED_OPCODE";
            result.error_message = "unsupported opcode in LLVM lowering: " + unsupported.value();
        }

        result.llvm_ir = llvm_backend::LowerTextual(module, target, opt_level);

#if SNOW_ENABLE_LLVM
        if (result.native_ready) {
            llvm::LLVMContext context;
            llvm::SMDiagnostic parse_error;
            std::unique_ptr<llvm::Module> parsed = llvm::parseAssemblyString(result.llvm_ir, parse_error, context);
            if (!parsed) {
                result.native_ready = false;
                result.error_code = "E_BACKEND_LLVM_PARSE";
                result.error_message = "LLVM IR parse failed";
            }
        }
#else
        if (result.native_ready) {
            result.native_ready = false;
            result.error_code = "E_BACKEND_LLVM_REQUIRED";
            result.error_message = "LLVM backend is unavailable";
        }
#endif

        return result;
    }

    ObjectEmitResult LlvmLowering::EmitObject(const snow::sir::Module &module, const TargetConfig &target,
                                              const snow::passes::OptLevel opt_level,
                                              const std::string &output_path) const {
        return llvm_backend::EmitObjectWithLlvmApi(module, target, opt_level, output_path);
    }

} // namespace snow::codegen
