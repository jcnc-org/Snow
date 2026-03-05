#pragma once

#include <string>
#include <string_view>
#include <vector>

namespace snow::common {

std::string MangleSymbol(std::string_view module_path, std::string_view item_name,
                         const std::vector<std::string>& param_types, std::string_view return_type,
                         bool extern_c = false);

}  // namespace snow::common
