#pragma once

#include <string>

#include "snow/passes/pass_manager.h"

namespace snow::driver {

struct EmitOptions {
  bool tokens = false;
  bool ast = false;
  bool sema = false;
  bool sir = false;
  bool cfg = false;
  bool llvm = false;
};

struct CompileRequest {
  std::string input_path;
  std::string target_triple;
  snow::passes::OptLevel opt_level = snow::passes::OptLevel::O0;
  EmitOptions emit;
};

}  // namespace snow::driver
