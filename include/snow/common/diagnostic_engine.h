#pragma once

#include <vector>

#include "snow/common/diagnostic.h"

namespace snow::common {

    class DiagnosticEngine {
    public:
        void Add(const Diagnostic &diagnostic);
        void Append(const DiagnosticEngine &other);
        void Error(std::string code, std::string message, std::string file, SourceRange range,
                   std::optional<std::string> suggestion = std::nullopt);
        void Warning(std::string code, std::string message, std::string file, SourceRange range,
                     std::optional<std::string> suggestion = std::nullopt);

        [[nodiscard]] bool HasErrors() const;
        [[nodiscard]] const std::vector<Diagnostic> &Diagnostics() const;

    private:
        std::vector<Diagnostic> diagnostics_;
    };

} // namespace snow::common
