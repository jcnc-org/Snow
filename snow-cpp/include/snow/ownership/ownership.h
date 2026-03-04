#pragma once

#include <string>
#include <vector>

#include "snow/common/diagnostic_engine.h"
#include "snow/sema/sema.h"

namespace snow::ownership {

struct OwnershipFact {
  std::string symbol;
  std::string name;
  std::string type_name;
  bool is_copy_type = false;
  bool drop_at_exit = false;
  std::size_t declaration_index = 0;
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
