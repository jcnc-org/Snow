// Module: Multi-module project build orchestration.
// Invariant: module graph traversal and compile scheduling are deterministic across runs.

#include "snow/driver/driver.h"

#include <algorithm>
#include <filesystem>
#include <fstream>
#include <functional>
#include <optional>
#include <set>
#include <sstream>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <utility>
#include <vector>

#include "snow/common/mangling.h"
#include "snow/common/manifest.h"
#include "snow/common/source_file.h"
#include "snow/common/target.h"
#include "snow/frontend/lexer.h"
#include "snow/frontend/parser.h"

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

        std::string ReplaceAll(std::string text, const char a, const char b) {
            for (char &c: text) {
                if (c == a) {
                    c = b;
                }
            }
            return text;
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

        struct ProjectBuildConfig {
            std::filesystem::path main_file;
            std::string manifest_target;
        };

        std::string JoinSegments(const std::vector<std::string> &segments) {
            std::ostringstream oss;
            for (std::size_t i = 0; i < segments.size(); ++i) {
                if (i > 0) {
                    oss << "/";
                }
                oss << segments[i];
            }
            return oss.str();
        }

        std::filesystem::path ResolveMainFile(const std::filesystem::path &project_root) {
            return project_root / "src" / "main.snow";
        }

        ProjectBuildConfig ResolveProjectBuildConfig(const std::filesystem::path &project_root,
                                                     common::DiagnosticEngine &diagnostics) {
            ProjectBuildConfig config;
            config.main_file = ResolveMainFile(project_root);

            const auto manifest = project_root / "snow.toml";
            if (!std::filesystem::exists(manifest)) {
                return config;
            }

            common::SnowManifest parsed;
            std::string error;
            if (!common::ParseSnowToml(manifest.string(), parsed, error)) {
                diagnostics.Error("E_MANIFEST_PARSE", "Cannot parse snow.toml", manifest.string(), FallbackRange(),
                                  error);
                return config;
            }

            if (!parsed.main.empty()) {
                config.main_file = project_root / parsed.main;
            }
            if (!parsed.target.empty()) {
                config.manifest_target = parsed.target;
            }

            return config;
        }

        std::vector<std::filesystem::path> ResolveImportFiles(const frontend::ImportDecl &import,
                                                              const std::filesystem::path &src_root,
                                                              common::DiagnosticEngine &diagnostics,
                                                              const std::string &diag_file,
                                                              const snow::common::SourceRange &import_range) {
            std::vector<std::filesystem::path> files;

            const auto joined = JoinSegments(import.path_segments);
            if (joined.empty()) {
                return files;
            }

            if (import.is_star) {
                const auto maybe_dir = src_root / joined;
                if (std::filesystem::exists(maybe_dir) && std::filesystem::is_directory(maybe_dir)) {
                    for (const auto &entry: std::filesystem::directory_iterator(maybe_dir)) {
                        if (entry.is_regular_file() && entry.path().extension() == ".snow") {
                            files.push_back(entry.path());
                        }
                    }
                    if (files.empty()) {
                        diagnostics.Warning("W_IMPORT_STAR_EMPTY", "star import directory has no .snow modules",
                                            diag_file, import_range.line == 0 ? FallbackRange() : import_range);
                    }
                    std::sort(files.begin(), files.end());
                    return files;
                }
            }

            const auto file = src_root / (joined + ".snow");
            if (!std::filesystem::exists(file)) {
                diagnostics.Error("E_MODULE_NOT_FOUND", "Module import not found: " + joined, diag_file,
                                  import_range.line == 0 ? FallbackRange() : import_range,
                                  "Create file " + file.string() + " or fix import path");
                return files;
            }

            files.push_back(file);
            return files;
        }

        struct ModuleNode {
            std::string module_id;
            std::filesystem::path file_path;
            frontend::AstModule ast;
            std::vector<std::string> deps;
        };

        bool BuildModuleGraph(const std::filesystem::path &main_file, const std::filesystem::path &src_root,
                              std::unordered_map<std::string, ModuleNode> &nodes,
                              common::DiagnosticEngine &diagnostics) {
            std::vector<std::filesystem::path> pending;
            std::unordered_set<std::string> seen_files;

            pending.push_back(main_file);

            frontend::Lexer lexer;
            frontend::Parser parser;

            while (!pending.empty()) {
                const auto current = pending.back();
                pending.pop_back();

                const auto abs = std::filesystem::weakly_canonical(current);
                const std::string abs_key = abs.generic_string();
                if (seen_files.contains(abs_key)) {
                    continue;
                }
                seen_files.insert(abs_key);

                const auto source_text = ReadFile(abs.string());
                if (!source_text.has_value()) {
                    diagnostics.Error("E_DRIVER_INPUT", "Cannot read input file", abs.string(), FallbackRange());
                    continue;
                }

                const std::string module_id = GuessModulePathFromFile(abs);
                const common::SourceFile source{abs.string(), source_text.value()};
                const auto tokens = lexer.Tokenize(source, diagnostics);
                auto ast = parser.Parse(module_id, tokens, diagnostics, abs.string());

                ModuleNode node;
                node.module_id = module_id;
                node.file_path = abs;
                node.ast = ast;

                for (const auto &import: ast.imports) {
                    auto import_files = ResolveImportFiles(import, src_root, diagnostics, abs.string(), import.range);
                    for (const auto &import_file: import_files) {
                        const auto import_abs = std::filesystem::weakly_canonical(import_file);
                        const auto dep_id = GuessModulePathFromFile(import_abs);
                        node.deps.push_back(dep_id);
                        pending.push_back(import_abs);
                    }
                }

                std::sort(node.deps.begin(), node.deps.end());
                node.deps.erase(std::unique(node.deps.begin(), node.deps.end()), node.deps.end());

                nodes[node.module_id] = std::move(node);
            }

            return !diagnostics.HasErrors();
        }

        bool TopologicalOrder(const std::unordered_map<std::string, ModuleNode> &nodes, std::vector<std::string> &order,
                              common::DiagnosticEngine &diagnostics) {
            std::unordered_map<std::string, int> state;
            std::vector<std::string> stack;

            std::vector<std::string> all_ids;
            all_ids.reserve(nodes.size());
            for (const auto &kv: nodes) {
                all_ids.push_back(kv.first);
                state[kv.first] = 0;
            }
            std::sort(all_ids.begin(), all_ids.end());

            std::function<bool(const std::string &)> dfs = [&](const std::string &id) -> bool {
                const auto node_it = nodes.find(id);
                const std::string diag_file = node_it == nodes.end() ? id : node_it->second.file_path.string();
                const int current = state[id];
                if (current == 2) {
                    return true;
                }
                if (current == 1) {
                    std::ostringstream cycle;
                    for (const auto &s: stack) {
                        cycle << s << " -> ";
                    }
                    cycle << id;
                    diagnostics.Error("E_MODULE_CYCLE", "Module dependency cycle detected", diag_file, FallbackRange(),
                                      cycle.str());
                    return false;
                }

                state[id] = 1;
                stack.push_back(id);

                const auto it = nodes.find(id);
                if (it != nodes.end()) {
                    for (const auto &dep: it->second.deps) {
                        if (!nodes.contains(dep)) {
                            diagnostics.Error("E_MODULE_MISSING", "Dependency module not loaded: " + dep, diag_file,
                                              FallbackRange());
                            continue;
                        }
                        if (!dfs(dep)) {
                            return false;
                        }
                    }
                }

                stack.pop_back();
                state[id] = 2;
                order.push_back(id);
                return true;
            };

            for (const auto &id: all_ids) {
                if (state[id] == 0 && !dfs(id)) {
                    return false;
                }
            }

            return !diagnostics.HasErrors();
        }

    } // namespace

    BuildResult Driver::BuildProject(const BuildRequest &request) const {
        BuildResult result;

        const std::filesystem::path root = request.project_root.empty() ? std::filesystem::current_path()
                                                                        : std::filesystem::path(request.project_root);
        const std::filesystem::path src_root = root / "src";
        const ProjectBuildConfig project_config = ResolveProjectBuildConfig(root, result.diagnostics);
        const std::filesystem::path main_file = project_config.main_file;
        const std::string requested_target =
                request.target_triple.empty() ? project_config.manifest_target : request.target_triple;
        const std::string build_target = requested_target.empty() ? snow::common::DetectHostTriple() : requested_target;
        if (!ValidateTargetTriple(build_target, main_file.string(), result.diagnostics)) {
            result.success = false;
            return result;
        }

        if (!std::filesystem::exists(main_file)) {
            result.diagnostics.Error("E_BUILD_MAIN_NOT_FOUND", "Main source file not found", main_file.string(),
                                     FallbackRange(), "Create src/main.snow or set main in snow.toml");
            return result;
        }
        const std::filesystem::path main_file_abs = std::filesystem::weakly_canonical(main_file);

        std::unordered_map<std::string, ModuleNode> nodes;
        (void) BuildModuleGraph(main_file_abs, src_root, nodes, result.diagnostics);

        std::vector<std::string> topo;
        (void) TopologicalOrder(nodes, topo, result.diagnostics);
        result.module_order = topo;
        std::vector<std::string> linkable_objects;
        std::vector<snow::common::FunctionSignature> available_functions;

        for (const auto &module_id: topo) {
            const auto node_it = nodes.find(module_id);
            if (node_it == nodes.end()) {
                continue;
            }
            for (const auto &function: node_it->second.ast.functions) {
                if (function.visibility != snow::frontend::Visibility::Public) {
                    continue;
                }
                if (function.name.empty()) {
                    continue;
                }
                std::vector<std::string> param_types;
                param_types.reserve(function.params.size());
                for (const auto &param: function.params) {
                    param_types.push_back(param.type);
                }
                const std::string return_type = function.return_type.empty() ? "i32" : function.return_type;
                available_functions.push_back(snow::common::FunctionSignature{
                        .module_path = module_id,
                        .source_name = function.name,
                        .param_types = param_types,
                        .return_type = return_type,
                        .mangled_name =
                                snow::common::MangleSymbol(module_id, function.name, param_types, return_type, false),
                });
            }
        }

        for (const auto &module_id: topo) {
            const auto it = nodes.find(module_id);
            if (it == nodes.end()) {
                continue;
            }
            const bool is_main_module = std::filesystem::weakly_canonical(it->second.file_path) == main_file_abs;

            CompileRequest module_request;
            module_request.input_path = it->second.file_path.string();
            module_request.target_triple = build_target;
            module_request.opt_level = request.opt_level;
            module_request.output_kind = OutputKind::Object;
            module_request.write_artifact = true;
            module_request.available_functions = available_functions;

            if (is_main_module) {
                module_request.emit = request.emit;
                module_request.output_kind = request.output_kind;
                if (request.output_kind != OutputKind::Object) {
                    module_request.link_inputs = linkable_objects;
                }
                if (!request.output_path.empty()) {
                    module_request.output_path = request.output_path;
                } else if (!request.project_root.empty()) {
                    const std::filesystem::path root_path = std::filesystem::path(request.project_root);
                    const std::filesystem::path out_dir = root_path / "snow-build";
                    module_request.output_path =
                            (out_dir / ("main" + ExtensionForOutputKind(request.output_kind))).string();
                }
            } else if (!request.project_root.empty()) {
                const std::filesystem::path root_path = std::filesystem::path(request.project_root);
                const std::string module_name = ReplaceAll(module_id, '.', '_');
                module_request.output_path =
                        (root_path / "snow-build" / (module_name + ExtensionForOutputKind(OutputKind::Object)))
                                .string();
            }

            auto compile_result = Compile(module_request);
            result.diagnostics.Append(compile_result.diagnostics);
            if (!is_main_module && module_request.output_kind == OutputKind::Object && compile_result.success &&
                !compile_result.artifact_path.empty()) {
                linkable_objects.push_back(compile_result.artifact_path);
            }
            result.module_compiles.push_back(std::move(compile_result));
        }

        std::ostringstream summary;
        summary << "build modules: " << result.module_order.size() << "\n";
        for (std::size_t i = 0; i < result.module_order.size(); ++i) {
            summary << "  - " << result.module_order[i];
            if (i < result.module_compiles.size() && !result.module_compiles[i].artifact_path.empty()) {
                summary << " -> " << result.module_compiles[i].artifact_path;
            }
            summary << "\n";
        }
        result.summary = summary.str();

        result.success = !result.diagnostics.HasErrors();
        return result;
    }

} // namespace snow::driver
