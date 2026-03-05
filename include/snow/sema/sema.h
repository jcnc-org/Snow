#pragma once

#include <string>
#include <vector>

#include "snow/common/diagnostic_engine.h"
#include "snow/common/function_signature.h"
#include "snow/frontend/ast.h"

namespace snow::sema {

    struct ResolvedImport {
        std::string canonical_path;
        std::string alias;
        bool is_star = false;
        std::string unqualified_name;
    };

    struct SemaModule {
        snow::frontend::AstModule ast;
        std::vector<ResolvedImport> imports;
        std::vector<std::string> symbols;
        std::vector<snow::common::FunctionSignature> resolved_external_functions;
    };

    class SemanticAnalyzer {
    public:
        SemaModule Analyze(const snow::frontend::AstModule &ast_module, snow::common::DiagnosticEngine &diagnostics,
                           const std::vector<snow::common::FunctionSignature> &available_functions) const;
        SemaModule Analyze(const snow::frontend::AstModule &ast_module,
                           snow::common::DiagnosticEngine &diagnostics) const;
    };

    std::string DumpSema(const SemaModule &module);

} // namespace snow::sema
