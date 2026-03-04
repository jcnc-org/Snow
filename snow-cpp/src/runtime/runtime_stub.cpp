#include "snow/runtime/runtime_api.h"

extern "C" int snow_runtime_start(int (*user_main)()) {
  // Stub runtime start for v1.0 bootstrap. This preserves the entry ABI shape.
  if (user_main == nullptr) {
    return 1;
  }
  return user_main();
}
