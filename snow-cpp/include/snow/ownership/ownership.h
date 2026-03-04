#pragma once

#include <string>
#include <vector>

#include "snow/common/diagnostic_engine.h"
#include "snow/sema/sema.h"

namespace snow::ownership {

struct OwnershipFact {
  std::string symbol;
  bool is_copy_type = false;
};

struct OwnershipFacts {
  std::vector<OwnershipFact> facts;
};

class OwnershipChecker {
 public:
  OwnershipFacts Check(const snow::sema::SemaModule& sema_module,
                       snow::common::DiagnosticEngine& diagnostics) const;
};

std::string DumpOwnership(const OwnershipFacts& facts);

}  // namespace snow::ownership
