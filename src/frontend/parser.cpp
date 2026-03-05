// Module: Public parser facade.

#include "snow/frontend/parser.h"

#include <utility>

#include "parser_internal.h"

namespace snow::frontend {

    AstModule Parser::Parse(std::string module_path, const TokenStream &tokens,
                            snow::common::DiagnosticEngine &diagnostics, std::string source_path) const {
        return detail::ParseModule(std::move(module_path), tokens, diagnostics, std::move(source_path));
    }

} // namespace snow::frontend
