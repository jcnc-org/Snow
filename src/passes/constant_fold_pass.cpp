// Module: Constant folding pass for literal arithmetic/comparison simplification.

#include "internal_passes.h"

#include <optional>
#include <string>
#include <unordered_map>
#include <utility>
#include <vector>

#include "pass_utils.h"

namespace snow::passes::detail {

    const snow::passes::PassContract &ConstantFoldContract() {
        static const snow::passes::PassContract kContract{
                .name = "ConstantFold",
                .input_invariants = "Binary ops have type-consistent operands; literals are parseable.",
                .output_invariants =
                        "Folded values preserve type/behavior; removed instructions are dead replacements.",
                .failure_modes = "Divide-by-zero fold is skipped; malformed patterns are left unchanged.",
        };
        return kContract;
    }

    bool RunConstantFold(snow::sir::Function &function) {
        bool changed = false;
        std::unordered_map<std::string, std::string> known_values;

        auto replace_operand = [&](const snow::sir::Opcode opcode, const std::size_t index, std::string &operand) {
            if (!OperandIsValuePosition(opcode, index)) {
                return;
            }
            const auto it = known_values.find(operand);
            if (it != known_values.end()) {
                operand = it->second;
                changed = true;
            }
        };

        for (auto &block: function.blocks) {
            std::vector<snow::sir::Instruction> new_instructions;
            new_instructions.reserve(block.instructions.size());

            for (auto instr: block.instructions) {
                for (std::size_t i = 0; i < instr.operands.size(); ++i) {
                    replace_operand(instr.opcode, i, instr.operands[i]);
                }

                if (instr.result.has_value()) {
                    known_values.erase(instr.result.value());
                }

                auto fold_binary_int = [&](const auto eval_int) {
                    if (!instr.result.has_value() || instr.operands.size() != 2) {
                        return false;
                    }
                    const auto lhs = ParseInt64(instr.operands[0]);
                    const auto rhs = ParseInt64(instr.operands[1]);
                    if (!lhs.has_value() || !rhs.has_value()) {
                        return false;
                    }
                    const auto value = eval_int(lhs.value(), rhs.value());
                    if (!value.has_value()) {
                        return false;
                    }
                    const std::string literal = std::to_string(value.value());
                    known_values[instr.result.value()] = literal;
                    changed = true;
                    return true;
                };

                auto fold_binary_bool = [&](const auto eval_cmp) {
                    if (!instr.result.has_value() || instr.operands.size() != 2) {
                        return false;
                    }
                    const auto lhs = ParseInt64(instr.operands[0]);
                    const auto rhs = ParseInt64(instr.operands[1]);
                    if (!lhs.has_value() || !rhs.has_value()) {
                        return false;
                    }
                    known_values[instr.result.value()] = eval_cmp(lhs.value(), rhs.value()) ? "true" : "false";
                    changed = true;
                    return true;
                };

                bool removed = false;
                switch (instr.opcode) {
                    case snow::sir::Opcode::Add:
                        removed = fold_binary_int(
                                [](const int64_t a, const int64_t b) { return std::optional<int64_t>(a + b); });
                        break;
                    case snow::sir::Opcode::Sub:
                        removed = fold_binary_int(
                                [](const int64_t a, const int64_t b) { return std::optional<int64_t>(a - b); });
                        break;
                    case snow::sir::Opcode::Mul:
                        removed = fold_binary_int(
                                [](const int64_t a, const int64_t b) { return std::optional<int64_t>(a * b); });
                        break;
                    case snow::sir::Opcode::Div:
                        removed = fold_binary_int([](const int64_t a, const int64_t b) {
                            if (b == 0) {
                                return std::optional<int64_t>{};
                            }
                            return std::optional<int64_t>(a / b);
                        });
                        break;
                    case snow::sir::Opcode::Eq:
                        removed = fold_binary_bool([](const int64_t a, const int64_t b) { return a == b; });
                        break;
                    case snow::sir::Opcode::Ne:
                        removed = fold_binary_bool([](const int64_t a, const int64_t b) { return a != b; });
                        break;
                    case snow::sir::Opcode::Lt:
                        removed = fold_binary_bool([](const int64_t a, const int64_t b) { return a < b; });
                        break;
                    case snow::sir::Opcode::Gt:
                        removed = fold_binary_bool([](const int64_t a, const int64_t b) { return a > b; });
                        break;
                    case snow::sir::Opcode::Le:
                        removed = fold_binary_bool([](const int64_t a, const int64_t b) { return a <= b; });
                        break;
                    case snow::sir::Opcode::Ge:
                        removed = fold_binary_bool([](const int64_t a, const int64_t b) { return a >= b; });
                        break;
                    case snow::sir::Opcode::Phi:
                        if (instr.result.has_value() && instr.operands.size() >= 2) {
                            std::string first = instr.operands[0];
                            bool same = true;
                            for (std::size_t i = 2; i + 1 < instr.operands.size(); i += 2) {
                                if (instr.operands[i] != first) {
                                    same = false;
                                    break;
                                }
                            }
                            if (same) {
                                known_values[instr.result.value()] = first;
                                changed = true;
                                removed = true;
                            }
                        }
                        break;
                    case snow::sir::Opcode::CondBr:
                        if (instr.operands.size() == 3) {
                            const auto cond = ParseBool(instr.operands[0]);
                            if (cond.has_value()) {
                                instr.opcode = snow::sir::Opcode::Br;
                                instr.operands = {cond.value() ? instr.operands[1] : instr.operands[2]};
                                changed = true;
                            }
                        }
                        break;
                    default:
                        break;
                }

                if (!removed) {
                    new_instructions.push_back(std::move(instr));
                }
            }

            block.instructions = std::move(new_instructions);
        }

        return changed;
    }

} // namespace snow::passes::detail
