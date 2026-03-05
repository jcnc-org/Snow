// Module: Pass orchestration, deterministic pass ordering, and timing capture.

#include "snow/passes/pass_manager.h"

#include <chrono>
#include <functional>
#include <utility>
#include <vector>

#include "inline_pass.h"
#include "internal_passes.h"

namespace snow::passes {

    namespace {

        struct ModulePass {
            const PassContract *contract = nullptr;
            std::function<void(snow::sir::Module &)> run;
        };

        struct FunctionPass {
            const PassContract *contract = nullptr;
            std::function<bool(snow::sir::Function &)> run;
        };

        std::vector<PassContract> BuildContractsFor(const OptLevel level) {
            if (level == OptLevel::O0) {
                return {
                        PassContract{
                                .name = "Canonicalize",
                                .input_invariants = "Validated SIR module with explicit CFG blocks.",
                                .output_invariants = "No semantic change; remains validator-compatible.",
                                .failure_modes = "Invalid pre-state is reported by sir-validator.",
                        },
                };
            }

            return {
                    detail::ConstantFoldContract(),
                    detail::CfgSimplifyContract(),
                    detail::CopyPropagationContract(),
                    detail::DeadCodeEliminationContract(),
                    InlineContract(),
                    detail::CfgSimplifyContract(),
                    detail::DeadCodeEliminationContract(),
            };
        }

    } // namespace

    std::vector<PassContract> PassManager::ContractsFor(const OptLevel level) const { return BuildContractsFor(level); }

    PassResult PassManager::Run(const snow::sir::Module &input, const OptLevel level,
                                const snow::sir::ValidationLevel validation_level,
                                const snow::sir::SirValidator &validator,
                                snow::common::DiagnosticEngine &diagnostics) const {
        PassResult result;
        result.module = input;

        auto run_validation = [&]() {
            if (validation_level == snow::sir::ValidationLevel::Debug) {
                (void) validator.Validate(result.module, validation_level, diagnostics);
            }
        };

        auto run_function_pass = [&](const FunctionPass &pass) {
            const auto start = std::chrono::steady_clock::now();
            for (auto &function: result.module.functions) {
                (void) pass.run(function);
            }
            const auto end = std::chrono::steady_clock::now();
            const double wall_ms = std::chrono::duration<double, std::milli>(end - start).count();
            result.executed_passes.push_back(pass.contract->name);
            result.timings.push_back(PassResult::PassTiming{.pass_name = pass.contract->name, .wall_ms = wall_ms});
            run_validation();
        };

        auto run_module_pass = [&](const ModulePass &pass) {
            const auto start = std::chrono::steady_clock::now();
            pass.run(result.module);
            const auto end = std::chrono::steady_clock::now();
            const double wall_ms = std::chrono::duration<double, std::milli>(end - start).count();
            result.executed_passes.push_back(pass.contract->name);
            result.timings.push_back(PassResult::PassTiming{.pass_name = pass.contract->name, .wall_ms = wall_ms});
            run_validation();
        };

        if (level == OptLevel::O0) {
            const auto contracts = BuildContractsFor(level);
            const auto start = std::chrono::steady_clock::now();
            const auto end = std::chrono::steady_clock::now();
            const double wall_ms = std::chrono::duration<double, std::milli>(end - start).count();
            result.executed_passes.push_back(contracts.front().name);
            result.timings.push_back(PassResult::PassTiming{.pass_name = contracts.front().name, .wall_ms = wall_ms});
            run_validation();
            if (validation_level == snow::sir::ValidationLevel::Release) {
                (void) validator.Validate(result.module, validation_level, diagnostics);
            }
            return result;
        }

        const FunctionPass constant_fold{
                .contract = &detail::ConstantFoldContract(),
                .run = [](snow::sir::Function &function) { return detail::RunConstantFold(function); },
        };
        const FunctionPass cfg_simplify{
                .contract = &detail::CfgSimplifyContract(),
                .run = [](snow::sir::Function &function) { return detail::RunCfgSimplify(function); },
        };
        const FunctionPass copy_propagation{
                .contract = &detail::CopyPropagationContract(),
                .run = [](snow::sir::Function &function) { return detail::RunCopyPropagation(function); },
        };
        const FunctionPass dead_code_elimination{
                .contract = &detail::DeadCodeEliminationContract(),
                .run = [](snow::sir::Function &function) { return detail::RunDeadCodeElimination(function); },
        };
        const ModulePass inline_pass{
                .contract = &InlineContract(),
                .run = [](snow::sir::Module &module) { (void) InlineModule(module); },
        };

        run_function_pass(constant_fold);
        run_function_pass(cfg_simplify);
        run_function_pass(copy_propagation);
        run_function_pass(dead_code_elimination);
        run_module_pass(inline_pass);
        run_function_pass(cfg_simplify);
        run_function_pass(dead_code_elimination);

        if (validation_level == snow::sir::ValidationLevel::Release) {
            (void) validator.Validate(result.module, validation_level, diagnostics);
        }

        return result;
    }

} // namespace snow::passes
