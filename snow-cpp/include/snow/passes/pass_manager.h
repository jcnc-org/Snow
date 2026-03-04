#pragma once

#include <string>
#include <vector>

#include "snow/common/diagnostic_engine.h"
#include "snow/sir/sir.h"
#include "snow/sir/validator.h"

namespace snow::passes {

enum class OptLevel {
  O0,
  O2,
};

struct PassResult {
  snow::sir::Module module;
  std::vector<std::string> executed_passes;
};

class PassManager {
 public:
  PassResult Run(const snow::sir::Module& input, OptLevel level,
                 snow::sir::ValidationLevel validation_level,
                 const snow::sir::SirValidator& validator,
                 snow::common::DiagnosticEngine& diagnostics) const;
};

}  // namespace snow::passes
