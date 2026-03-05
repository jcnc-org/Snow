#include <cstdint>
#include <iostream>
#include <string>

#include "snow/common/mangling.h"
#include "snow/runtime/runtime_api.h"

namespace {

    bool Fail(const std::string &test_name, const std::string &message) {
        std::cerr << "[FAIL] " << test_name << ": " << message << "\n";
        return false;
    }

    bool TestMangleSymbolSanitizesModulePath() {
        const std::string test_name = "TestMangleSymbolSanitizesModulePath";
        const std::string symbol =
                snow::common::MangleSymbol("C:.Users/LukeK-demo.app", "main", std::vector<std::string>{}, "i32", false);

        if (symbol.find(':') != std::string::npos || symbol.find('/') != std::string::npos ||
            symbol.find('\\') != std::string::npos || symbol.find('-') != std::string::npos) {
            return Fail(test_name, "mangled symbol still contains non-identifier path characters");
        }
        if (symbol.rfind("_snow_", 0) != 0) {
            return Fail(test_name, "mangled symbol must keep _snow_ prefix");
        }
        return true;
    }

    bool TestMangleSymbolExternCBypass() {
        const std::string test_name = "TestMangleSymbolExternCBypass";
        const std::string symbol =
                snow::common::MangleSymbol("pkg.core", "c_func", std::vector<std::string>{"i32"}, "i32", true);
        if (symbol != "c_func") {
            return Fail(test_name, "extern C symbol should bypass mangling");
        }
        return true;
    }

    bool TestRuntimeAbiSurface() {
        const std::string test_name = "TestRuntimeAbiSurface";

        void *ptr = snow_alloc(64, 16);
        if (ptr == nullptr) {
            return Fail(test_name, "snow_alloc returned null");
        }
        const auto addr = reinterpret_cast<std::uintptr_t>(ptr);
        if ((addr % 16) != 0) {
            return Fail(test_name, "snow_alloc alignment mismatch");
        }

        snow_runtime_drop_dispatch(0, ptr);
        snow_free(ptr, 16);

        const int rc = snow_runtime_start(nullptr);
        if (rc != 1) {
            return Fail(test_name, "snow_runtime_start(nullptr) should return 1");
        }

        return true;
    }

} // namespace

int main() {
    int failed = 0;
    failed += TestMangleSymbolSanitizesModulePath() ? 0 : 1;
    failed += TestMangleSymbolExternCBypass() ? 0 : 1;
    failed += TestRuntimeAbiSurface() ? 0 : 1;

    if (failed == 0) {
        std::cout << "[PASS] snow-common-tests\n";
        return 0;
    }

    std::cerr << "[FAIL] snow-common-tests failed=" << failed << "\n";
    return 1;
}
