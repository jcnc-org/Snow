#include "snow/passes/pass_manager.h"
#include "inline_pass.h"

#include <algorithm>
#include <cctype>
#include <cstdint>
#include <optional>
#include <queue>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <utility>
#include <vector>

namespace snow::passes {

namespace {

bool IsValueName(const std::string& operand) {
  return !operand.empty() && operand[0] == '%';
}

bool IsIntegerLiteral(const std::string& text) {
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

bool IsBooleanLiteral(const std::string& text) {
  return text == "true" || text == "false";
}

std::optional<int64_t> ParseInt64(const std::string& text) {
  if (!IsIntegerLiteral(text)) {
    return std::nullopt;
  }
  try {
    return std::stoll(text);
  } catch (...) {
    return std::nullopt;
  }
}

std::optional<bool> ParseBool(const std::string& text) {
  if (text == "true") {
    return true;
  }
  if (text == "false") {
    return false;
  }
  return std::nullopt;
}

std::string ResolveReplacement(const std::string& value,
                               const std::unordered_map<std::string, std::string>& replacements) {
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

void ReplaceValueOperands(snow::sir::Function& function, const std::unordered_map<std::string, std::string>& replacements) {
  if (replacements.empty()) {
    return;
  }
  for (auto& block : function.blocks) {
    for (auto& instr : block.instructions) {
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

bool ConstantFoldFunction(snow::sir::Function& function) {
  bool changed = false;
  std::unordered_map<std::string, std::string> known_values;

  auto replace_operand = [&](const snow::sir::Opcode opcode, const std::size_t index, std::string& operand) {
    if (!OperandIsValuePosition(opcode, index)) {
      return;
    }
    const auto it = known_values.find(operand);
    if (it != known_values.end()) {
      operand = it->second;
      changed = true;
    }
  };

  for (auto& block : function.blocks) {
    std::vector<snow::sir::Instruction> new_instructions;
    new_instructions.reserve(block.instructions.size());

    for (auto instr : block.instructions) {
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
          removed = fold_binary_int([](const int64_t a, const int64_t b) { return std::optional<int64_t>(a + b); });
          break;
        case snow::sir::Opcode::Sub:
          removed = fold_binary_int([](const int64_t a, const int64_t b) { return std::optional<int64_t>(a - b); });
          break;
        case snow::sir::Opcode::Mul:
          removed = fold_binary_int([](const int64_t a, const int64_t b) { return std::optional<int64_t>(a * b); });
          break;
        case snow::sir::Opcode::Div:
          removed = fold_binary_int(
              [](const int64_t a, const int64_t b) {
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

bool CfgSimplifyFunction(snow::sir::Function& function) {
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
    const auto& block = function.blocks[it->second];
    if (block.instructions.empty()) {
      continue;
    }
    const auto& term = block.instructions.back();
    if (term.opcode == snow::sir::Opcode::Br && term.operands.size() == 1) {
      work.push(term.operands[0]);
    } else if (term.opcode == snow::sir::Opcode::CondBr && term.operands.size() == 3) {
      work.push(term.operands[1]);
      work.push(term.operands[2]);
    }
  }

  std::vector<snow::sir::BasicBlock> new_blocks;
  new_blocks.reserve(function.blocks.size());
  for (auto& block : function.blocks) {
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
  for (auto& block : function.blocks) {
    std::vector<snow::sir::Instruction> rebuilt;
    rebuilt.reserve(block.instructions.size());
    for (auto instr : block.instructions) {
      if (instr.opcode == snow::sir::Opcode::CondBr && instr.operands.size() == 3 && instr.operands[1] == instr.operands[2]) {
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

bool CopyPropagateFunction(snow::sir::Function& function) {
  bool changed = false;
  std::unordered_map<std::string, std::string> replacements;

  for (auto& block : function.blocks) {
    std::unordered_map<std::string, std::string> value_aliases;
    std::unordered_map<std::string, std::string> pointer_last_store;
    std::vector<snow::sir::Instruction> rebuilt;
    rebuilt.reserve(block.instructions.size());

    for (auto instr : block.instructions) {
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

bool DeadCodeEliminateFunction(snow::sir::Function& function) {
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
    for (const auto& block : function.blocks) {
      for (const auto& instr : block.instructions) {
        for (const auto& operand : instr.operands) {
          if (IsValueName(operand)) {
            use_count[operand] += 1;
          }
        }
      }
    }

    for (auto& block : function.blocks) {
      std::vector<snow::sir::Instruction> kept;
      kept.reserve(block.instructions.size());
      for (auto& instr : block.instructions) {
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

}  // namespace

PassResult PassManager::Run(const snow::sir::Module& input, const OptLevel level,
                            const snow::sir::ValidationLevel validation_level,
                            const snow::sir::SirValidator& validator,
                            snow::common::DiagnosticEngine& diagnostics) const {
  PassResult result;
  result.module = input;

  auto run_validation = [&]() {
    if (validation_level == snow::sir::ValidationLevel::Debug) {
      (void)validator.Validate(result.module, validation_level, diagnostics);
    }
  };

  if (level == OptLevel::O0) {
    result.executed_passes.push_back("Canonicalize");
    run_validation();
    if (validation_level == snow::sir::ValidationLevel::Release) {
      (void)validator.Validate(result.module, validation_level, diagnostics);
    }
    return result;
  }

  result.executed_passes.push_back("ConstantFold");
  for (auto& function : result.module.functions) {
    (void)ConstantFoldFunction(function);
  }
  run_validation();

  result.executed_passes.push_back("CfgSimplify");
  for (auto& function : result.module.functions) {
    (void)CfgSimplifyFunction(function);
  }
  run_validation();

  result.executed_passes.push_back("CopyPropagation");
  for (auto& function : result.module.functions) {
    (void)CopyPropagateFunction(function);
  }
  run_validation();

  result.executed_passes.push_back("DeadCodeElimination");
  for (auto& function : result.module.functions) {
    (void)DeadCodeEliminateFunction(function);
  }
  run_validation();

  result.executed_passes.push_back("Inline");
  (void)InlineModule(result.module);
  run_validation();

  result.executed_passes.push_back("CfgSimplify");
  for (auto& function : result.module.functions) {
    (void)CfgSimplifyFunction(function);
  }
  run_validation();

  result.executed_passes.push_back("DeadCodeElimination");
  for (auto& function : result.module.functions) {
    (void)DeadCodeEliminateFunction(function);
  }
  run_validation();

  if (validation_level == snow::sir::ValidationLevel::Release) {
    (void)validator.Validate(result.module, validation_level, diagnostics);
  }

  return result;
}

}  // namespace snow::passes
