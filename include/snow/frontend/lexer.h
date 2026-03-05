#pragma once

#include "snow/common/diagnostic_engine.h"
#include "snow/common/source_file.h"
#include "snow/frontend/token.h"

namespace snow::frontend {

    class Lexer {
    public:
        TokenStream Tokenize(const snow::common::SourceFile &source, snow::common::DiagnosticEngine &diagnostics) const;
    };

} // namespace snow::frontend
