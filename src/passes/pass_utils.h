#pragma once

#include <cstdint>
#include <optional>
#include <string>
#include <unordered_map>

#include "snow/sir/sir.h"

namespace snow::passes::detail {

    bool IsValueName(const std::string &operand);
    bool IsIntegerLiteral(const std::string &text);
    bool IsBooleanLiteral(const std::string &text);
    std::optional<int64_t> ParseInt64(const std::string &text);
    std::optional<bool> ParseBool(const std::string &text);

    std::string ResolveReplacement(const std::string &value,
                                   const std::unordered_map<std::string, std::string> &replacements);

    bool OperandIsValuePosition(snow::sir::Opcode opcode, std::size_t operand_index);

    void ReplaceValueOperands(snow::sir::Function &function,
                              const std::unordered_map<std::string, std::string> &replacements);

} // namespace snow::passes::detail
