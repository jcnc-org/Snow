#include "snow/codegen/lowering.h"

#include <sstream>

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
    auto* fn_type = llvm::FunctionType::get(ret_type, {}, false);
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
      auto* runtime_fn = llvm::Function::Create(runtime_type, llvm::Function::ExternalLinkage, "snow_runtime_start",
                                                llvm_module.get());

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
    oss << "define " << LinkagePrefixText(function.linkage) << ret_ty << " @" << function.name << "() {\n";
    oss << "entry:\n";
    oss << "  ret " << ret_ty << " " << ZeroValueText(ret_ty) << "\n";
    oss << "}\n\n";
  }

  if (target.executable_entry_wrapper) {
    const snow::sir::Function* user_main = FindUserMain(module);
    if (user_main != nullptr) {
      oss << "declare i32 @snow_runtime_start(ptr)\n\n";
      oss << "define i32 @main() {\n";
      oss << "entry:\n";
      oss << "  %0 = call i32 @snow_runtime_start(ptr @" << user_main->name << ")\n";
      oss << "  ret i32 %0\n";
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
