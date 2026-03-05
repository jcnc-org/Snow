#pragma once

#include <string>
#include <vector>

namespace snow::common {

struct FunctionSignature {
  std::string module_path;
  std::string source_name;
  std::vector<std::string> param_types;
  std::string return_type;
  std::string mangled_name;
};

}  // namespace snow::common
