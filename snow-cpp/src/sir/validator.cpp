#include "snow/sir/validator.h"

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

}  // namespace

ValidationReport SirValidator::Validate(const Module& module, const ValidationLevel level,
                                        snow::common::DiagnosticEngine& diagnostics) const {
  ValidationReport report;

  for (const auto& function : module.functions) {
    std::unordered_set<std::string> defs;
    std::unordered_map<std::string, int> drop_counts;
    std::unordered_set<std::string> block_labels;

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
