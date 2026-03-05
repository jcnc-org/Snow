#pragma once

#include "snow/passes/pass_contract.h"
#include "snow/sir/sir.h"

namespace snow::passes::detail {

    const snow::passes::PassContract &ConstantFoldContract();
    const snow::passes::PassContract &CfgSimplifyContract();
    const snow::passes::PassContract &CopyPropagationContract();
    const snow::passes::PassContract &DeadCodeEliminationContract();

    bool RunConstantFold(snow::sir::Function &function);
    bool RunCfgSimplify(snow::sir::Function &function);
    bool RunCopyPropagation(snow::sir::Function &function);
    bool RunDeadCodeElimination(snow::sir::Function &function);

} // namespace snow::passes::detail
