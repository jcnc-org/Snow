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

#include "snow/codegen/lowering.h"
#include "snow/common/source_file.h"
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

std::string Trim(const std::string& in) {
  const auto first = in.find_first_not_of(" \t\r\n");
  if (first == std::string::npos) {
    return "";
  }
  const auto last = in.find_last_not_of(" \t\r\n");
  return in.substr(first, last - first + 1);
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
  const auto manifest = project_root / "snow.toml";
  if (!std::filesystem::exists(manifest)) {
    return project_root / "src" / "main.snow";
  }

  std::ifstream in(manifest);
  if (!in) {
    return project_root / "src" / "main.snow";
  }

  std::string line;
  while (std::getline(in, line)) {
    const std::string t = Trim(line);
    if (t.rfind("main", 0) != 0) {
      continue;
    }
    const auto pos = t.find('=');
    if (pos == std::string::npos) {
      continue;
    }
    std::string value = Trim(t.substr(pos + 1));
    if (!value.empty() && value.front() == '"' && value.back() == '"' && value.size() >= 2) {
      value = value.substr(1, value.size() - 2);
    }
    if (!value.empty()) {
      return project_root / value;
    }
  }

  return project_root / "src" / "main.snow";
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
        .triple = request.target_triple.empty() ? "host" : request.target_triple,
        .executable_entry_wrapper = request.output_kind == OutputKind::Executable,
    };
    const auto llvm_result = lowering.Lower(pass_result.module, target, request.opt_level);
    if (request.emit.llvm) {
      result.llvm_dump = llvm_result.llvm_ir;
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
  const std::filesystem::path main_file = ResolveMainFile(root);
  const std::filesystem::path main_file_abs = std::filesystem::weakly_canonical(main_file);

  if (!std::filesystem::exists(main_file)) {
    result.diagnostics.Error("E_BUILD_MAIN_NOT_FOUND", "Main source file not found", main_file.string(), {0, 0, 0, 0},
                             "Create src/main.snow or set main in snow.toml");
    return result;
  }

  std::unordered_map<std::string, ModuleNode> nodes;
  (void)BuildModuleGraph(main_file_abs, src_root, nodes, result.diagnostics);

  std::vector<std::string> topo;
  (void)TopologicalOrder(nodes, topo, result.diagnostics);
  result.module_order = topo;

  for (const auto& module_id : topo) {
    const auto it = nodes.find(module_id);
    if (it == nodes.end()) {
      continue;
    }

    CompileRequest module_request;
    module_request.input_path = it->second.file_path.string();
    module_request.target_triple = request.target_triple;
    module_request.opt_level = request.opt_level;
    module_request.output_kind = request.output_kind;

    if (std::filesystem::weakly_canonical(it->second.file_path) == main_file_abs) {
      module_request.emit = request.emit;
    }

    auto compile_result = Compile(module_request);
    result.diagnostics.Append(compile_result.diagnostics);
    result.module_compiles.push_back(std::move(compile_result));
  }

  std::ostringstream summary;
  summary << "build modules: " << result.module_order.size() << "\n";
  for (const auto& module_id : result.module_order) {
    summary << "  - " << module_id << "\n";
  }
  result.summary = summary.str();

  result.success = !result.diagnostics.HasErrors();
  return result;
}

}  // namespace snow::driver
