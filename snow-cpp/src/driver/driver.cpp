#include "snow/driver/driver.h"

#include <algorithm>
#include <cctype>
#include <cstdlib>
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

#if defined(_WIN32)
#include <process.h>
#endif

#include "snow/codegen/lowering.h"
#include "snow/common/manifest.h"
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

std::optional<std::string> ReadFile(const std::string& path) {
  std::ifstream in(path, std::ios::in | std::ios::binary);
  if (!in) {
    return std::nullopt;
  }
  std::ostringstream oss;
  oss << in.rdbuf();
  return oss.str();
}

std::string DumpTokens(const frontend::TokenStream& tokens) {
  std::ostringstream oss;
  for (const auto& token : tokens) {
    oss << frontend::ToString(token.type) << "('" << token.lexeme << "') @" << token.range.line << ":"
        << token.range.column << "\n";
  }
  return oss.str();
}

std::string ReplaceAll(std::string text, const char a, const char b) {
  for (char& c : text) {
    if (c == a) {
      c = b;
    }
  }
  return text;
}

std::string SanitizeFileStem(std::string text) {
  for (char& ch : text) {
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

struct ProjectBuildConfig {
  std::filesystem::path main_file;
  std::string manifest_target;
};

std::string ToString(const OutputKind output_kind) {
  switch (output_kind) {
    case OutputKind::Object:
      return "object";
    case OutputKind::Library:
      return "library";
    case OutputKind::Executable:
      return "executable";
  }
  return "object";
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

std::string GuessModulePathFromFile(const std::filesystem::path& file_path) {
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

std::filesystem::path DefaultArtifactPath(const CompileRequest& request, const std::string& module_path) {
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

std::string QuoteShellArg(const std::string& value) {
  return "\"" + value + "\"";
}

std::string QuoteShellArg(const std::filesystem::path& value) {
  return QuoteShellArg(value.string());
}

std::optional<std::string> ReadEnvValue(const char* env_name) {
#if defined(_WIN32)
  char* buffer = nullptr;
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
  const char* env_value = std::getenv(env_name);
  if (env_value == nullptr || env_value[0] == '\0') {
    return std::nullopt;
  }
  return std::string(env_value);
#endif
}

std::string ResolveToolPath(const char* env_name, const std::string& fallback_name) {
  if (const auto env_value = ReadEnvValue(env_name); env_value.has_value()) {
    return env_value.value();
  }
  return fallback_name;
}

bool WriteTextFile(const std::filesystem::path& path, const std::string& content) {
  std::ofstream out(path, std::ios::out | std::ios::binary);
  if (!out) {
    return false;
  }
  out << content;
  return true;
}

int RunSystemCommand(const std::string& command) {
  return std::system(command.c_str());
}

int RunProcess(const std::string& program, const std::vector<std::string>& args) {
#if defined(_WIN32)
  std::vector<const char*> argv;
  argv.reserve(args.size() + 2);
  argv.push_back(program.c_str());
  for (const auto& arg : args) {
    argv.push_back(arg.c_str());
  }
  argv.push_back(nullptr);
  return _spawnvp(_P_WAIT, program.c_str(), argv.data());
#else
  std::ostringstream command;
  command << QuoteShellArg(program);
  for (const auto& arg : args) {
    command << " " << QuoteShellArg(arg);
  }
  return RunSystemCommand(command.str());
#endif
}

bool TryEmitNativeArtifact(const std::filesystem::path& output_path, const OutputKind output_kind,
                           const std::string& target_triple, const std::vector<std::string>& link_inputs,
                           const std::string& llvm_ir) {
  if (target_triple != common::DetectHostTriple()) {
    return false;
  }

  const std::string clang = ResolveToolPath("SNOW_CLANG", "clang");
  const std::string llvm_ar = ResolveToolPath("SNOW_LLVM_AR", "llvm-ar");

  const std::filesystem::path ir_path = output_path.string() + ".ll";
  if (!WriteTextFile(ir_path, llvm_ir)) {
    return false;
  }

  auto cleanup = [&](const std::optional<std::filesystem::path>& extra = std::nullopt) {
    std::error_code ignore_ec;
    std::filesystem::remove(ir_path, ignore_ec);
    if (extra.has_value()) {
      std::filesystem::remove(extra.value(), ignore_ec);
    }
  };

  auto compile_ir = [&](const std::filesystem::path& destination, const bool object_only) {
    std::vector<std::string> args = {"-Wno-override-module", "-x", "ir"};
    if (object_only) {
      args.push_back("-c");
    }
    args.push_back(ir_path.string());
    args.push_back("-o");
    args.push_back(destination.string());
    return RunProcess(clang, args) == 0;
  };

  switch (output_kind) {
    case OutputKind::Object: {
      const bool ok = compile_ir(output_path, true);
      cleanup();
      return ok;
    }

    case OutputKind::Executable: {
      if (link_inputs.empty()) {
        const bool ok = compile_ir(output_path, false);
        cleanup();
        return ok;
      }

#if defined(_WIN32)
      const std::filesystem::path temp_obj = output_path.string() + ".tmp.obj";
#else
      const std::filesystem::path temp_obj = output_path.string() + ".tmp.o";
#endif
      if (!compile_ir(temp_obj, true)) {
        cleanup(temp_obj);
        return false;
      }

      std::vector<std::string> link_args;
      link_args.reserve(link_inputs.size() + 3);
      link_args.push_back(temp_obj.string());
      for (const auto& input : link_inputs) {
        link_args.push_back(input);
      }
      link_args.push_back("-o");
      link_args.push_back(output_path.string());

      const bool ok = RunProcess(clang, link_args) == 0;
      cleanup(temp_obj);
      return ok;
    }

    case OutputKind::Library: {
#if defined(_WIN32)
      const std::filesystem::path temp_obj = output_path.string() + ".tmp.obj";
#else
      const std::filesystem::path temp_obj = output_path.string() + ".tmp.o";
#endif
      if (!compile_ir(temp_obj, true)) {
        cleanup(temp_obj);
        return false;
      }
      std::vector<std::string> ar_args;
      ar_args.reserve(link_inputs.size() + 3);
      ar_args.push_back("rcs");
      ar_args.push_back(output_path.string());
      ar_args.push_back(temp_obj.string());
      for (const auto& input : link_inputs) {
        ar_args.push_back(input);
      }
      const bool ok = RunProcess(llvm_ar, ar_args) == 0;
      cleanup(temp_obj);
      return ok;
    }
  }

  cleanup();
  return false;
}

bool WriteBootstrapArtifact(const std::filesystem::path& output_path, const OutputKind output_kind,
                            const std::string& target_triple, const std::string& module_path,
                            const std::string& llvm_ir, common::DiagnosticEngine& diagnostics) {
  std::ofstream out(output_path, std::ios::out | std::ios::binary);
  if (!out) {
    diagnostics.Error("E_DRIVER_OUTFILE", "Cannot write output artifact", output_path.string(), {0, 0, 0, 0});
    return false;
  }

  out << "# snow artifact (bootstrap)\\n";
  out << "kind=" << ToString(output_kind) << "\\n";
  out << "target=" << target_triple << "\\n";
  out << "module=" << module_path << "\\n";
  out << "--- llvm ---\\n";
  out << llvm_ir;
  return true;
}

bool WriteArtifact(const std::filesystem::path& output_path, const OutputKind output_kind, const std::string& target_triple,
                   const std::string& module_path, const std::vector<std::string>& link_inputs,
                   const std::string& llvm_ir, common::DiagnosticEngine& diagnostics) {
  std::error_code ec;
  std::filesystem::create_directories(output_path.parent_path(), ec);
  if (ec) {
    diagnostics.Error("E_DRIVER_OUTDIR", "Cannot create output directory", output_path.string(), {0, 0, 0, 0},
                      ec.message());
    return false;
  }

  if (TryEmitNativeArtifact(output_path, output_kind, target_triple, link_inputs, llvm_ir)) {
    return true;
  }
  return WriteBootstrapArtifact(output_path, output_kind, target_triple, module_path, llvm_ir, diagnostics);
}

std::string JoinSegments(const std::vector<std::string>& segments) {
  std::ostringstream oss;
  for (std::size_t i = 0; i < segments.size(); ++i) {
    if (i > 0) {
      oss << "/";
    }
    oss << segments[i];
  }
  return oss.str();
}

std::filesystem::path ResolveMainFile(const std::filesystem::path& project_root) {
  return project_root / "src" / "main.snow";
}

ProjectBuildConfig ResolveProjectBuildConfig(const std::filesystem::path& project_root, common::DiagnosticEngine& diagnostics) {
  ProjectBuildConfig config;
  config.main_file = ResolveMainFile(project_root);

  const auto manifest = project_root / "snow.toml";
  if (!std::filesystem::exists(manifest)) {
    return config;
  }

  common::SnowManifest parsed;
  std::string error;
  if (!common::ParseSnowToml(manifest.string(), parsed, error)) {
    diagnostics.Error("E_MANIFEST_PARSE", "Cannot parse snow.toml", manifest.string(), {0, 0, 0, 0}, error);
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

std::vector<std::filesystem::path> ResolveImportFiles(const frontend::ImportDecl& import, const std::filesystem::path& src_root,
                                                      common::DiagnosticEngine& diagnostics,
                                                      const std::string& module_path_for_diag) {
  std::vector<std::filesystem::path> files;

  const auto joined = JoinSegments(import.path_segments);
  if (joined.empty()) {
    return files;
  }

  if (import.is_star) {
    const auto maybe_dir = src_root / joined;
    if (std::filesystem::exists(maybe_dir) && std::filesystem::is_directory(maybe_dir)) {
      for (const auto& entry : std::filesystem::directory_iterator(maybe_dir)) {
        if (entry.is_regular_file() && entry.path().extension() == ".snow") {
          files.push_back(entry.path());
        }
      }
      if (files.empty()) {
        diagnostics.Warning("W_IMPORT_STAR_EMPTY", "star import directory has no .snow modules", module_path_for_diag,
                            {0, 0, 0, 0});
      }
      std::sort(files.begin(), files.end());
      return files;
    }
  }

  const auto file = src_root / (joined + ".snow");
  if (!std::filesystem::exists(file)) {
    diagnostics.Error("E_MODULE_NOT_FOUND", "Module import not found: " + joined, module_path_for_diag, {0, 0, 0, 0},
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

bool BuildModuleGraph(const std::filesystem::path& main_file, const std::filesystem::path& src_root,
                      std::unordered_map<std::string, ModuleNode>& nodes, common::DiagnosticEngine& diagnostics) {
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
      diagnostics.Error("E_DRIVER_INPUT", "Cannot read input file", abs.string(), {0, 0, 0, 0});
      continue;
    }

    const std::string module_id = GuessModulePathFromFile(abs);
    const common::SourceFile source{abs.string(), source_text.value()};
    const auto tokens = lexer.Tokenize(source, diagnostics);
    auto ast = parser.Parse(module_id, tokens, diagnostics);

    ModuleNode node;
    node.module_id = module_id;
    node.file_path = abs;
    node.ast = ast;

    for (const auto& import : ast.imports) {
      auto import_files = ResolveImportFiles(import, src_root, diagnostics, module_id);
      for (const auto& import_file : import_files) {
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

bool TopologicalOrder(const std::unordered_map<std::string, ModuleNode>& nodes, std::vector<std::string>& order,
                      common::DiagnosticEngine& diagnostics) {
  std::unordered_map<std::string, int> state;
  std::vector<std::string> stack;

  std::vector<std::string> all_ids;
  all_ids.reserve(nodes.size());
  for (const auto& kv : nodes) {
    all_ids.push_back(kv.first);
    state[kv.first] = 0;
  }
  std::sort(all_ids.begin(), all_ids.end());

  std::function<bool(const std::string&)> dfs = [&](const std::string& id) -> bool {
    const int current = state[id];
    if (current == 2) {
      return true;
    }
    if (current == 1) {
      std::ostringstream cycle;
      for (const auto& s : stack) {
        cycle << s << " -> ";
      }
      cycle << id;
      diagnostics.Error("E_MODULE_CYCLE", "Module dependency cycle detected", id, {0, 0, 0, 0}, cycle.str());
      return false;
    }

    state[id] = 1;
    stack.push_back(id);

    const auto it = nodes.find(id);
    if (it != nodes.end()) {
      for (const auto& dep : it->second.deps) {
        if (!nodes.contains(dep)) {
          diagnostics.Error("E_MODULE_MISSING", "Dependency module not loaded: " + dep, id, {0, 0, 0, 0});
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

  for (const auto& id : all_ids) {
    if (state[id] == 0 && !dfs(id)) {
      return false;
    }
  }

  return !diagnostics.HasErrors();
}

}  // namespace

std::string RenderDiagnostics(const snow::common::DiagnosticEngine& diagnostics) {
  std::ostringstream oss;
  for (const auto& diagnostic : diagnostics.Diagnostics()) {
    oss << snow::common::FormatDiagnostic(diagnostic);
  }
  return oss.str();
}

CompileResult Driver::Compile(const CompileRequest& request) const {
  CompileResult result;

  const auto source_text = ReadFile(request.input_path);
  if (!source_text.has_value()) {
    result.diagnostics.Error("E_DRIVER_INPUT", "Cannot read input file", request.input_path, {0, 0, 0, 0});
    return result;
  }

  const auto module_path = GuessModulePathFromFile(std::filesystem::path(request.input_path));
  const std::string target_triple = request.target_triple.empty() ? snow::common::DetectHostTriple() : request.target_triple;
  result.target_triple = target_triple;
  const snow::common::SourceFile source{request.input_path, source_text.value()};

  frontend::Lexer lexer;
  const auto tokens = lexer.Tokenize(source, result.diagnostics);
  if (request.emit.tokens) {
    result.token_dump = DumpTokens(tokens);
  }

  frontend::Parser parser;
  auto ast = parser.Parse(module_path, tokens, result.diagnostics);
  if (request.emit.ast) {
    result.ast_dump = frontend::DumpAst(ast);
  }

  sema::SemanticAnalyzer sema;
  auto sema_module = sema.Analyze(ast, result.diagnostics);
  if (request.emit.sema) {
    result.sema_dump = sema::DumpSema(sema_module);
  }

  ownership::OwnershipChecker ownership_checker;
  auto ownership_facts = ownership_checker.Check(sema_module, result.diagnostics);
  result.ownership_dump = ownership::DumpOwnership(ownership_facts);

  sir::SirBuilder builder;
  auto sir_module = builder.Build(sema_module, ownership_facts);

  sir::SirValidator validator;
#ifndef NDEBUG
  constexpr auto kValidationLevel = sir::ValidationLevel::Debug;
#else
  constexpr auto kValidationLevel = sir::ValidationLevel::Release;
#endif

  (void)validator.Validate(sir_module, kValidationLevel, result.diagnostics);

  passes::PassManager pass_manager;
  auto pass_result = pass_manager.Run(sir_module, request.opt_level, kValidationLevel, validator, result.diagnostics);

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
    const auto llvm_result = lowering.Lower(pass_result.module, target, request.opt_level);
    if (request.emit.llvm) {
      result.llvm_dump = llvm_result.llvm_ir;
    }

    if (request.write_artifact) {
      const std::filesystem::path out_path = request.output_path.empty()
                                                 ? DefaultArtifactPath(request, module_path)
                                                 : std::filesystem::path(request.output_path);
      if (WriteArtifact(out_path, request.output_kind, target_triple, module_path, request.link_inputs, llvm_result.llvm_ir,
                        result.diagnostics)) {
        result.artifact_path = out_path.string();
      }
    }
  }

  result.success = !result.diagnostics.HasErrors();
  return result;
}

BuildResult Driver::BuildProject(const BuildRequest& request) const {
  BuildResult result;

  const std::filesystem::path root = request.project_root.empty() ? std::filesystem::current_path()
                                                                  : std::filesystem::path(request.project_root);
  const std::filesystem::path src_root = root / "src";
  const ProjectBuildConfig project_config = ResolveProjectBuildConfig(root, result.diagnostics);
  const std::filesystem::path main_file = project_config.main_file;
  const std::string build_target =
      request.target_triple.empty() ? project_config.manifest_target : request.target_triple;

  if (!std::filesystem::exists(main_file)) {
    result.diagnostics.Error("E_BUILD_MAIN_NOT_FOUND", "Main source file not found", main_file.string(), {0, 0, 0, 0},
                             "Create src/main.snow or set main in snow.toml");
    return result;
  }
  const std::filesystem::path main_file_abs = std::filesystem::weakly_canonical(main_file);

  std::unordered_map<std::string, ModuleNode> nodes;
  (void)BuildModuleGraph(main_file_abs, src_root, nodes, result.diagnostics);

  std::vector<std::string> topo;
  (void)TopologicalOrder(nodes, topo, result.diagnostics);
  result.module_order = topo;
  std::vector<std::string> linkable_objects;

  for (const auto& module_id : topo) {
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
        module_request.output_path = (out_dir / ("main" + ExtensionForOutputKind(request.output_kind))).string();
      }
    } else if (!request.project_root.empty()) {
      const std::filesystem::path root_path = std::filesystem::path(request.project_root);
      const std::string module_name = ReplaceAll(module_id, '.', '_');
      module_request.output_path = (root_path / "snow-build" / (module_name + ExtensionForOutputKind(OutputKind::Object))).string();
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

}  // namespace snow::driver
