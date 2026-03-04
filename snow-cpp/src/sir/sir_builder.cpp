#include "snow/sir/sir_builder.h"

#include <sstream>
#include <unordered_map>

#include "snow/common/mangling.h"

namespace snow::sir {

namespace {

struct EmittedValue {
  std::string value;
  std::string type;
};

bool IsComparisonOp(const snow::frontend::BinaryOp op) {
  switch (op) {
    case snow::frontend::BinaryOp::Eq:
    case snow::frontend::BinaryOp::Ne:
    case snow::frontend::BinaryOp::Lt:
    case snow::frontend::BinaryOp::Gt:
    case snow::frontend::BinaryOp::Le:
    case snow::frontend::BinaryOp::Ge:
      return true;
    default:
      return false;
  }
}

std::string MergeNumericType(const std::string& a, const std::string& b) {
  if (a == "i64" || b == "i64") {
    return "i64";
  }
  return "i32";
}

Opcode ToSirOpcode(const snow::frontend::BinaryOp op) {
  switch (op) {
    case snow::frontend::BinaryOp::Add:
      return Opcode::Add;
    case snow::frontend::BinaryOp::Sub:
      return Opcode::Sub;
    case snow::frontend::BinaryOp::Mul:
      return Opcode::Mul;
    case snow::frontend::BinaryOp::Div:
      return Opcode::Div;
    case snow::frontend::BinaryOp::Eq:
      return Opcode::Eq;
    case snow::frontend::BinaryOp::Ne:
      return Opcode::Ne;
    case snow::frontend::BinaryOp::Lt:
      return Opcode::Lt;
    case snow::frontend::BinaryOp::Gt:
      return Opcode::Gt;
    case snow::frontend::BinaryOp::Le:
      return Opcode::Le;
    case snow::frontend::BinaryOp::Ge:
      return Opcode::Ge;
    case snow::frontend::BinaryOp::Mod:
      return Opcode::Unreachable;
  }
  return Opcode::Add;
}

EmittedValue EmitExpr(const std::shared_ptr<snow::frontend::Expr>& expr,
                      const std::unordered_map<std::string, std::string>& symbol_types,
                      std::vector<Instruction>& instructions, int& next_ssa_id) {
  if (!expr) {
    return EmittedValue{.value = "0", .type = "i32"};
  }

  switch (expr->kind) {
    case snow::frontend::Expr::Kind::Number:
      return EmittedValue{.value = expr->value, .type = "i32"};

    case snow::frontend::Expr::Kind::Identifier: {
      const auto it = symbol_types.find(expr->value);
      if (it != symbol_types.end()) {
        return EmittedValue{.value = expr->value, .type = it->second};
      }
      return EmittedValue{.value = expr->value, .type = "i32"};
    }

    case snow::frontend::Expr::Kind::Binary: {
      if (expr->op == snow::frontend::BinaryOp::Mod) {
        // Unsupported in MVP; semantic analyzer reports E_SEMA_UNSUPPORTED_OP.
        return EmittedValue{.value = "0", .type = "i32"};
      }

      const EmittedValue lhs = EmitExpr(expr->lhs, symbol_types, instructions, next_ssa_id);
      const EmittedValue rhs = EmitExpr(expr->rhs, symbol_types, instructions, next_ssa_id);

      const std::string result_name = "%" + std::to_string(next_ssa_id++);
      const bool comparison = IsComparisonOp(expr->op);
      const std::string result_type = comparison ? "bool" : MergeNumericType(lhs.type, rhs.type);

      instructions.push_back(Instruction{
          .result = result_name,
          .type = result_type,
          .opcode = ToSirOpcode(expr->op),
          .operands = {lhs.value, rhs.value},
          .is_terminator = false,
      });

      return EmittedValue{.value = result_name, .type = result_type};
    }
  }

  return EmittedValue{.value = "0", .type = "i32"};
}

}  // namespace

std::string ToString(const Opcode opcode) {
  switch (opcode) {
    case Opcode::Add:
      return "add";
    case Opcode::Sub:
      return "sub";
    case Opcode::Mul:
      return "mul";
    case Opcode::Div:
      return "div";
    case Opcode::Eq:
      return "eq";
    case Opcode::Ne:
      return "ne";
    case Opcode::Lt:
      return "lt";
    case Opcode::Gt:
      return "gt";
    case Opcode::Le:
      return "le";
    case Opcode::Ge:
      return "ge";
    case Opcode::Phi:
      return "phi";
    case Opcode::Br:
      return "br";
    case Opcode::CondBr:
      return "cond_br";
    case Opcode::Ret:
      return "ret";
    case Opcode::Unreachable:
      return "unreachable";
    case Opcode::Alloc:
      return "alloc";
    case Opcode::Load:
      return "load";
    case Opcode::Store:
      return "store";
    case Opcode::Drop:
      return "drop";
    case Opcode::Call:
      return "call";
    case Opcode::Extract:
      return "extract";
    case Opcode::Insert:
      return "insert";
  }
  return "unknown";
}

std::string ToString(const Linkage linkage) {
  switch (linkage) {
    case Linkage::External:
      return "external";
    case Linkage::Internal:
      return "internal";
    case Linkage::Private:
      return "private";
  }
  return "internal";
}

std::string DumpSir(const Module& module) {
  std::ostringstream oss;
  oss << "sir module " << module.module_path << "\n";
  for (const auto& function : module.functions) {
    oss << "fn " << function.name;
    oss << " ; linkage=" << ToString(function.linkage);
    if (!function.original_name.empty()) {
      oss << " ; original=" << function.original_name;
    }
    oss << "\n";
    for (const auto& block : function.blocks) {
      oss << block.label << ":\n";
      for (const auto& instr : block.instructions) {
        oss << "  ";
        if (instr.result.has_value()) {
          oss << instr.result.value() << " = ";
        }
        oss << ToString(instr.opcode);
        if (!instr.operands.empty()) {
          oss << " ";
          for (std::size_t i = 0; i < instr.operands.size(); ++i) {
            if (i > 0) {
              oss << ", ";
            }
            oss << instr.operands[i];
          }
        }
        if (!instr.type.empty()) {
          oss << " : " << instr.type;
        }
        oss << "\n";
      }
    }
  }
  return oss.str();
}

std::string DumpCfg(const Module& module) {
  std::ostringstream oss;
  oss << "cfg module " << module.module_path << "\n";
  for (const auto& function : module.functions) {
    oss << "function " << function.name << "\n";
    for (const auto& block : function.blocks) {
      oss << "  block " << block.label << " -> ";
      if (block.instructions.empty()) {
        oss << "<invalid>\n";
        continue;
      }
      const auto& term = block.instructions.back();
      if (term.opcode == Opcode::Br && !term.operands.empty()) {
        oss << term.operands[0];
      } else if (term.opcode == Opcode::CondBr && term.operands.size() >= 3) {
        oss << term.operands[1] << ", " << term.operands[2];
      } else if (term.opcode == Opcode::Ret) {
        oss << "<ret>";
      } else {
        oss << "<term:" << ToString(term.opcode) << ">";
      }
      oss << "\n";
    }
  }
  return oss.str();
}

Module SirBuilder::Build(const snow::sema::SemaModule& sema_module,
                         const snow::ownership::OwnershipFacts& ownership_facts) const {
  Module module;
  module.module_path = sema_module.ast.module_path;

  std::unordered_map<std::string, bool> ownership_by_symbol;
  for (const auto& fact : ownership_facts.facts) {
    ownership_by_symbol[fact.symbol] = fact.is_copy_type;
  }

  for (const auto& function_ast : sema_module.ast.functions) {
    Function function;
    function.original_name = function_ast.name;
    std::vector<std::string> param_types;
    param_types.reserve(function_ast.params.size());
    for (const auto& param : function_ast.params) {
      param_types.push_back(param.type);
    }
    function.name = snow::common::MangleSymbol(sema_module.ast.module_path, function_ast.name, param_types,
                                               function_ast.return_type, false);
    function.return_type = function_ast.return_type;
    switch (function_ast.visibility) {
      case snow::frontend::Visibility::Public:
        function.linkage = Linkage::External;
        break;
      case snow::frontend::Visibility::Internal:
        function.linkage = Linkage::Internal;
        break;
      case snow::frontend::Visibility::Private:
      default:
        function.linkage = Linkage::Private;
        break;
    }

    BasicBlock entry;
    entry.label = "entry";

    std::unordered_map<std::string, std::string> symbol_types;
    for (const auto& param : function_ast.params) {
      symbol_types[param.name] = param.type;
    }

    int next_ssa_id = 1;
    EmittedValue return_value;
    if (function_ast.return_expr) {
      return_value = EmitExpr(function_ast.return_expr, symbol_types, entry.instructions, next_ssa_id);
    } else if (function.return_type == "bool" || function.return_type == "i1") {
      return_value = EmittedValue{.value = "0", .type = "bool"};
    } else {
      return_value = EmittedValue{.value = "0", .type = function.return_type.empty() ? "i32" : function.return_type};
    }

    for (const auto& param : function_ast.params) {
      const std::string key = function_ast.name + "::" + param.name;
      const bool copy_type = ownership_by_symbol.contains(key) ? ownership_by_symbol[key] : false;
      if (!copy_type) {
        entry.instructions.push_back(Instruction{
            .result = std::nullopt,
            .type = param.type,
            .opcode = Opcode::Drop,
            .operands = {param.name},
            .is_terminator = false,
        });
      }
    }

    entry.instructions.push_back(Instruction{
        .result = std::nullopt,
        .type = function.return_type,
        .opcode = Opcode::Ret,
        .operands = {return_value.value},
        .is_terminator = true,
    });

    function.blocks.push_back(std::move(entry));
    module.functions.push_back(std::move(function));
  }

  return module;
}

}  // namespace snow::sir
