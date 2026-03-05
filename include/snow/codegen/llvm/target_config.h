#pragma once

#include <string>

namespace snow::codegen {

    struct TargetConfig {
        std::string triple;
        bool executable_entry_wrapper = false;
    };

} // namespace snow::codegen
