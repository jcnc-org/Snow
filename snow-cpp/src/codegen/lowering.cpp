#include "snow/codegen/lowering.h"

#include <cctype>
#include <sstream>
#include <unordered_map>
#include <vector>

#if SNOW_ENABLE_LLVM
#include <llvm/IR/BasicBlock.h>
#include <llvm/IR/Constants.h>
#include <llvm/IR/DerivedTypes.h>
#include <llvm/IR/Function.h>
#include <llvm/IR/IRBuilder.h>
#include <llvm/IR/LLVMContext.h>
#include <llvm/IR/Module.h>
#include <llvm/Support/raw_ostream.h>
#endif

namespace snow::codegen {

namespace {

std::string ToLlvmTypeText(const std::string& snow_type) {
  if (snow_type == "i1" || snow_type == "bool") {
    return "i1";
  }
  if (snow_type == "i32") {
    return "i32";
  }
  if (snow_type == "i64") {
    return "i64";
  }
  if (snow_type == "f32") {
    return "float";
  }
  if (snow_type == "f64") {
    return "double";
  }
  return "i32";
}

std::string ZeroValueText(const std::string& llvm_type) {
  if (llvm_type == "float" || llvm_type == "double") {
    return "0.0";
  }
  return "0";
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

std::string NormalizeOperand(const std::string& operand) {
  if (operand.empty()) {
    return "0";
  }
  if (operand[0] == '%' || IsIntegerLiteral(operand)) {
    return operand;
  }
  if (operand == "true") {
    return "1";
  }
  if (operand == "false") {
    return "0";
  }
  // Bootstrap fallback for unresolved symbols (for example function names in expressions).
  return "0";
}

std::string InferOperandLlvmTypeText(const std::string& operand,
                                     const std::unordered_map<std::string, std::string>& value_types) {
  if (operand == "true" || operand == "false") {
    return "i1";
  }
  if (!operand.empty() && operand[0] == '%') {
    const auto it = value_types.find(operand);
    if (it != value_types.end()) {
      return ToLlvmTypeText(it->second);
    }
    return "i32";
  }
  if (IsIntegerLiteral(operand)) {
    return "i32";
  }
  return "i32";
}

std::string ComparePredicateText(const snow::sir::Opcode opcode) {
  switch (opcode) {
    case snow::sir::Opcode::Eq:
      return "eq";
    case snow::sir::Opcode::Ne:
      return "ne";
    case snow::sir::Opcode::Lt:
      return "slt";
    case snow::sir::Opcode::Gt:
      return "sgt";
    case snow::sir::Opcode::Le:
      return "sle";
    case snow::sir::Opcode::Ge:
      return "sge";
    default:
      return "eq";
  }
}

const snow::sir::Function* FindUserMain(const snow::sir::Module& module) {
  for (const auto& function : module.functions) {
    if (function.original_name == "main") {
      return &function;
    }
  }
  return nullptr;
}

std::string LinkagePrefixText(const snow::sir::Linkage linkage) {
  switch (linkage) {
    case snow::sir::Linkage::External:
      return "";
    case snow::sir::Linkage::Internal:
      return "internal ";
    case snow::sir::Linkage::Private:
      return "private ";
  }
  return "";
}

#if SNOW_ENABLE_LLVM
llvm::Type* ToLlvmType(llvm::LLVMContext& context, const std::string& snow_type) {
  if (snow_type == "i1" || snow_type == "bool") {
    return llvm::Type::getInt1Ty(context);
  }
  if (snow_type == "i32") {
    return llvm::Type::getInt32Ty(context);
  }
  if (snow_type == "i64") {
    return llvm::Type::getInt64Ty(context);
  }
  if (snow_type == "f32") {
    return llvm::Type::getFloatTy(context);
  }
  if (snow_type == "f64") {
    return llvm::Type::getDoubleTy(context);
  }
  return llvm::Type::getInt32Ty(context);
}

llvm::Constant* ZeroValue(llvm::Type* type) {
  if (type->isFloatingPointTy()) {
    return llvm::ConstantFP::get(type, 0.0);
  }
  if (type->isIntegerTy()) {
    return llvm::ConstantInt::get(type, 0);
  }
  return llvm::Constant::getNullValue(type);
}

llvm::GlobalValue::LinkageTypes ToLlvmLinkage(const snow::sir::Linkage linkage) {
  switch (linkage) {
    case snow::sir::Linkage::External:
      return llvm::GlobalValue::ExternalLinkage;
    case snow::sir::Linkage::Internal:
      return llvm::GlobalValue::InternalLinkage;
    case snow::sir::Linkage::Private:
      return llvm::GlobalValue::PrivateLinkage;
  }
  return llvm::GlobalValue::ExternalLinkage;
}

std::string LowerWithLlvmApi(const snow::sir::Module& module, const TargetConfig& target) {
  llvm::LLVMContext context;
  auto llvm_module = std::make_unique<llvm::Module>("snow_module", context);
  llvm_module->setTargetTriple(target.triple);

  for (const auto& function : module.functions) {
    llvm::Type* ret_type = ToLlvmType(context, function.return_type);
    std::vector<llvm::Type*> arg_types;
    arg_types.reserve(function.params.size());
    for (const auto& param : function.params) {
      arg_types.push_back(ToLlvmType(context, param.type));
    }
    auto* fn_type = llvm::FunctionType::get(ret_type, arg_types, false);
    auto* fn = llvm::Function::Create(fn_type, ToLlvmLinkage(function.linkage), function.name, llvm_module.get());

    auto* entry = llvm::BasicBlock::Create(context, "entry", fn);
    llvm::IRBuilder<> builder(entry);
    builder.CreateRet(ZeroValue(ret_type));
  }

  if (target.executable_entry_wrapper) {
    const snow::sir::Function* user_main = FindUserMain(module);
    if (user_main != nullptr) {
      auto* i32_ty = llvm::Type::getInt32Ty(context);
      auto* ptr_ty = llvm::PointerType::get(context, 0);

      auto* runtime_type = llvm::FunctionType::get(i32_ty, {ptr_ty}, false);
      auto* runtime_fn =
          llvm::Function::Create(runtime_type, llvm::Function::ExternalLinkage, "snow_runtime_start", llvm_module.get());
      if (runtime_fn->empty()) {
        auto* entry = llvm::BasicBlock::Create(context, "entry", runtime_fn);
        llvm::IRBuilder<> runtime_builder(entry);
        auto* user_main_ptr = runtime_fn->getArg(0);
        auto* result = runtime_builder.CreateCall(runtime_type, user_main_ptr, {});
        runtime_builder.CreateRet(result);
      }

      auto* host_type = llvm::FunctionType::get(i32_ty, {}, false);
      auto* host_main = llvm::Function::Create(host_type, llvm::Function::ExternalLinkage, "main", llvm_module.get());
      auto* entry = llvm::BasicBlock::Create(context, "entry", host_main);
      llvm::IRBuilder<> builder(entry);

      auto* user_fn = llvm_module->getFunction(user_main->name);
      llvm::Value* user_ptr = user_fn;
      if (user_fn->getType() != ptr_ty) {
        user_ptr = builder.CreateBitCast(user_fn, ptr_ty);
      }

      auto* call = builder.CreateCall(runtime_fn, {user_ptr});
      builder.CreateRet(call);
    }
  }

  std::string buffer;
  llvm::raw_string_ostream stream(buffer);
  llvm_module->print(stream, nullptr);
  stream.flush();
  return buffer;
}
#endif

std::string LowerTextual(const snow::sir::Module& module, const TargetConfig& target,
                         const snow::passes::OptLevel opt_level) {
  std::ostringstream oss;
  oss << "; snow llvm ir (textual lowering)\n";
  oss << "target triple = \"" << target.triple << "\"\n";
  oss << "; entry-wrapper = " << (target.executable_entry_wrapper ? "enabled" : "disabled") << "\n";
  oss << "; opt-level = " << (opt_level == snow::passes::OptLevel::O0 ? "O0" : "O2") << "\n\n";

  for (const auto& function : module.functions) {
    const std::string ret_ty = ToLlvmTypeText(function.return_type);
    std::unordered_map<std::string, std::string> value_types;
    std::unordered_map<std::string, std::string> pointer_element_types;
    oss << "define " << LinkagePrefixText(function.linkage) << ret_ty << " @" << function.name << "(";
    for (std::size_t i = 0; i < function.params.size(); ++i) {
      if (i > 0) {
        oss << ", ";
      }
      const auto& param = function.params[i];
      oss << ToLlvmTypeText(param.type) << " %" << param.name;
      value_types["%" + param.name] = param.type;
    }
    oss << ") {\n";
    bool emitted_ret = false;
    bool emitted_block = false;

    for (const auto& block : function.blocks) {
      emitted_block = true;
      oss << block.label << ":\n";
      for (const auto& instr : block.instructions) {
        switch (instr.opcode) {
          case snow::sir::Opcode::Add:
          case snow::sir::Opcode::Sub:
          case snow::sir::Opcode::Mul:
          case snow::sir::Opcode::Div: {
            if (!instr.result.has_value() || instr.operands.size() != 2) {
              oss << "  ; malformed arithmetic instruction\n";
              break;
            }
            const std::string op_ty = ToLlvmTypeText(instr.type);
            const std::string lhs = NormalizeOperand(instr.operands[0]);
            const std::string rhs = NormalizeOperand(instr.operands[1]);
            std::string op;
            switch (instr.opcode) {
              case snow::sir::Opcode::Add:
                op = "add";
                break;
              case snow::sir::Opcode::Sub:
                op = "sub";
                break;
              case snow::sir::Opcode::Mul:
                op = "mul";
                break;
              case snow::sir::Opcode::Div:
                op = "sdiv";
                break;
              default:
                op = "add";
                break;
            }
            oss << "  " << instr.result.value() << " = " << op << " " << op_ty << " " << lhs << ", " << rhs << "\n";
            break;
          }

          case snow::sir::Opcode::Eq:
          case snow::sir::Opcode::Ne:
          case snow::sir::Opcode::Lt:
          case snow::sir::Opcode::Gt:
          case snow::sir::Opcode::Le:
          case snow::sir::Opcode::Ge: {
            if (!instr.result.has_value() || instr.operands.size() != 2) {
              oss << "  ; malformed compare instruction\n";
              break;
            }
            const std::string lhs = NormalizeOperand(instr.operands[0]);
            const std::string rhs = NormalizeOperand(instr.operands[1]);
            const std::string cmp_ty = InferOperandLlvmTypeText(instr.operands[0], value_types);
            oss << "  " << instr.result.value() << " = icmp " << ComparePredicateText(instr.opcode) << " " << cmp_ty
                << " " << lhs << ", " << rhs << "\n";
            break;
          }

          case snow::sir::Opcode::Drop:
            if (!instr.operands.empty()) {
              oss << "  ; drop " << instr.operands[0] << "\n";
            } else {
              oss << "  ; drop\n";
            }
            break;

          case snow::sir::Opcode::Alloc: {
            if (!instr.result.has_value()) {
              oss << "  ; malformed alloc instruction\n";
              break;
            }
            const std::string element_type =
                ToLlvmTypeText((instr.operands.empty() || instr.operands[0].empty()) ? "i32" : instr.operands[0]);
            oss << "  " << instr.result.value() << " = alloca " << element_type << "\n";
            pointer_element_types[instr.result.value()] = element_type;
            break;
          }

          case snow::sir::Opcode::Load: {
            if (!instr.result.has_value() || instr.operands.size() != 1) {
              oss << "  ; malformed load instruction\n";
              break;
            }
            const std::string ptr = NormalizeOperand(instr.operands[0]);
            const std::string element_type =
                pointer_element_types.contains(ptr) ? pointer_element_types[ptr]
                                                    : ToLlvmTypeText(instr.type.empty() ? "i32" : instr.type);
            oss << "  " << instr.result.value() << " = load " << element_type << ", ptr " << ptr << "\n";
            break;
          }

          case snow::sir::Opcode::Store: {
            if (instr.operands.size() != 2) {
              oss << "  ; malformed store instruction\n";
              break;
            }
            const std::string value = NormalizeOperand(instr.operands[0]);
            const std::string ptr = NormalizeOperand(instr.operands[1]);
            const std::string element_type = pointer_element_types.contains(ptr)
                                                 ? pointer_element_types[ptr]
                                                 : InferOperandLlvmTypeText(instr.operands[0], value_types);
            oss << "  store " << element_type << " " << value << ", ptr " << ptr << "\n";
            break;
          }

          case snow::sir::Opcode::Phi: {
            if (!instr.result.has_value() || instr.operands.size() < 4 || (instr.operands.size() % 2) != 0) {
              oss << "  ; malformed phi instruction\n";
              break;
            }
            const std::string phi_ty = ToLlvmTypeText(instr.type.empty() ? "i32" : instr.type);
            oss << "  " << instr.result.value() << " = phi " << phi_ty << " ";
            for (std::size_t i = 0; i + 1 < instr.operands.size(); i += 2) {
              if (i > 0) {
                oss << ", ";
              }
              oss << "[ " << NormalizeOperand(instr.operands[i]) << ", %" << instr.operands[i + 1] << " ]";
            }
            oss << "\n";
            break;
          }

          case snow::sir::Opcode::Br:
            if (instr.operands.size() != 1) {
              oss << "  ; malformed br instruction\n";
              break;
            }
            oss << "  br label %" << instr.operands[0] << "\n";
            break;

          case snow::sir::Opcode::CondBr:
            if (instr.operands.size() != 3) {
              oss << "  ; malformed cond_br instruction\n";
              break;
            }
            oss << "  br i1 " << NormalizeOperand(instr.operands[0]) << ", label %" << instr.operands[1]
                << ", label %" << instr.operands[2] << "\n";
            break;

          case snow::sir::Opcode::Call: {
            if (!instr.result.has_value() || instr.operands.empty()) {
              oss << "  ; malformed call instruction\n";
              break;
            }
            const std::string call_ret_ty = ToLlvmTypeText(instr.type.empty() ? "i32" : instr.type);
            const std::string callee = instr.operands[0];
            oss << "  " << instr.result.value() << " = call " << call_ret_ty << " @" << callee << "(";
            for (std::size_t i = 1; i < instr.operands.size(); ++i) {
              if (i > 1) {
                oss << ", ";
              }
              const std::string arg_ty = InferOperandLlvmTypeText(instr.operands[i], value_types);
              oss << arg_ty << " " << NormalizeOperand(instr.operands[i]);
            }
            oss << ")\n";
            break;
          }

          case snow::sir::Opcode::Ret: {
            const std::string value =
                instr.operands.empty() ? ZeroValueText(ret_ty) : NormalizeOperand(instr.operands.front());
            oss << "  ret " << ret_ty << " " << value << "\n";
            emitted_ret = true;
            break;
          }

          default:
            oss << "  ; unsupported instruction: " << snow::sir::ToString(instr.opcode) << "\n";
            break;
        }

        if (instr.result.has_value() && !instr.type.empty()) {
          value_types[instr.result.value()] = instr.type;
        }
      }
    }

    if (!emitted_block) {
      oss << "entry:\n";
    }
    if (!emitted_ret) {
      oss << "  ret " << ret_ty << " " << ZeroValueText(ret_ty) << "\n";
    }
    oss << "}\n\n";
  }

  if (target.executable_entry_wrapper) {
    const snow::sir::Function* user_main = FindUserMain(module);
    if (user_main != nullptr) {
      oss << "define i32 @snow_runtime_start(ptr %user_main) {\n";
      oss << "entry:\n";
      oss << "  %0 = call i32 %user_main()\n";
      oss << "  ret i32 %0\n";
      oss << "}\n\n";
      oss << "define i32 @main() {\n";
      oss << "entry:\n";
      oss << "  %1 = call i32 @snow_runtime_start(ptr @" << user_main->name << ")\n";
      oss << "  ret i32 %1\n";
      oss << "}\n\n";
    }
  }

  return oss.str();
}

}  // namespace

LoweringResult LlvmLowering::Lower(const snow::sir::Module& module, const TargetConfig& target,
                                   const snow::passes::OptLevel opt_level) const {
  LoweringResult result;

#if SNOW_ENABLE_LLVM
  result.used_real_llvm = true;
  result.llvm_ir = LowerWithLlvmApi(module, target);
#else
  result.used_real_llvm = false;
  result.llvm_ir = LowerTextual(module, target, opt_level);
#endif

  return result;
}

}  // namespace snow::codegen
