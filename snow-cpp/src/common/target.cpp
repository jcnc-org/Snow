#include "snow/common/target.h"

namespace snow::common {

std::string DetectHostTriple() {
#if defined(_WIN32)
#if defined(_M_X64) || defined(__x86_64__)
  return "x86_64-pc-windows-msvc";
#elif defined(_M_ARM64) || defined(__aarch64__)
  return "aarch64-pc-windows-msvc";
#else
  return "x86_64-pc-windows-msvc";
#endif
#elif defined(__APPLE__)
#if defined(__aarch64__)
  return "aarch64-apple-darwin";
#else
  return "x86_64-apple-darwin";
#endif
#elif defined(__linux__)
#if defined(__aarch64__)
  return "aarch64-unknown-linux-gnu";
#else
  return "x86_64-unknown-linux-gnu";
#endif
#else
  return "x86_64-unknown-linux-gnu";
#endif
}

}  // namespace snow::common
