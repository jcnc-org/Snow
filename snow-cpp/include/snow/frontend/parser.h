#pragma once

#include "snow/common/diagnostic_engine.h"
#include "snow/frontend/ast.h"
#include "snow/frontend/token.h"

namespace snow::frontend {

class Parser {
 public:
  AstModule Parse(std::string module_path, const TokenStream& tokens, snow::common::DiagnosticEngine& diagnostics) const;
};

}  // namespace snow::frontend
