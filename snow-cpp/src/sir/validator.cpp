#include "snow/sir/validator.h"

#include <cctype>
#include <unordered_map>
#include <unordered_set>

namespace snow::sir {

namespace {

bool IsTerminator(const Opcode opcode) {
  return opcode == Opcode::Br || opcode == Opcode::CondBr || opcode == Opcode::Ret || opcode == Opcode::Unreachable;
}

bool LooksLikeValue(const std::string& operand) {
  return !operand.empty() && operand[0] == '%';
}

bool IsNumericType(const std::string& type) {
  return type == "i32" || type == "i64";
}

bool IsBooleanType(const std::string& type) {
  return type == "bool" || type == "i1";
}

std::optional<std::string> InferOperandType(const std::string& operand,
                                            const std::unordered_map<std::string, std::string>& value_types) {
  if (LooksLikeValue(operand)) {
    const auto it = value_types.find(operand);
    if (it != value_types.end()) {
      return it->second;
    }
    return std::nullopt;
  }

  if (operand == "true" || operand == "false") {
    return "bool";
  }

  bool numeric = !operand.empty();
  std::size_t start = 0;
  if (numeric && (operand[0] == '-' || operand[0] == '+')) {
    start = 1;
    numeric = start < operand.size();
  }
  for (std::size_t i = start; i < operand.size(); ++i) {
    if (!std::isdigit(static_cast<unsigned char>(operand[i]))) {
      numeric = false;
      break;
    }
  }
  if (numeric) {
    return "i32";
  }

  return std::nullopt;
}

}  // namespace

ValidationReport SirValidator::Validate(const Module& module, const ValidationLevel level,
                                        snow::common::DiagnosticEngine& diagnostics) const {
  ValidationReport report;

  for (const auto& function : module.functions) {
    std::unordered_set<std::string> defs;
    std::unordered_map<std::string, int> drop_counts;
    std::unordered_set<std::string> block_labels;
    std::unordered_map<std::string, std::string> value_types;

    for (const auto& block : function.blocks) {
      block_labels.insert(block.label);
    }

    for (const auto& block : function.blocks) {
      if (block.instructions.empty()) {
        diagnostics.Error("E_SIR_EMPTY_BLOCK", "BasicBlock has no instructions: " + block.label, module.module_path,
                          {0, 0, 0, 0});
        report.ok = false;
        continue;
      }

      bool non_phi_seen = false;
      int terminator_count = 0;

      for (std::size_t i = 0; i < block.instructions.size(); ++i) {
        const auto& instr = block.instructions[i];

        if (instr.opcode == Opcode::Phi && non_phi_seen) {
          diagnostics.Error("E_SIR_PHI_ORDER", "phi instruction must be at block start", module.module_path,
                            {0, 0, 0, 0});
          report.ok = false;
        }
        if (instr.opcode != Opcode::Phi) {
          non_phi_seen = true;
        }

        if (instr.result.has_value()) {
          if (defs.contains(instr.result.value())) {
            diagnostics.Error("E_SIR_SSA_REDEF", "SSA redefinition: " + instr.result.value(), module.module_path,
                              {0, 0, 0, 0});
            report.ok = false;
          }
          defs.insert(instr.result.value());
          if (!instr.type.empty()) {
            value_types[instr.result.value()] = instr.type;
          }
        }

        for (const auto& operand : instr.operands) {
          if (LooksLikeValue(operand) && !defs.contains(operand) && instr.opcode != Opcode::Phi) {
            diagnostics.Warning("W_SIR_USE_BEFORE_DEF", "Use before def candidate: " + operand, module.module_path,
                                {0, 0, 0, 0});
          }
        }

        if (IsTerminator(instr.opcode) || instr.is_terminator) {
          ++terminator_count;
          if (i + 1 != block.instructions.size()) {
            diagnostics.Error("E_SIR_TERM_NOT_LAST", "Terminator must be final instruction", module.module_path,
                              {0, 0, 0, 0});
            report.ok = false;
          }
        }

        switch (instr.opcode) {
          case Opcode::Add:
          case Opcode::Sub:
          case Opcode::Mul:
          case Opcode::Div:
          case Opcode::Eq:
          case Opcode::Ne:
          case Opcode::Lt:
          case Opcode::Gt:
          case Opcode::Le:
          case Opcode::Ge:
            if (instr.operands.size() != 2) {
              diagnostics.Error("E_SIR_ARITY", "Binary instruction requires 2 operands", module.module_path,
                                {0, 0, 0, 0});
              report.ok = false;
            } else {
              const auto lhs_type = InferOperandType(instr.operands[0], value_types);
              const auto rhs_type = InferOperandType(instr.operands[1], value_types);
              if (lhs_type.has_value() && rhs_type.has_value() && lhs_type.value() != rhs_type.value()) {
                diagnostics.Error("E_SIR_TYPE_MISMATCH", "Binary operands have incompatible types", module.module_path,
                                  {0, 0, 0, 0});
                report.ok = false;
              }
              if (instr.opcode == Opcode::Add || instr.opcode == Opcode::Sub || instr.opcode == Opcode::Mul ||
                  instr.opcode == Opcode::Div) {
                if (!instr.type.empty() && !IsNumericType(instr.type)) {
                  diagnostics.Error("E_SIR_TYPE_ARITH", "Arithmetic instruction result must be numeric",
                                    module.module_path, {0, 0, 0, 0});
                  report.ok = false;
                }
              } else if (!instr.type.empty() && !IsBooleanType(instr.type)) {
                diagnostics.Error("E_SIR_TYPE_CMP", "Comparison instruction result must be bool/i1",
                                  module.module_path, {0, 0, 0, 0});
                report.ok = false;
              }
            }
            break;
          case Opcode::CondBr:
            if (instr.operands.size() != 3) {
              diagnostics.Error("E_SIR_CONDBR_ARITY", "cond_br requires condition and two targets", module.module_path,
                                {0, 0, 0, 0});
              report.ok = false;
            } else {
              const auto& true_label = instr.operands[1];
              const auto& false_label = instr.operands[2];
              if (!block_labels.contains(true_label) || !block_labels.contains(false_label)) {
                diagnostics.Error("E_SIR_CFG_TARGET", "cond_br target label does not exist", module.module_path,
                                  {0, 0, 0, 0});
                report.ok = false;
              }
            }
            break;
          case Opcode::Br:
            if (instr.operands.size() != 1) {
              diagnostics.Error("E_SIR_BR_ARITY", "br requires exactly one target label", module.module_path,
                                {0, 0, 0, 0});
              report.ok = false;
            } else if (!block_labels.contains(instr.operands[0])) {
              diagnostics.Error("E_SIR_CFG_TARGET", "br target label does not exist", module.module_path,
                                {0, 0, 0, 0});
              report.ok = false;
            }
            break;
          case Opcode::Drop:
            if (instr.operands.size() != 1) {
              diagnostics.Error("E_SIR_DROP_ARITY", "drop requires exactly one operand", module.module_path,
                                {0, 0, 0, 0});
              report.ok = false;
            } else {
              const auto& dropped = instr.operands[0];
              drop_counts[dropped] += 1;
              if (drop_counts[dropped] > 1) {
                diagnostics.Error("E_SIR_DOUBLE_DROP", "value dropped more than once: " + dropped, module.module_path,
                                  {0, 0, 0, 0});
                report.ok = false;
              }
            }
            break;
          case Opcode::Call:
            if (instr.operands.empty()) {
              diagnostics.Error("E_SIR_CALL_ARITY", "call requires callee operand", module.module_path,
                                {0, 0, 0, 0});
              report.ok = false;
            }
            if (!instr.result.has_value()) {
              diagnostics.Warning("W_SIR_CALL_NO_RESULT", "call result is ignored in MVP pipeline", module.module_path,
                                  {0, 0, 0, 0});
            }
            break;
          case Opcode::Ret:
            if (instr.operands.size() != 1) {
              diagnostics.Error("E_SIR_RET_ARITY", "ret requires exactly one operand", module.module_path,
                                {0, 0, 0, 0});
              report.ok = false;
            } else {
              const auto ret_type = InferOperandType(instr.operands[0], value_types);
              if (ret_type.has_value() && !function.return_type.empty() && ret_type.value() != function.return_type) {
                const bool bool_compat =
                    IsBooleanType(ret_type.value()) && IsBooleanType(function.return_type);
                const bool i32_to_i64 = ret_type.value() == "i32" && function.return_type == "i64";
                if (!bool_compat && !i32_to_i64) {
                  diagnostics.Error("E_SIR_RET_TYPE", "ret operand type does not match function return type",
                                    module.module_path, {0, 0, 0, 0});
                  report.ok = false;
                }
              }
            }
            break;
          default:
            break;
        }
      }

      if (terminator_count != 1) {
        diagnostics.Error("E_SIR_TERM_COUNT", "BasicBlock must have exactly one terminator", module.module_path,
                          {0, 0, 0, 0});
        report.ok = false;
      }
    }
  }

  report.notes.push_back(level == ValidationLevel::Debug ? "validated(debug)" : "validated(release)");
  return report;
}

}  // namespace snow::sir
