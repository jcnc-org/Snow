// Module: Compile pipeline orchestration and artifact emission.
// Contract: phase ordering is fixed; each stage emits diagnostics before downstream stages execute.

#include "snow/driver/driver.h"

#include <cctype>
#include <chrono>
#include <cstdlib>
#include <filesystem>
#include <fstream>
#include <optional>
#include <sstream>
#include <string>
#include <utility>
#include <vector>

#if defined(_WIN32)
#include <process.h>
#endif

#include "snow/codegen/lowering.h"
#include "snow/common/source_file.h"
#include "snow/common/target.h"
#include "snow/frontend/lexer.h"
#include "snow/frontend/parser.h"
#include "snow/ownership/ownership.h"
#include "snow/passes/pass_manager.h"
#include "snow/sema/sema.h"
#include "snow/sir/sir_builder.h"
#include "snow/sir/validator.h"

namespace snow::driver {

    namespace {

        snow::common::SourceRange FallbackRange() { return snow::common::SourceRange{1, 1, 1, 1}; }

        std::optional<std::string> ReadFile(const std::string &path) {
            std::ifstream in(path, std::ios::in | std::ios::binary);
            if (!in) {
                return std::nullopt;
            }
            std::ostringstream oss;
            oss << in.rdbuf();
            return oss.str();
        }

        std::string DumpTokens(const frontend::TokenStream &tokens) {
            std::ostringstream oss;
            for (const auto &token: tokens) {
                oss << frontend::ToString(token.type) << "('" << token.lexeme << "') @" << token.range.line << ":"
                    << token.range.column << "\n";
            }
            return oss.str();
        }

        std::string ReplaceAll(std::string text, const char a, const char b) {
            for (char &c: text) {
                if (c == a) {
                    c = b;
                }
            }
            return text;
        }

        std::string SanitizeFileStem(std::string text) {
            for (char &ch: text) {
                const auto uch = static_cast<unsigned char>(ch);
                if (!(std::isalnum(uch) || ch == '_' || ch == '-')) {
                    ch = '_';
                }
            }
            while (!text.empty() && text.front() == '_') {
                text.erase(text.begin());
            }
            return text;
        }

        std::string ExtensionForOutputKind(const OutputKind output_kind) {
            switch (output_kind) {
                case OutputKind::Object:
                    return ".o";
                case OutputKind::Library:
                    return ".a";
                case OutputKind::Executable:
#if defined(_WIN32)
                    return ".exe";
#else
                    return "";
#endif
            }
            return ".o";
        }

        std::string GuessModulePathFromFile(const std::filesystem::path &file_path) {
            std::filesystem::path p = file_path;
            p.replace_extension();
            std::string generic = p.generic_string();

            const std::string marker = "/src/";
            const auto pos = generic.rfind(marker);
            std::string module = pos == std::string::npos ? generic : generic.substr(pos + marker.size());
            module = ReplaceAll(module, '/', '.');
            module = ReplaceAll(module, '\\', '.');

            while (!module.empty() && module.front() == '.') {
                module.erase(module.begin());
            }
            while (!module.empty() && module.back() == '.') {
                module.pop_back();
            }
            return module;
        }

        std::filesystem::path DefaultArtifactPath(const CompileRequest &request, const std::string &module_path) {
            const std::filesystem::path input_path = request.input_path;
            const std::filesystem::path out_dir = std::filesystem::current_path() / "snow-build";
            std::string name = module_path;
            name = ReplaceAll(name, '.', '_');
            name = SanitizeFileStem(name);
            if (name.empty()) {
                name = input_path.stem().string();
            }
            return out_dir / (name + ExtensionForOutputKind(request.output_kind));
        }

        std::string QuoteShellArg(const std::string &value) { return "\"" + value + "\""; }

        std::optional<std::string> ReadEnvValue(const char *env_name) {
#if defined(_WIN32)
            char *buffer = nullptr;
            std::size_t size = 0;
            if (_dupenv_s(&buffer, &size, env_name) != 0 || buffer == nullptr) {
                return std::nullopt;
            }
            std::string value(buffer);
            std::free(buffer);
            if (value.empty()) {
                return std::nullopt;
            }
            return value;
#else
            const char *env_value = std::getenv(env_name);
            if (env_value == nullptr || env_value[0] == '\0') {
                return std::nullopt;
            }
            return std::string(env_value);
#endif
        }

        std::string ResolveToolPath(const char *env_name, const std::string &fallback_name) {
            if (const auto env_value = ReadEnvValue(env_name); env_value.has_value()) {
                return env_value.value();
            }
            return fallback_name;
        }

        std::string JoinSupportedTargets() {
            std::ostringstream oss;
            const auto &supported = snow::common::SupportedTargetTriples();
            for (std::size_t i = 0; i < supported.size(); ++i) {
                if (i > 0) {
                    oss << ", ";
                }
                oss << supported[i];
            }
            return oss.str();
        }

        bool ValidateTargetTriple(const std::string &target_triple, const std::string &diag_file,
                                  common::DiagnosticEngine &diagnostics) {
            if (snow::common::IsSupportedTargetTriple(target_triple)) {
                return true;
            }
            diagnostics.Error("E_TARGET_UNSUPPORTED", "Unsupported target triple: " + target_triple, diag_file,
                              FallbackRange(), "Supported targets: " + JoinSupportedTargets());
            return false;
        }

        int RunSystemCommand(const std::string &command) { return std::system(command.c_str()); }

        int RunProcess(const std::string &program, const std::vector<std::string> &args) {
#if defined(_WIN32)
            std::vector<const char *> argv;
            argv.reserve(args.size() + 2);
            argv.push_back(program.c_str());
            for (const auto &arg: args) {
                argv.push_back(arg.c_str());
            }
            argv.push_back(nullptr);
            return _spawnvp(_P_WAIT, program.c_str(), argv.data());
#else
            std::ostringstream command;
            command << QuoteShellArg(program);
            for (const auto &arg: args) {
                command << " " << QuoteShellArg(arg);
            }
            return RunSystemCommand(command.str());
#endif
        }

        bool EnsureOutputDirectory(const std::filesystem::path &output_path, common::DiagnosticEngine &diagnostics) {
            const auto parent = output_path.parent_path();
            if (parent.empty()) {
                return true;
            }
            std::error_code ec;
            std::filesystem::create_directories(parent, ec);
            if (ec) {
                diagnostics.Error("E_DRIVER_OUTDIR", "Cannot create output directory", output_path.string(),
                                  {1, 1, 1, 1}, ec.message());
                return false;
            }
            return true;
        }

        bool LinkExecutable(const std::filesystem::path &output_path, const std::string &target_triple,
                            const std::vector<std::string> &objects, common::DiagnosticEngine &diagnostics) {
            const std::string clang = ResolveToolPath("SNOW_CLANG", "clang");
            std::vector<std::string> args;
            args.reserve(objects.size() + 4);
            args.push_back("--target=" + target_triple);
            for (const auto &object: objects) {
                args.push_back(object);
            }
            args.push_back("-o");
            args.push_back(output_path.string());
            if (RunProcess(clang, args) == 0) {
                return true;
            }
            diagnostics.Error("E_BACKEND_LINK_FAIL", "Failed to link executable artifact", output_path.string(),
                              {1, 1, 1, 1}, "Ensure clang/lld is installed and visible in PATH");
            return false;
        }

        bool CreateStaticLibrary(const std::filesystem::path &output_path, const std::vector<std::string> &objects,
                                 common::DiagnosticEngine &diagnostics) {
            const std::string llvm_ar = ResolveToolPath("SNOW_LLVM_AR", "llvm-ar");
            std::vector<std::string> args;
            args.reserve(objects.size() + 2);
            args.push_back("rcs");
            args.push_back(output_path.string());
            for (const auto &object: objects) {
                args.push_back(object);
            }
            if (RunProcess(llvm_ar, args) == 0) {
                return true;
            }
            diagnostics.Error("E_BACKEND_LINK_FAIL", "Failed to archive static library artifact", output_path.string(),
                              {1, 1, 1, 1}, "Ensure llvm-ar is installed and visible in PATH");
            return false;
        }

        bool WriteArtifact(const std::filesystem::path &output_path, const OutputKind output_kind,
                           const std::string &target_triple, const std::vector<std::string> &link_inputs,
                           const snow::sir::Module &module, const snow::passes::OptLevel opt_level,
                           common::DiagnosticEngine &diagnostics) {
            if (!EnsureOutputDirectory(output_path, diagnostics)) {
                return false;
            }

            codegen::LlvmLowering lowering;
            codegen::TargetConfig target{
                    .triple = target_triple,
                    .executable_entry_wrapper = output_kind == OutputKind::Executable,
            };

            if (output_kind == OutputKind::Object) {
                const auto emit = lowering.EmitObject(module, target, opt_level, output_path.string());
                if (!emit.success) {
                    diagnostics.Error(emit.error_code.empty() ? "E_BACKEND_OBJECT_EMIT" : emit.error_code,
                                      "Failed to emit object artifact", output_path.string(), FallbackRange(),
                                      emit.error_message);
                    return false;
                }
                return true;
            }

#if defined(_WIN32)
            const std::filesystem::path module_object = output_path.string() + ".tmp.obj";
#else
            const std::filesystem::path module_object = output_path.string() + ".tmp.o";
#endif

            const auto emit = lowering.EmitObject(module, target, opt_level, module_object.string());
            if (!emit.success) {
                diagnostics.Error(emit.error_code.empty() ? "E_BACKEND_OBJECT_EMIT" : emit.error_code,
                                  "Failed to emit object artifact", output_path.string(), FallbackRange(),
                                  emit.error_message);
                std::error_code ignore_ec;
                std::filesystem::remove(module_object, ignore_ec);
                return false;
            }

            std::vector<std::string> objects;
            objects.reserve(link_inputs.size() + 1);
            objects.push_back(module_object.string());
            for (const auto &input: link_inputs) {
                objects.push_back(input);
            }

            bool ok = false;
            if (output_kind == OutputKind::Executable) {
                ok = LinkExecutable(output_path, target_triple, objects, diagnostics);
            } else {
                ok = CreateStaticLibrary(output_path, objects, diagnostics);
            }

            std::error_code ignore_ec;
            std::filesystem::remove(module_object, ignore_ec);
            return ok;
        }

        std::string TimingsToString(const std::vector<snow::passes::PassResult::PassTiming> &timings) {
            std::ostringstream oss;
            oss << "timings (passes)\n";
            for (const auto &timing: timings) {
                oss << "  " << timing.pass_name << ": " << timing.wall_ms << "ms\n";
            }
            return oss.str();
        }

        template<typename Fn>
        double MeasureMs(Fn &&fn) {
            const auto start = std::chrono::steady_clock::now();
            std::forward<Fn>(fn)();
            const auto end = std::chrono::steady_clock::now();
            return std::chrono::duration<double, std::milli>(end - start).count();
        }

    } // namespace

    std::string RenderDiagnostics(const snow::common::DiagnosticEngine &diagnostics) {
        std::ostringstream oss;
        for (const auto &diagnostic: diagnostics.Diagnostics()) {
            oss << snow::common::FormatDiagnostic(diagnostic);
        }
        return oss.str();
    }

    CompileResult Driver::Compile(const CompileRequest &request) const {
        CompileResult result;
        std::vector<std::pair<std::string, double>> phase_timings;

        const auto source_text = ReadFile(request.input_path);
        if (!source_text.has_value()) {
            result.diagnostics.Error("E_DRIVER_INPUT", "Cannot read input file", request.input_path, FallbackRange());
            return result;
        }

        const auto module_path = GuessModulePathFromFile(std::filesystem::path(request.input_path));
        const std::string target_triple =
                request.target_triple.empty() ? snow::common::DetectHostTriple() : request.target_triple;
        result.target_triple = target_triple;
        if (!ValidateTargetTriple(target_triple, request.input_path, result.diagnostics)) {
            result.success = false;
            return result;
        }
        const snow::common::SourceFile source{request.input_path, source_text.value()};

        frontend::Lexer lexer;
        frontend::TokenStream tokens;
        phase_timings.push_back({"lex", MeasureMs([&]() { tokens = lexer.Tokenize(source, result.diagnostics); })});
        if (request.emit.tokens) {
            result.token_dump = DumpTokens(tokens);
        }

        frontend::Parser parser;
        frontend::AstModule ast;
        phase_timings.push_back({"parse", MeasureMs([&]() {
                                     ast = parser.Parse(module_path, tokens, result.diagnostics, source.path);
                                 })});
        if (request.emit.ast) {
            result.ast_dump = frontend::DumpAst(ast);
        }

        sema::SemanticAnalyzer sema;
        sema::SemaModule sema_module;
        phase_timings.push_back({"sema", MeasureMs([&]() {
                                     sema_module = sema.Analyze(ast, result.diagnostics, request.available_functions);
                                 })});
        if (request.emit.sema) {
            result.sema_dump = sema::DumpSema(sema_module);
        }

        ownership::OwnershipChecker ownership_checker;
        ownership::OwnershipFacts ownership_facts;
        phase_timings.push_back({"ownership", MeasureMs([&]() {
                                     ownership_facts = ownership_checker.Check(sema_module, result.diagnostics);
                                 })});
        result.ownership_dump = ownership::DumpOwnership(ownership_facts);

        sir::SirBuilder builder;
        sir::Module sir_module;
        phase_timings.push_back(
                {"sir-build", MeasureMs([&]() { sir_module = builder.Build(sema_module, ownership_facts); })});

        sir::SirValidator validator;
#ifndef NDEBUG
        constexpr auto kValidationLevel = sir::ValidationLevel::Debug;
#else
        constexpr auto kValidationLevel = sir::ValidationLevel::Release;
#endif

        phase_timings.push_back({"sir-validate-pre-pass", MeasureMs([&]() {
                                     (void) validator.Validate(sir_module, kValidationLevel, result.diagnostics);
                                 })});

        passes::PassManager pass_manager;
        passes::PassResult pass_result;
        phase_timings.push_back({"passes", MeasureMs([&]() {
                                     pass_result = pass_manager.Run(sir_module, request.opt_level, kValidationLevel,
                                                                    validator, result.diagnostics);
                                 })});

        if (request.emit.sir) {
            result.sir_dump = sir::DumpSir(pass_result.module);
        }
        if (request.emit.cfg) {
            result.cfg_dump = sir::DumpCfg(pass_result.module);
        }

        if (!result.diagnostics.HasErrors()) {
            codegen::LlvmLowering lowering;
            codegen::TargetConfig target{
                    .triple = target_triple,
                    .executable_entry_wrapper = request.output_kind == OutputKind::Executable,
            };
            codegen::LoweringResult llvm_result;
            phase_timings.push_back({"llvm-lower", MeasureMs([&]() {
                                         llvm_result = lowering.Lower(pass_result.module, target, request.opt_level);
                                     })});
            if (!llvm_result.native_ready) {
                const std::string code =
                        llvm_result.error_code.empty() ? "E_BACKEND_LLVM_REQUIRED" : llvm_result.error_code;
                const std::string message = llvm_result.error_message.empty()
                                                    ? "LLVM backend is not ready for this module"
                                                    : llvm_result.error_message;
                result.diagnostics.Error(code, message, request.input_path, FallbackRange(),
                                         "Enable LLVM backend support and verify toolchain installation");
            }
            if (request.emit.llvm) {
                result.llvm_dump = llvm_result.llvm_ir;
            }

            if (request.write_artifact && !result.diagnostics.HasErrors()) {
                const std::filesystem::path out_path = request.output_path.empty()
                                                               ? DefaultArtifactPath(request, module_path)
                                                               : std::filesystem::path(request.output_path);
                const bool wrote_artifact = [&]() {
                    bool ok = false;
                    phase_timings.push_back({"artifact-write", MeasureMs([&]() {
                                                 ok = WriteArtifact(out_path, request.output_kind, target_triple,
                                                                    request.link_inputs, pass_result.module,
                                                                    request.opt_level, result.diagnostics);
                                             })});
                    return ok;
                }();
                if (wrote_artifact) {
                    result.artifact_path = out_path.string();
                }
            }
        }

        if (request.emit.timings) {
            std::ostringstream timing_oss;
            timing_oss << "timings (phases)\n";
            for (const auto &phase: phase_timings) {
                timing_oss << "  " << phase.first << ": " << phase.second << "ms\n";
            }
            timing_oss << TimingsToString(pass_result.timings);
            result.timing_dump = timing_oss.str();
        }

        result.success = !result.diagnostics.HasErrors();
        return result;
    }

} // namespace snow::driver
