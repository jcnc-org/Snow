#include "snow/codegen/lowering.h"

#include <sstream>

namespace snow::codegen {

namespace {

std::string ToLlvmType(const std::string& snow_type) {
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

std::string ZeroValue(const std::string& llvm_type) {
  if (llvm_type == "float") {
    return "0.0";
  }
  if (llvm_type == "double") {
    return "0.0";
  }
  return "0";
}

}  // namespace

LoweringResult LlvmLowering::Lower(const snow::sir::Module& module, const TargetConfig& target,
                                   const snow::passes::OptLevel opt_level) const {
  LoweringResult result;

#if SNOW_ENABLE_LLVM
  result.used_real_llvm = true;
#else
  result.used_real_llvm = false;
#endif

  std::ostringstream oss;
  oss << "; snow llvm ir (textual lowering)\n";
  oss << "target triple = \"" << target.triple << "\"\n";
  oss << "; opt-level = " << (opt_level == snow::passes::OptLevel::O0 ? "O0" : "O2") << "\n\n";

  for (const auto& function : module.functions) {
    const std::string ret_ty = ToLlvmType(function.return_type);
    oss << "define " << ret_ty << " @" << function.name << "() {\n";
    oss << "entry:\n";
    oss << "  ret " << ret_ty << " " << ZeroValue(ret_ty) << "\n";
    oss << "}\n\n";
  }

  result.llvm_ir = oss.str();
  return result;
}

}  // namespace snow::codegen
