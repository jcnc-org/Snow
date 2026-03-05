#pragma once

#include <cstddef>

extern "C" void snow_runtime_init();
extern "C" void snow_runtime_shutdown();
extern "C" void snow_runtime_drop(void *value);
extern "C" void snow_runtime_drop_dispatch(int tag, void *value);
extern "C" int snow_runtime_start(int (*user_main)());
extern "C" void *snow_alloc(std::size_t size, std::size_t alignment);
extern "C" void snow_free(void *ptr, std::size_t alignment);
extern "C" [[noreturn]] void snow_panic(const char *message);
