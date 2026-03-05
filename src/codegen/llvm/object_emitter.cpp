// Module: LLVM API object emission backend.

#include "object_emitter_internal.h"

#if SNOW_ENABLE_LLVM
#include <llvm/AsmParser/Parser.h>
#include <llvm/IR/LLVMContext.h>
#include <llvm/IR/LegacyPassManager.h>
#include <llvm/IR/Module.h>
#include <llvm/IR/Verifier.h>
#include <llvm/MC/TargetRegistry.h>
#include <llvm/Support/FileSystem.h>
#include <llvm/Support/SourceMgr.h>
#include <llvm/Support/TargetSelect.h>
#include <llvm/Support/raw_ostream.h>
#include <llvm/Target/TargetMachine.h>
#include <llvm/Target/TargetOptions.h>
#include <llvm/TargetParser/Triple.h>
#endif

#include <optional>
#include <string>

#include "textual_lowering_internal.h"

namespace snow::codegen::llvm_backend {

#if SNOW_ENABLE_LLVM
    namespace {

        void InitializeTargetsOnce() {
            static const bool initialized = []() {
                LLVMInitializeX86TargetInfo();
                LLVMInitializeX86Target();
                LLVMInitializeX86TargetMC();
                LLVMInitializeX86AsmParser();
                LLVMInitializeX86AsmPrinter();

                LLVMInitializeAArch64TargetInfo();
                LLVMInitializeAArch64Target();
                LLVMInitializeAArch64TargetMC();
                LLVMInitializeAArch64AsmParser();
                LLVMInitializeAArch64AsmPrinter();
                return true;
            }();
            (void) initialized;
        }

    } // namespace
#endif

    ObjectEmitResult EmitObjectWithLlvmApi(const snow::sir::Module &module, const TargetConfig &target,
                                           const snow::passes::OptLevel opt_level, const std::string &output_path) {
#if SNOW_ENABLE_LLVM
        if (const auto unsupported = FirstUnsupportedOpcode(module); unsupported.has_value()) {
            return ObjectEmitResult{
                    .success = false,
                    .error_code = "E_BACKEND_UNSUPPORTED_OPCODE",
                    .error_message = "unsupported opcode in LLVM lowering: " + unsupported.value(),
            };
        }

        llvm::LLVMContext context;
        llvm::SMDiagnostic parse_error;
        const std::string textual_ir = LowerTextual(module, target, opt_level);
        std::unique_ptr<llvm::Module> llvm_module = llvm::parseAssemblyString(textual_ir, parse_error, context);
        if (!llvm_module) {
            std::string error;
            llvm::raw_string_ostream error_stream(error);
            parse_error.print("snowc", error_stream);
            error_stream.flush();
            return ObjectEmitResult{
                    .success = false,
                    .error_code = "E_BACKEND_LLVM_PARSE",
                    .error_message = "LLVM IR parse failed: " + error,
            };
        }

        InitializeTargetsOnce();

        std::string target_error;
        const llvm::Target *llvm_target = llvm::TargetRegistry::lookupTarget(target.triple, target_error);
        if (llvm_target == nullptr) {
            return ObjectEmitResult{
                    .success = false,
                    .error_code = "E_TARGET_UNSUPPORTED",
                    .error_message = "target lookup failed for '" + target.triple + "': " + target_error,
            };
        }

        llvm::TargetOptions target_options;
        llvm::Triple triple(target.triple);
        std::optional<llvm::Reloc::Model> reloc_model = std::nullopt;
        std::unique_ptr<llvm::TargetMachine> machine(
                llvm_target->createTargetMachine(triple, "generic", "", target_options, reloc_model));
        if (!machine) {
            return ObjectEmitResult{
                    .success = false,
                    .error_code = "E_BACKEND_TARGET_MACHINE",
                    .error_message = "cannot create target machine for '" + target.triple + "'",
            };
        }

        llvm_module->setDataLayout(machine->createDataLayout());
        llvm_module->setTargetTriple(triple);

        std::string verify_error;
        llvm::raw_string_ostream verify_stream(verify_error);
        if (llvm::verifyModule(*llvm_module, &verify_stream)) {
            verify_stream.flush();
            return ObjectEmitResult{
                    .success = false,
                    .error_code = "E_BACKEND_LLVM_VERIFY",
                    .error_message = "module verification failed: " + verify_error,
            };
        }

        std::error_code ec;
        llvm::raw_fd_ostream output(output_path, ec, llvm::sys::fs::OF_None);
        if (ec) {
            return ObjectEmitResult{
                    .success = false,
                    .error_code = "E_BACKEND_OBJECT_EMIT",
                    .error_message = "cannot open output object: " + ec.message(),
            };
        }

        llvm::legacy::PassManager pass_manager;
        if (machine->addPassesToEmitFile(pass_manager, output, nullptr, llvm::CodeGenFileType::ObjectFile)) {
            return ObjectEmitResult{
                    .success = false,
                    .error_code = "E_BACKEND_OBJECT_EMIT",
                    .error_message = "target does not support object emission for '" + target.triple + "'",
            };
        }

        pass_manager.run(*llvm_module);
        output.flush();
        return ObjectEmitResult{
                .success = true,
                .error_code = "",
                .error_message = "",
        };
#else
        (void) module;
        (void) target;
        (void) opt_level;
        (void) output_path;
        return ObjectEmitResult{
                .success = false,
                .error_code = "E_BACKEND_LLVM_REQUIRED",
                .error_message = "LLVM backend is unavailable",
        };
#endif
    }

} // namespace snow::codegen::llvm_backend
