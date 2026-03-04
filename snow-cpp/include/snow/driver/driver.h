#pragma once

#include <string>

#include "snow/common/diagnostic_engine.h"
#include "snow/driver/compile_options.h"

namespace snow::driver {

struct CompileResult {
  bool success = false;
  std::string token_dump;
  std::string ast_dump;
  std::string sema_dump;
  std::string ownership_dump;
  std::string sir_dump;
  std::string cfg_dump;
  std::string llvm_dump;
  snow::common::DiagnosticEngine diagnostics;
};

class Driver {
 public:
  CompileResult Compile(const CompileRequest& request) const;
};

std::string RenderDiagnostics(const snow::common::DiagnosticEngine& diagnostics);

}  // namespace snow::driver
