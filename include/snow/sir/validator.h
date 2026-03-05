#pragma once

#include <string>
#include <vector>

#include "snow/common/diagnostic_engine.h"
#include "snow/sir/sir.h"

namespace snow::sir {

    enum class ValidationLevel {
        Debug,
        Release,
    };

    struct ValidationReport {
        bool ok = true;
        std::vector<std::string> notes;
    };

    class SirValidator {
    public:
        ValidationReport Validate(const Module &module, ValidationLevel level,
                                  snow::common::DiagnosticEngine &diagnostics) const;
    };

} // namespace snow::sir
