// Module: Shared helper utilities for SIR passes.

#include "pass_utils.h"

#include <cctype>
#include <unordered_set>
#include <utility>

namespace snow::passes::detail {

    bool IsValueName(const std::string &operand) { return !operand.empty() && operand[0] == '%'; }

    bool IsIntegerLiteral(const std::string &text) {
        if (text.empty()) {
            return false;
        }
        std::size_t i = 0;
        if (text[0] == '+' || text[0] == '-') {
            i = 1;
            if (i >= text.size()) {
                return false;
            }
        }
        for (; i < text.size(); ++i) {
            if (!std::isdigit(static_cast<unsigned char>(text[i]))) {
                return false;
            }
        }
        return true;
    }

    bool IsBooleanLiteral(const std::string &text) { return text == "true" || text == "false"; }

    std::optional<int64_t> ParseInt64(const std::string &text) {
        if (!IsIntegerLiteral(text)) {
            return std::nullopt;
        }
        try {
            return std::stoll(text);
        } catch (...) {
            return std::nullopt;
        }
    }

    std::optional<bool> ParseBool(const std::string &text) {
        if (text == "true") {
            return true;
        }
        if (text == "false") {
            return false;
        }
        return std::nullopt;
    }

    std::string ResolveReplacement(const std::string &value,
                                   const std::unordered_map<std::string, std::string> &replacements) {
        std::string current = value;
        std::unordered_set<std::string> seen;
        while (true) {
            const auto it = replacements.find(current);
            if (it == replacements.end()) {
                break;
            }
            if (seen.contains(current)) {
                break;
            }
            seen.insert(current);
            current = it->second;
        }
        return current;
    }

    bool OperandIsValuePosition(const snow::sir::Opcode opcode, const std::size_t operand_index) {
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
                return operand_index < 2;

            case snow::sir::Opcode::CondBr:
            case snow::sir::Opcode::Ret:
            case snow::sir::Opcode::Drop:
                return operand_index == 0;

            case snow::sir::Opcode::Call:
                return operand_index >= 1;

            case snow::sir::Opcode::Phi:
                return (operand_index % 2) == 0;

            case snow::sir::Opcode::Store:
                return operand_index == 0 || operand_index == 1;

            case snow::sir::Opcode::Load:
                return operand_index == 0;

            default:
                return false;
        }
    }

    void ReplaceValueOperands(snow::sir::Function &function,
                              const std::unordered_map<std::string, std::string> &replacements) {
        if (replacements.empty()) {
            return;
        }

        for (auto &block: function.blocks) {
            for (auto &instr: block.instructions) {
                for (std::size_t i = 0; i < instr.operands.size(); ++i) {
                    if (!OperandIsValuePosition(instr.opcode, i)) {
                        continue;
                    }
                    const std::string replacement = ResolveReplacement(instr.operands[i], replacements);
                    if (replacement != instr.operands[i]) {
                        instr.operands[i] = replacement;
                    }
                }
            }
        }
    }

} // namespace snow::passes::detail
