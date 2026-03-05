#include "snow/runtime/runtime_api.h"

#include <cstdio>
#include <cstdlib>
#include <new>

#if defined(_WIN32)
#include <malloc.h>
#endif

extern "C" void snow_runtime_init() {}

extern "C" void snow_runtime_shutdown() {}

extern "C" void snow_runtime_drop(void *value) { (void) value; }

extern "C" void snow_runtime_drop_dispatch(const int tag, void *value) {
    (void) tag;
    snow_runtime_drop(value);
}

extern "C" [[noreturn]] void snow_panic(const char *message) {
    std::fprintf(stderr, "snow panic: %s\n", message == nullptr ? "<null>" : message);
    std::abort();
}

extern "C" void *snow_alloc(std::size_t size, std::size_t alignment) {
    if (size == 0) {
        size = 1;
    }
    if (alignment == 0) {
        alignment = alignof(std::max_align_t);
    }

#if defined(_WIN32)
    void *ptr = _aligned_malloc(size, alignment);
#else
    if (alignment < alignof(void *)) {
        alignment = alignof(void *);
    }
    const std::size_t aligned_size = ((size + alignment - 1) / alignment) * alignment;
    void *ptr = std::aligned_alloc(alignment, aligned_size);
#endif

    if (ptr == nullptr) {
        snow_panic("allocation failed");
    }
    return ptr;
}

extern "C" void snow_free(void *ptr, const std::size_t alignment) {
    (void) alignment;
    if (ptr == nullptr) {
        return;
    }
#if defined(_WIN32)
    _aligned_free(ptr);
#else
    std::free(ptr);
#endif
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
