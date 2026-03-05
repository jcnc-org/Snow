#pragma once

#include <string>

namespace snow::passes {

    struct PassContract {
        std::string name;
        std::string input_invariants;
        std::string output_invariants;
        std::string failure_modes;
    };

} // namespace snow::passes
