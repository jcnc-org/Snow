#pragma once

#include "snow/passes/pass_contract.h"
#include "snow/sir/sir.h"

namespace snow::passes {

    const PassContract &InlineContract();

    bool InlineModule(snow::sir::Module &module);

} // namespace snow::passes
