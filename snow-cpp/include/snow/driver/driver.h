#pragma once

#include <string>
#include <vector>

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

struct BuildRequest {
  std::string project_root;
  std::string target_triple;
  snow::passes::OptLevel opt_level = snow::passes::OptLevel::O0;
  OutputKind output_kind = OutputKind::Executable;
  EmitOptions emit;
};

struct BuildResult {
  bool success = false;
  std::vector<std::string> module_order;
  std::vector<CompileResult> module_compiles;
  std::string summary;
  snow::common::DiagnosticEngine diagnostics;
};

class Driver {
 public:
  CompileResult Compile(const CompileRequest& request) const;
  BuildResult BuildProject(const BuildRequest& request) const;
};

std::string RenderDiagnostics(const snow::common::DiagnosticEngine& diagnostics);

}  // namespace snow::driver
