#include "snow/runtime/runtime_api.h"

extern "C" void snow_runtime_init() {}

extern "C" void snow_runtime_shutdown() {}

extern "C" void snow_runtime_drop(void* value) {
  (void)value;
}

extern "C" int snow_runtime_start(int (*user_main)()) {
  snow_runtime_init();
  if (user_main == nullptr) {
    snow_runtime_shutdown();
    return 1;
  }
  const int code = user_main();
  snow_runtime_shutdown();
  return code;
}
