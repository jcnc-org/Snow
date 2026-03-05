#include "snow/common/mangling.h"

#include <cctype>
#include <iomanip>
#include <sstream>

namespace snow::common {

    namespace {

        uint32_t Fnva32(const std::string &input) {
            uint32_t hash = 2166136261u;
            for (const unsigned char ch: input) {
                hash ^= ch;
                hash *= 16777619u;
            }
            return hash;
        }

        std::string NormalizePath(std::string_view module_path) {
            std::string out;
            out.reserve(module_path.size());
            for (const unsigned char ch: module_path) {
                if (std::isalnum(ch) || ch == '_') {
                    out.push_back(static_cast<char>(ch));
                    continue;
                }
                out.push_back('_');
            }
            return out;
        }

    } // namespace

    std::string MangleSymbol(const std::string_view module_path, const std::string_view item_name,
                             const std::vector<std::string> &param_types, const std::string_view return_type,
                             const bool extern_c) {
        if (extern_c) {
            return std::string(item_name);
        }

        std::ostringstream sig;
        sig << item_name << "(";
        for (std::size_t i = 0; i < param_types.size(); ++i) {
            if (i > 0) {
                sig << ",";
            }
            sig << param_types[i];
        }
        sig << ")->" << return_type;

        const uint32_t hash = Fnva32(sig.str());

        std::ostringstream hex;
        hex << std::hex << std::setfill('0') << std::setw(8) << hash;

        std::ostringstream out;
        out << "_snow_" << NormalizePath(module_path) << "_" << item_name << "_" << hex.str();
        return out.str();
    }

} // namespace snow::common
