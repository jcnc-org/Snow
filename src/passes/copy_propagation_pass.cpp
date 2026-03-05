// Module: Copy propagation pass for SSA and load/store alias simplification.

#include "internal_passes.h"

#include <string>
#include <unordered_map>
#include <utility>
#include <vector>

#include "pass_utils.h"

namespace snow::passes::detail {

    const snow::passes::PassContract &CopyPropagationContract() {
        static const snow::passes::PassContract kContract{
                .name = "CopyPropagation",
                .input_invariants = "Pointer load/store relationships are explicit in SIR.",
                .output_invariants = "Replacements are transitively resolved and side-effecting ordering is preserved.",
                .failure_modes = "Unknown aliasing shapes are ignored conservatively.",
        };
        return kContract;
    }

    bool RunCopyPropagation(snow::sir::Function &function) {
        bool changed = false;
        std::unordered_map<std::string, std::string> replacements;

        for (auto &block: function.blocks) {
            std::unordered_map<std::string, std::string> value_aliases;
            std::unordered_map<std::string, std::string> pointer_last_store;
            std::vector<snow::sir::Instruction> rebuilt;
            rebuilt.reserve(block.instructions.size());

            for (auto instr: block.instructions) {
                for (std::size_t i = 0; i < instr.operands.size(); ++i) {
                    if (!OperandIsValuePosition(instr.opcode, i)) {
                        continue;
                    }
                    const std::string resolved = ResolveReplacement(instr.operands[i], value_aliases);
                    if (resolved != instr.operands[i]) {
                        instr.operands[i] = resolved;
                        changed = true;
                    }
                }

                if (instr.result.has_value()) {
                    value_aliases.erase(instr.result.value());
                }

                if (instr.opcode == snow::sir::Opcode::Store) {
                    if (instr.operands.size() == 2) {
                        const std::string stored_value = ResolveReplacement(instr.operands[0], value_aliases);
                        const std::string ptr_value = ResolveReplacement(instr.operands[1], value_aliases);
                        if (stored_value != instr.operands[0]) {
                            instr.operands[0] = stored_value;
                            changed = true;
                        }
                        if (ptr_value != instr.operands[1]) {
                            instr.operands[1] = ptr_value;
                            changed = true;
                        }
                        if (IsValueName(ptr_value)) {
                            pointer_last_store[ptr_value] = stored_value;
                        } else {
                            pointer_last_store.clear();
                        }
                    } else {
                        pointer_last_store.clear();
                    }
                    rebuilt.push_back(std::move(instr));
                    continue;
                }

                if (instr.opcode == snow::sir::Opcode::Load && instr.result.has_value() && instr.operands.size() == 1) {
                    const std::string ptr_value = ResolveReplacement(instr.operands[0], value_aliases);
                    if (ptr_value != instr.operands[0]) {
                        instr.operands[0] = ptr_value;
                        changed = true;
                    }

                    const auto it = pointer_last_store.find(ptr_value);
                    if (it != pointer_last_store.end()) {
                        const std::string replacement = ResolveReplacement(it->second, value_aliases);
                        value_aliases[instr.result.value()] = replacement;
                        replacements[instr.result.value()] = replacement;
                        changed = true;
                        continue;
                    }

                    rebuilt.push_back(std::move(instr));
                    continue;
                }

                if (instr.opcode == snow::sir::Opcode::Call || instr.opcode == snow::sir::Opcode::Drop) {
                    pointer_last_store.clear();
                }

                rebuilt.push_back(std::move(instr));
            }

            block.instructions = std::move(rebuilt);
        }

        ReplaceValueOperands(function, replacements);
        return changed || !replacements.empty();
    }

} // namespace snow::passes::detail
