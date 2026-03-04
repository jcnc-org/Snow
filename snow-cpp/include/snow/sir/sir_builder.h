#pragma once

#include "snow/ownership/ownership.h"
#include "snow/sema/sema.h"
#include "snow/sir/sir.h"

namespace snow::sir {

class SirBuilder {
 public:
  Module Build(const snow::sema::SemaModule& sema_module,
               const snow::ownership::OwnershipFacts& ownership_facts) const;
};

}  // namespace snow::sir
