// Module: CFG simplification pass (reachability pruning and branch cleanup).

#include "internal_passes.h"

#include <queue>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <utility>
#include <vector>

#include "pass_utils.h"

namespace snow::passes::detail {

    const snow::passes::PassContract &CfgSimplifyContract() {
        static const snow::passes::PassContract kContract{
                .name = "CfgSimplify",
                .input_invariants = "Block labels are unique and terminators are present.",
                .output_invariants = "Unreachable blocks removed; branch targets valid; phi labels stay valid.",
                .failure_modes = "Malformed CFG is reported by validator after pass checkpoints.",
        };
        return kContract;
    }

    bool RunCfgSimplify(snow::sir::Function &function) {
        if (function.blocks.empty()) {
            return false;
        }

        bool changed = false;

        std::unordered_map<std::string, std::size_t> label_to_index;
        for (std::size_t i = 0; i < function.blocks.size(); ++i) {
            label_to_index[function.blocks[i].label] = i;
        }

        std::unordered_set<std::string> reachable;
        std::queue<std::string> work;
        work.push(function.blocks.front().label);

        while (!work.empty()) {
            const std::string label = work.front();
            work.pop();
            if (reachable.contains(label)) {
                continue;
            }
            reachable.insert(label);

            const auto it = label_to_index.find(label);
            if (it == label_to_index.end()) {
                continue;
            }
            const auto &block = function.blocks[it->second];
            if (block.instructions.empty()) {
                continue;
            }
            const auto &term = block.instructions.back();
            if (term.opcode == snow::sir::Opcode::Br && term.operands.size() == 1) {
                work.push(term.operands[0]);
            } else if (term.opcode == snow::sir::Opcode::CondBr && term.operands.size() == 3) {
                work.push(term.operands[1]);
                work.push(term.operands[2]);
            }
        }

        std::vector<snow::sir::BasicBlock> new_blocks;
        new_blocks.reserve(function.blocks.size());
        for (auto &block: function.blocks) {
            if (reachable.contains(block.label)) {
                new_blocks.push_back(std::move(block));
            } else {
                changed = true;
            }
        }
        function.blocks = std::move(new_blocks);

        label_to_index.clear();
        std::unordered_set<std::string> existing_labels;
        for (std::size_t i = 0; i < function.blocks.size(); ++i) {
            label_to_index[function.blocks[i].label] = i;
            existing_labels.insert(function.blocks[i].label);
        }

        std::unordered_map<std::string, std::string> phi_replacements;
        for (auto &block: function.blocks) {
            std::vector<snow::sir::Instruction> rebuilt;
            rebuilt.reserve(block.instructions.size());
            for (auto instr: block.instructions) {
                if (instr.opcode == snow::sir::Opcode::CondBr && instr.operands.size() == 3 &&
                    instr.operands[1] == instr.operands[2]) {
                    instr.opcode = snow::sir::Opcode::Br;
                    instr.operands = {instr.operands[1]};
                    changed = true;
                }

                if (instr.opcode == snow::sir::Opcode::Phi) {
                    std::vector<std::string> filtered;
                    for (std::size_t i = 0; i + 1 < instr.operands.size(); i += 2) {
                        if (existing_labels.contains(instr.operands[i + 1])) {
                            filtered.push_back(instr.operands[i]);
                            filtered.push_back(instr.operands[i + 1]);
                        } else {
                            changed = true;
                        }
                    }

                    if (filtered.size() == 2 && instr.result.has_value()) {
                        phi_replacements[instr.result.value()] = filtered[0];
                        changed = true;
                        continue;
                    }

                    instr.operands = std::move(filtered);
                }

                rebuilt.push_back(std::move(instr));
            }
            block.instructions = std::move(rebuilt);
        }

        ReplaceValueOperands(function, phi_replacements);
        return changed || !phi_replacements.empty();
    }

} // namespace snow::passes::detail
