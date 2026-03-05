// Module: Dead code elimination pass for unused non-side-effect SSA definitions.

#include "internal_passes.h"

#include <unordered_map>
#include <vector>

#include "pass_utils.h"

namespace snow::passes::detail {

    const snow::passes::PassContract &DeadCodeEliminationContract() {
        static const snow::passes::PassContract kContract{
                .name = "DeadCodeElimination",
                .input_invariants = "SSA def-use graph can be derived from instruction operands.",
                .output_invariants = "Only unused non-side-effecting defs are removed.",
                .failure_modes = "If use-count reasoning is inconsistent, validator should fail later.",
        };
        return kContract;
    }

    bool RunDeadCodeElimination(snow::sir::Function &function) {
        bool changed_any = false;
        bool changed = true;

        auto is_side_effecting = [](const snow::sir::Opcode opcode) {
            switch (opcode) {
                case snow::sir::Opcode::Br:
                case snow::sir::Opcode::CondBr:
                case snow::sir::Opcode::Ret:
                case snow::sir::Opcode::Unreachable:
                case snow::sir::Opcode::Store:
                case snow::sir::Opcode::Drop:
                case snow::sir::Opcode::Call:
                    return true;
                default:
                    return false;
            }
        };

        while (changed) {
            changed = false;
            std::unordered_map<std::string, int> use_count;
            for (const auto &block: function.blocks) {
                for (const auto &instr: block.instructions) {
                    for (const auto &operand: instr.operands) {
                        if (IsValueName(operand)) {
                            use_count[operand] += 1;
                        }
                    }
                }
            }

            for (auto &block: function.blocks) {
                std::vector<snow::sir::Instruction> kept;
                kept.reserve(block.instructions.size());
                for (auto &instr: block.instructions) {
                    if (instr.result.has_value() && !is_side_effecting(instr.opcode)) {
                        const auto it = use_count.find(instr.result.value());
                        if (it == use_count.end() || it->second == 0) {
                            changed = true;
                            changed_any = true;
                            continue;
                        }
                    }
                    kept.push_back(std::move(instr));
                }
                block.instructions = std::move(kept);
            }
        }

        return changed_any;
    }

} // namespace snow::passes::detail
