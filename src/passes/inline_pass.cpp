// Module: Inline pass for small pure callees.
// Contract: only pure/small callees are inlined; behavior must remain semantically equivalent.

#include "inline_pass.h"

#include <algorithm>
#include <cctype>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <utility>
#include <vector>

#include "pass_utils.h"

namespace snow::passes {

    namespace {

        bool IsInlinePureOpcode(const snow::sir::Opcode opcode) {
            switch (opcode) {
                case snow::sir::Opcode::Add:
                case snow::sir::Opcode::Sub:
                case snow::sir::Opcode::Mul:
                case snow::sir::Opcode::Div:
                case snow::sir::Opcode::Eq:
                case snow::sir::Opcode::Ne:
                case snow::sir::Opcode::Lt:
                case snow::sir::Opcode::Gt:
                case snow::sir::Opcode::Le:
                case snow::sir::Opcode::Ge:
                case snow::sir::Opcode::Extract:
                case snow::sir::Opcode::Insert:
                    return true;
                default:
                    return false;
            }
        }

        bool IsNumericSsaName(const std::string &value) {
            if (value.size() < 2 || value[0] != '%') {
                return false;
            }
            for (std::size_t i = 1; i < value.size(); ++i) {
                if (!std::isdigit(static_cast<unsigned char>(value[i]))) {
                    return false;
                }
            }
            return true;
        }

        int NextSsaId(const snow::sir::Function &function) {
            int max_id = 0;
            for (const auto &block: function.blocks) {
                for (const auto &instr: block.instructions) {
                    if (!instr.result.has_value() || !IsNumericSsaName(instr.result.value())) {
                        continue;
                    }
                    const int id = std::stoi(instr.result.value().substr(1));
                    max_id = std::max(max_id, id);
                }
            }
            return max_id + 1;
        }

        struct InlineCandidate {
            std::vector<snow::sir::FunctionParam> params;
            std::vector<snow::sir::Instruction> body;
            std::string return_operand;
        };

        bool TryBuildInlineCandidate(const snow::sir::Function &function, InlineCandidate &candidate) {
            if (function.original_name == "main" || function.blocks.empty() || function.blocks.size() > 2) {
                return false;
            }

            std::vector<const snow::sir::BasicBlock *> sequence;
            if (function.blocks.size() == 1) {
                sequence.push_back(&function.blocks.front());
            } else {
                const auto &first = function.blocks[0];
                const auto &second = function.blocks[1];
                if (first.instructions.empty()) {
                    return false;
                }
                const auto &term = first.instructions.back();
                if (term.opcode != snow::sir::Opcode::Br || term.operands.size() != 1 ||
                    term.operands[0] != second.label) {
                    return false;
                }
                sequence.push_back(&first);
                sequence.push_back(&second);
            }

            std::unordered_set<std::string> defs;
            for (const auto &param: function.params) {
                defs.insert("%" + param.name);
            }

            std::vector<snow::sir::Instruction> body;
            std::string ret_operand;
            bool saw_ret = false;

            for (std::size_t block_index = 0; block_index < sequence.size(); ++block_index) {
                const auto &block = *sequence[block_index];
                for (std::size_t i = 0; i < block.instructions.size(); ++i) {
                    const auto &instr = block.instructions[i];
                    const bool last_instr = i + 1 == block.instructions.size();

                    if (instr.opcode == snow::sir::Opcode::Br) {
                        if (function.blocks.size() != 2 || block_index != 0 || !last_instr ||
                            instr.operands.size() != 1 || instr.operands[0] != sequence[1]->label) {
                            return false;
                        }
                        continue;
                    }

                    if (instr.opcode == snow::sir::Opcode::Ret) {
                        if (block_index + 1 != sequence.size() || !last_instr || instr.operands.size() != 1) {
                            return false;
                        }
                        saw_ret = true;
                        ret_operand = instr.operands[0];
                        continue;
                    }

                    if (!IsInlinePureOpcode(instr.opcode) || !instr.result.has_value()) {
                        return false;
                    }

                    for (std::size_t op_i = 0; op_i < instr.operands.size(); ++op_i) {
                        if (!detail::OperandIsValuePosition(instr.opcode, op_i)) {
                            continue;
                        }
                        if (detail::IsValueName(instr.operands[op_i]) && !defs.contains(instr.operands[op_i])) {
                            return false;
                        }
                    }

                    defs.insert(instr.result.value());
                    body.push_back(instr);
                }
            }

            if (!saw_ret) {
                return false;
            }
            if (detail::IsValueName(ret_operand) && !defs.contains(ret_operand)) {
                return false;
            }

            candidate.params = function.params;
            candidate.body = std::move(body);
            candidate.return_operand = std::move(ret_operand);
            return true;
        }

        bool InlineFunction(snow::sir::Function &function,
                            const std::unordered_map<std::string, InlineCandidate> &candidates) {
            if (function.blocks.empty() || candidates.empty()) {
                return false;
            }

            bool changed = false;
            int next_ssa = NextSsaId(function);
            std::unordered_map<std::string, std::string> cross_block_replacements;

            for (auto &block: function.blocks) {
                std::unordered_map<std::string, std::string> local_replacements = cross_block_replacements;
                std::vector<snow::sir::Instruction> rebuilt;
                rebuilt.reserve(block.instructions.size());

                for (auto instr: block.instructions) {
                    for (std::size_t op_i = 0; op_i < instr.operands.size(); ++op_i) {
                        if (!detail::OperandIsValuePosition(instr.opcode, op_i)) {
                            continue;
                        }
                        const std::string resolved =
                                detail::ResolveReplacement(instr.operands[op_i], local_replacements);
                        if (resolved != instr.operands[op_i]) {
                            instr.operands[op_i] = resolved;
                            changed = true;
                        }
                    }

                    if (instr.opcode == snow::sir::Opcode::Call && !instr.operands.empty()) {
                        const auto it = candidates.find(instr.operands[0]);
                        if (it != candidates.end() && instr.operands[0] != function.name) {
                            const InlineCandidate &candidate = it->second;
                            if (instr.operands.size() - 1 == candidate.params.size()) {
                                std::unordered_map<std::string, std::string> value_map;
                                for (std::size_t arg_i = 0; arg_i < candidate.params.size(); ++arg_i) {
                                    value_map["%" + candidate.params[arg_i].name] =
                                            detail::ResolveReplacement(instr.operands[arg_i + 1], local_replacements);
                                }

                                for (const auto &templ: candidate.body) {
                                    snow::sir::Instruction cloned = templ;
                                    for (std::size_t op_i = 0; op_i < cloned.operands.size(); ++op_i) {
                                        if (!detail::OperandIsValuePosition(cloned.opcode, op_i)) {
                                            continue;
                                        }
                                        if (detail::IsValueName(cloned.operands[op_i])) {
                                            cloned.operands[op_i] =
                                                    detail::ResolveReplacement(cloned.operands[op_i], value_map);
                                        }
                                    }
                                    if (cloned.result.has_value()) {
                                        const std::string old_name = cloned.result.value();
                                        const std::string new_name = "%" + std::to_string(next_ssa++);
                                        value_map[old_name] = new_name;
                                        cloned.result = new_name;
                                    }
                                    rebuilt.push_back(std::move(cloned));
                                }

                                std::string inlined_result = candidate.return_operand;
                                if (detail::IsValueName(inlined_result)) {
                                    inlined_result = detail::ResolveReplacement(inlined_result, value_map);
                                }

                                if (instr.result.has_value()) {
                                    local_replacements[instr.result.value()] = inlined_result;
                                    cross_block_replacements[instr.result.value()] = inlined_result;
                                }
                                changed = true;
                                continue;
                            }
                        }
                    }

                    rebuilt.push_back(std::move(instr));
                }

                block.instructions = std::move(rebuilt);
            }

            detail::ReplaceValueOperands(function, cross_block_replacements);
            return changed || !cross_block_replacements.empty();
        }

    } // namespace

    const PassContract &InlineContract() {
        static const PassContract kContract{
                .name = "Inline",
                .input_invariants = "Candidates are small pure callees with analyzable structure.",
                .output_invariants = "Inlined operands remapped to caller SSA names; recursive self-inline skipped.",
                .failure_modes = "Unsupported candidate shape is skipped; malformed remap caught by validator.",
        };
        return kContract;
    }

    bool InlineModule(snow::sir::Module &module) {
        std::unordered_map<std::string, InlineCandidate> candidates;
        candidates.reserve(module.functions.size());

        for (const auto &function: module.functions) {
            InlineCandidate candidate;
            if (TryBuildInlineCandidate(function, candidate)) {
                candidates[function.name] = std::move(candidate);
            }
        }

        bool changed = false;
        for (auto &function: module.functions) {
            changed = InlineFunction(function, candidates) || changed;
        }
        return changed;
    }

} // namespace snow::passes
