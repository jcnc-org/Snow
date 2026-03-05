#pragma once

#include <string>
#include <vector>

namespace snow::common {

std::string DetectHostTriple();
const std::vector<std::string>& SupportedTargetTriples();
bool IsSupportedTargetTriple(const std::string& triple);

}  // namespace snow::common
