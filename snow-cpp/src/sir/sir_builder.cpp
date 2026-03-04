#include "snow/sir/sir_builder.h"

#include <functional>
#include <sstream>
#include <unordered_map>
#include <utility>

#include "snow/common/mangling.h"

namespace snow::sir {

namespace {

struct EmittedValue {
  std::string value;
  std::string type;
};

struct LoopTargets {
  std::string break_label;
  std::string continue_label;
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

bool IsTerminatorOpcode(const Opcode opcode) {
  return opcode == Opcode::Br || opcode == Opcode::CondBr || opcode == Opcode::Ret || opcode == Opcode::Unreachable;
}

bool IsBooleanType(const std::string& type) {
  return type == "bool" || type == "i1";
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

bool BlockTerminated(const BasicBlock& block) {
  if (block.instructions.empty()) {
    return false;
  }
  const auto& last = block.instructions.back();
  return last.is_terminator || IsTerminatorOpcode(last.opcode);
}

EmittedValue DefaultReturnValue(const std::string& function_return_type) {
  if (IsBooleanType(function_return_type)) {
    return EmittedValue{.value = "false", .type = "bool"};
  }
  return EmittedValue{.value = "0", .type = function_return_type.empty() ? "i32" : function_return_type};
}

using SymbolPtrMap = std::unordered_map<std::string, std::string>;

EmittedValue EmitExpr(const std::shared_ptr<snow::frontend::Expr>& expr,
                      const std::unordered_map<std::string, std::string>& symbol_types,
                      const SymbolPtrMap& symbol_ptrs,
                      const std::unordered_map<std::string, std::string>& callee_symbols,
                      const std::unordered_map<std::string, std::string>& callee_return_types,
                      std::vector<Instruction>& instructions, int& next_ssa_id) {
  if (!expr) {
    return EmittedValue{.value = "0", .type = "i32"};
  }

  switch (expr->kind) {
    case snow::frontend::Expr::Kind::Number:
      return EmittedValue{.value = expr->value, .type = "i32"};

    case snow::frontend::Expr::Kind::Identifier: {
      const auto ptr_it = symbol_ptrs.find(expr->value);
      const auto type_it = symbol_types.find(expr->value);
      if (ptr_it != symbol_ptrs.end() && type_it != symbol_types.end()) {
        const std::string result_name = "%" + std::to_string(next_ssa_id++);
        instructions.push_back(Instruction{
            .result = result_name,
            .type = type_it->second,
            .opcode = Opcode::Load,
            .operands = {ptr_it->second},
            .is_terminator = false,
        });
        return EmittedValue{.value = result_name, .type = type_it->second};
      }

      if (type_it != symbol_types.end()) {
        return EmittedValue{.value = expr->value, .type = type_it->second};
      }

      return EmittedValue{.value = expr->value, .type = "i32"};
    }

    case snow::frontend::Expr::Kind::Call: {
      std::vector<std::string> operands;
      const auto callee_it = callee_symbols.find(expr->value);
      operands.push_back(callee_it != callee_symbols.end() ? callee_it->second : expr->value);

      for (const auto& arg : expr->args) {
        const EmittedValue arg_value =
            EmitExpr(arg, symbol_types, symbol_ptrs, callee_symbols, callee_return_types, instructions, next_ssa_id);
        operands.push_back(arg_value.value);
      }

      const auto ret_it = callee_return_types.find(expr->value);
      const std::string call_ret_type = ret_it != callee_return_types.end() ? ret_it->second : "i32";
      const std::string result_name = "%" + std::to_string(next_ssa_id++);

      instructions.push_back(Instruction{
          .result = result_name,
          .type = call_ret_type,
          .opcode = Opcode::Call,
          .operands = std::move(operands),
          .is_terminator = false,
      });

      return EmittedValue{.value = result_name, .type = call_ret_type};
    }

    case snow::frontend::Expr::Kind::Binary: {
      if (expr->op == snow::frontend::BinaryOp::Mod) {
        // Unsupported in MVP; semantic analyzer reports E_SEMA_UNSUPPORTED_OP.
        return EmittedValue{.value = "0", .type = "i32"};
      }

      const EmittedValue lhs =
          EmitExpr(expr->lhs, symbol_types, symbol_ptrs, callee_symbols, callee_return_types, instructions, next_ssa_id);
      const EmittedValue rhs =
          EmitExpr(expr->rhs, symbol_types, symbol_ptrs, callee_symbols, callee_return_types, instructions, next_ssa_id);

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

  std::unordered_map<std::string, std::vector<snow::ownership::OwnershipFact>> ownership_facts_by_function;
  for (const auto& fact : ownership_facts.facts) {
    const auto sep = fact.symbol.find("::");
    if (sep == std::string::npos) {
      continue;
    }
    const std::string function_name = fact.symbol.substr(0, sep);
    ownership_facts_by_function[function_name].push_back(fact);
  }

  std::unordered_map<std::string, std::string> callee_symbols;
  std::unordered_map<std::string, std::string> callee_return_types;
  for (const auto& function_ast : sema_module.ast.functions) {
    std::vector<std::string> param_types;
    param_types.reserve(function_ast.params.size());
    for (const auto& param : function_ast.params) {
      param_types.push_back(param.type);
    }
    const std::string mangled = snow::common::MangleSymbol(sema_module.ast.module_path, function_ast.name, param_types,
                                                            function_ast.return_type, false);
    callee_symbols[function_ast.name] = mangled;
    callee_return_types[function_ast.name] = function_ast.return_type.empty() ? "i32" : function_ast.return_type;
  }

  for (const auto& function_ast : sema_module.ast.functions) {
    Function function;
    function.original_name = function_ast.name;
    std::vector<std::string> param_types;
    param_types.reserve(function_ast.params.size());
    for (const auto& param : function_ast.params) {
      param_types.push_back(param.type);
    }
    const auto callee_it = callee_symbols.find(function_ast.name);
    function.name = callee_it != callee_symbols.end()
                        ? callee_it->second
                        : snow::common::MangleSymbol(sema_module.ast.module_path, function_ast.name, param_types,
                                                     function_ast.return_type, false);
    function.return_type = function_ast.return_type.empty() ? "i32" : function_ast.return_type;
    for (const auto& param : function_ast.params) {
      function.params.push_back(FunctionParam{
          .name = param.name,
          .type = param.type,
      });
    }
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

    std::unordered_map<std::string, std::string> symbol_types;
    SymbolPtrMap symbol_ptrs;
    for (const auto& param : function_ast.params) {
      symbol_types[param.name] = param.type;
    }

    std::unordered_map<std::string, int> block_label_count;
    auto next_label = [&](const std::string& base) {
      int& count = block_label_count[base];
      if (count == 0) {
        count = 1;
        return base;
      }
      const std::string label = base + "_" + std::to_string(count);
      ++count;
      return label;
    };

    function.blocks.push_back(BasicBlock{.label = "entry"});
    block_label_count["entry"] = 1;
    std::size_t current_block_idx = 0;
    const std::string return_label = next_label("fn_return");
    std::vector<std::pair<std::string, std::string>> return_incoming;
    int next_ssa_id = 1;
    std::vector<LoopTargets> loop_stack;

    auto block_terminated = [&](const std::size_t index) {
      return BlockTerminated(function.blocks[index]);
    };

    auto create_block = [&](const std::string& base) {
      function.blocks.push_back(BasicBlock{.label = next_label(base)});
      return function.blocks.size() - 1;
    };

    auto emit_branch_to = [&](const std::string& target_label) {
      if (block_terminated(current_block_idx)) {
        return;
      }
      function.blocks[current_block_idx].instructions.push_back(Instruction{
          .result = std::nullopt,
          .type = "void",
          .opcode = Opcode::Br,
          .operands = {target_label},
          .is_terminator = true,
      });
    };

    auto append_owned_drops = [&](BasicBlock& block) {
      const auto it = ownership_facts_by_function.find(function_ast.name);
      if (it == ownership_facts_by_function.end()) {
        return;
      }

      for (auto rit = it->second.rbegin(); rit != it->second.rend(); ++rit) {
        if (rit->is_copy_type || !rit->drop_at_exit) {
          continue;
        }
        block.instructions.push_back(Instruction{
            .result = std::nullopt,
            .type = rit->type_name.empty() ? "unknown" : rit->type_name,
            .opcode = Opcode::Drop,
            .operands = {rit->name.empty() ? rit->symbol : rit->name},
            .is_terminator = false,
        });
      }
    };

    auto emit_return_jump = [&](const std::shared_ptr<snow::frontend::Expr>& expr) {
      if (block_terminated(current_block_idx)) {
        return;
      }

      EmittedValue return_value = DefaultReturnValue(function.return_type);
      if (expr) {
        return_value = EmitExpr(expr, symbol_types, symbol_ptrs, callee_symbols, callee_return_types,
                                function.blocks[current_block_idx].instructions, next_ssa_id);
      }

      return_incoming.emplace_back(return_value.value, function.blocks[current_block_idx].label);
      function.blocks[current_block_idx].instructions.push_back(Instruction{
          .result = std::nullopt,
          .type = "void",
          .opcode = Opcode::Br,
          .operands = {return_label},
          .is_terminator = true,
      });
    };

    // Materialize parameters into stack slots so assignment semantics are explicit via load/store.
    for (const auto& param : function_ast.params) {
      const std::string slot_name = "%" + std::to_string(next_ssa_id++);
      function.blocks[current_block_idx].instructions.push_back(Instruction{
          .result = slot_name,
          .type = "ptr",
          .opcode = Opcode::Alloc,
          .operands = {param.type},
          .is_terminator = false,
      });
      function.blocks[current_block_idx].instructions.push_back(Instruction{
          .result = std::nullopt,
          .type = param.type,
          .opcode = Opcode::Store,
          .operands = {"%" + param.name, slot_name},
          .is_terminator = false,
      });
      symbol_ptrs[param.name] = slot_name;
    }

    std::function<void(const std::vector<snow::frontend::Statement>&)> emit_statements;
    emit_statements = [&](const std::vector<snow::frontend::Statement>& statements) {
      for (const auto& stmt : statements) {
        if (block_terminated(current_block_idx)) {
          break;
        }

        switch (stmt.kind) {
          case snow::frontend::Statement::Kind::Expr:
            (void)EmitExpr(stmt.expr, symbol_types, symbol_ptrs, callee_symbols, callee_return_types,
                           function.blocks[current_block_idx].instructions, next_ssa_id);
            break;

          case snow::frontend::Statement::Kind::Assign: {
            if (stmt.name.empty() || !symbol_ptrs.contains(stmt.name)) {
              break;
            }
            EmittedValue assigned = DefaultReturnValue(symbol_types[stmt.name]);
            if (stmt.expr) {
              assigned = EmitExpr(stmt.expr, symbol_types, symbol_ptrs, callee_symbols, callee_return_types,
                                  function.blocks[current_block_idx].instructions, next_ssa_id);
            }
            function.blocks[current_block_idx].instructions.push_back(Instruction{
                .result = std::nullopt,
                .type = assigned.type.empty() ? symbol_types[stmt.name] : assigned.type,
                .opcode = Opcode::Store,
                .operands = {assigned.value, symbol_ptrs[stmt.name]},
                .is_terminator = false,
            });
            break;
          }

          case snow::frontend::Statement::Kind::Let: {
            EmittedValue init = DefaultReturnValue("i32");
            if (stmt.expr) {
              init = EmitExpr(stmt.expr, symbol_types, symbol_ptrs, callee_symbols, callee_return_types,
                              function.blocks[current_block_idx].instructions, next_ssa_id);
            }
            if (!stmt.name.empty()) {
              const std::string value_type =
                  !stmt.type_name.empty() ? stmt.type_name : (init.type.empty() ? "i32" : init.type);
              const std::string slot_name = "%" + std::to_string(next_ssa_id++);
              function.blocks[current_block_idx].instructions.push_back(Instruction{
                  .result = slot_name,
                  .type = "ptr",
                  .opcode = Opcode::Alloc,
                  .operands = {value_type},
                  .is_terminator = false,
              });
              function.blocks[current_block_idx].instructions.push_back(Instruction{
                  .result = std::nullopt,
                  .type = value_type,
                  .opcode = Opcode::Store,
                  .operands = {init.value, slot_name},
                  .is_terminator = false,
              });
              symbol_types[stmt.name] = value_type;
              symbol_ptrs[stmt.name] = slot_name;
            }
            break;
          }

          case snow::frontend::Statement::Kind::Return:
            emit_return_jump(stmt.expr);
            break;

          case snow::frontend::Statement::Kind::Break:
            if (!loop_stack.empty()) {
              emit_branch_to(loop_stack.back().break_label);
            } else {
              function.blocks[current_block_idx].instructions.push_back(Instruction{
                  .result = std::nullopt,
                  .type = "void",
                  .opcode = Opcode::Unreachable,
                  .operands = {},
                  .is_terminator = true,
              });
            }
            break;

          case snow::frontend::Statement::Kind::Continue:
            if (!loop_stack.empty()) {
              emit_branch_to(loop_stack.back().continue_label);
            } else {
              function.blocks[current_block_idx].instructions.push_back(Instruction{
                  .result = std::nullopt,
                  .type = "void",
                  .opcode = Opcode::Unreachable,
                  .operands = {},
                  .is_terminator = true,
              });
            }
            break;

          case snow::frontend::Statement::Kind::If: {
            const EmittedValue cond_value =
                EmitExpr(stmt.expr, symbol_types, symbol_ptrs, callee_symbols, callee_return_types,
                         function.blocks[current_block_idx].instructions, next_ssa_id);

            const auto saved_symbol_types = symbol_types;
            const auto saved_symbol_ptrs = symbol_ptrs;

            const std::size_t then_idx = create_block("if_then");
            std::size_t else_idx = then_idx;
            if (!stmt.else_body.empty()) {
              else_idx = create_block("if_else");
            }
            const std::size_t merge_idx = create_block("if_merge");

            function.blocks[current_block_idx].instructions.push_back(Instruction{
                .result = std::nullopt,
                .type = "void",
                .opcode = Opcode::CondBr,
                .operands = {cond_value.value, function.blocks[then_idx].label, function.blocks[else_idx].label},
                .is_terminator = true,
            });

            current_block_idx = then_idx;
            symbol_types = saved_symbol_types;
            symbol_ptrs = saved_symbol_ptrs;
            emit_statements(stmt.then_body);
            const bool then_falls_through = !block_terminated(current_block_idx);
            if (then_falls_through) {
              emit_branch_to(function.blocks[merge_idx].label);
            }

            bool else_falls_through = false;
            if (!stmt.else_body.empty()) {
              current_block_idx = else_idx;
              symbol_types = saved_symbol_types;
              symbol_ptrs = saved_symbol_ptrs;
              emit_statements(stmt.else_body);
              else_falls_through = !block_terminated(current_block_idx);
              if (else_falls_through) {
                emit_branch_to(function.blocks[merge_idx].label);
              }
            } else {
              else_falls_through = true;
            }

            if (!stmt.else_body.empty() && !then_falls_through && !else_falls_through) {
              if (merge_idx + 1 == function.blocks.size()) {
                function.blocks.pop_back();
              }
              symbol_types = saved_symbol_types;
              symbol_ptrs = saved_symbol_ptrs;
              current_block_idx = then_idx;
              break;
            }

            symbol_types = saved_symbol_types;
            symbol_ptrs = saved_symbol_ptrs;
            current_block_idx = merge_idx;
            break;
          }

          case snow::frontend::Statement::Kind::While: {
            const auto saved_symbol_types = symbol_types;
            const auto saved_symbol_ptrs = symbol_ptrs;

            const std::size_t cond_idx = create_block("loop_cond");
            const std::size_t body_idx = create_block("loop_body");
            const std::size_t exit_idx = create_block("loop_exit");

            emit_branch_to(function.blocks[cond_idx].label);

            current_block_idx = cond_idx;
            symbol_types = saved_symbol_types;
            symbol_ptrs = saved_symbol_ptrs;
            const EmittedValue cond_value =
                EmitExpr(stmt.expr, symbol_types, symbol_ptrs, callee_symbols, callee_return_types,
                         function.blocks[current_block_idx].instructions, next_ssa_id);
            function.blocks[current_block_idx].instructions.push_back(Instruction{
                .result = std::nullopt,
                .type = "void",
                .opcode = Opcode::CondBr,
                .operands = {cond_value.value, function.blocks[body_idx].label, function.blocks[exit_idx].label},
                .is_terminator = true,
            });

            current_block_idx = body_idx;
            symbol_types = saved_symbol_types;
            symbol_ptrs = saved_symbol_ptrs;
            loop_stack.push_back(LoopTargets{
                .break_label = function.blocks[exit_idx].label,
                .continue_label = function.blocks[cond_idx].label,
            });
            emit_statements(stmt.body);
            loop_stack.pop_back();
            if (!block_terminated(current_block_idx)) {
              emit_branch_to(function.blocks[cond_idx].label);
            }

            symbol_types = saved_symbol_types;
            symbol_ptrs = saved_symbol_ptrs;
            current_block_idx = exit_idx;
            break;
          }
        }
      }
    };

    emit_statements(function_ast.statements);
    if (!block_terminated(current_block_idx)) {
      emit_return_jump(nullptr);
    }

    if (return_incoming.empty()) {
      const std::size_t fallback_idx = create_block("fn_fallback");
      current_block_idx = fallback_idx;
      emit_return_jump(nullptr);
    }

    BasicBlock return_block;
    return_block.label = return_label;
    std::string ret_operand = DefaultReturnValue(function.return_type).value;
    if (return_incoming.size() == 1) {
      ret_operand = return_incoming.front().first;
    } else {
      const std::string phi_name = "%" + std::to_string(next_ssa_id++);
      std::vector<std::string> operands;
      operands.reserve(return_incoming.size() * 2);
      for (const auto& incoming : return_incoming) {
        operands.push_back(incoming.first);
        operands.push_back(incoming.second);
      }
      return_block.instructions.push_back(Instruction{
          .result = phi_name,
          .type = function.return_type,
          .opcode = Opcode::Phi,
          .operands = std::move(operands),
          .is_terminator = false,
      });
      ret_operand = phi_name;
    }

    append_owned_drops(return_block);
    return_block.instructions.push_back(Instruction{
        .result = std::nullopt,
        .type = function.return_type,
        .opcode = Opcode::Ret,
        .operands = {ret_operand},
        .is_terminator = true,
    });
    function.blocks.push_back(std::move(return_block));
    module.functions.push_back(std::move(function));
  }

  return module;
}

}  // namespace snow::sir
