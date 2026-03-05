#pragma once

#include <string>
#include <vector>

#include "snow/common/function_signature.h"
#include "snow/passes/pass_manager.h"

namespace snow::driver {

    enum class OutputKind {
        Object,
        Library,
        Executable,
    };

    struct EmitOptions {
        bool tokens = false;
        bool ast = false;
        bool sema = false;
        bool sir = false;
        bool cfg = false;
        bool llvm = false;
        bool timings = false;
    };

    struct CompileRequest {
        std::string input_path;
        std::string target_triple;
        snow::passes::OptLevel opt_level = snow::passes::OptLevel::O0;
        OutputKind output_kind = OutputKind::Executable;
        std::string output_path;
        std::vector<std::string> link_inputs;
        std::vector<snow::common::FunctionSignature> available_functions;
        bool write_artifact = true;
        EmitOptions emit;
    };

} // namespace snow::driver
