#include "snow/ownership/ownership.h"

#include <sstream>
#include <unordered_set>

namespace snow::ownership {

namespace {

bool IsCopyType(const std::string& type_name) {
  static const std::unordered_set<std::string> kCopyTypes = {
      "bool",
      "i1",
      "i32",
      "i64",
      "f32",
      "f64",
  };
  return kCopyTypes.contains(type_name);
}

}  // namespace

OwnershipFacts OwnershipChecker::Check(const snow::sema::SemaModule& sema_module,
                                       snow::common::DiagnosticEngine& diagnostics) const {
  OwnershipFacts facts;

  for (const auto& function : sema_module.ast.functions) {
    for (const auto& param : function.params) {
      facts.facts.push_back(OwnershipFact{
          .symbol = function.name + "::" + param.name,
          .is_copy_type = IsCopyType(param.type),
      });
    }

    if (function.return_type.empty()) {
      diagnostics.Error("E_OWNERSHIP_RET_TYPE", "Function missing return type: " + function.name,
                        sema_module.ast.module_path, {0, 0, 0, 0});
    }
  }

  return facts;
}

std::string DumpOwnership(const OwnershipFacts& facts) {
  std::ostringstream oss;
  oss << "ownership-facts\n";
  for (const auto& fact : facts.facts) {
    oss << "  - " << fact.symbol << " copy=" << (fact.is_copy_type ? "true" : "false") << "\n";
  }
  oss << "model: ownership + deterministic drop (no implicit GC)\n";
  return oss.str();
}

}  // namespace snow::ownership
