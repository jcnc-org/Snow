#include "snow/sema/sema.h"

#include <sstream>
#include <unordered_map>
#include <utility>

namespace snow::sema {

namespace {

std::string JoinPath(const std::vector<std::string>& parts) {
  std::ostringstream oss;
  for (std::size_t i = 0; i < parts.size(); ++i) {
    if (i > 0) {
      oss << ".";
    }
    oss << parts[i];
  }
  return oss.str();
}

}  // namespace

SemaModule SemanticAnalyzer::Analyze(const snow::frontend::AstModule& ast_module,
                                     snow::common::DiagnosticEngine& diagnostics) const {
  SemaModule sema;
  sema.ast = ast_module;

  std::unordered_map<std::string, std::string> import_owner;
  for (const auto& import : ast_module.imports) {
    ResolvedImport resolved;
    resolved.canonical_path = JoinPath(import.path_segments);
    resolved.alias = import.alias;
    resolved.is_star = import.is_star;

    if (import.is_star) {
      diagnostics.Warning("W_STAR_IMPORT_DISCOURAGED", "star import is discouraged", ast_module.module_path,
                          {0, 0, 0, 0}, "Use explicit import or alias import for stable name resolution");
      resolved.unqualified_name = "";
    } else if (!import.alias.empty()) {
      resolved.unqualified_name = import.alias;
    } else if (!import.path_segments.empty()) {
      resolved.unqualified_name = import.path_segments.back();
    }

    if (!resolved.unqualified_name.empty()) {
      const auto it = import_owner.find(resolved.unqualified_name);
      if (it != import_owner.end() && it->second != resolved.canonical_path) {
        diagnostics.Error(
            "AmbiguousSymbol",
            "Unqualified symbol conflict for import name '" + resolved.unqualified_name + "'", ast_module.module_path,
            {0, 0, 0, 0}, "Use alias import (as ...) or qualified module path");
      } else {
        import_owner.emplace(resolved.unqualified_name, resolved.canonical_path);
      }
    }

    sema.imports.push_back(std::move(resolved));
  }

  std::unordered_map<std::string, bool> symbol_seen;
  for (const auto& function : ast_module.functions) {
    if (symbol_seen.contains(function.name)) {
      diagnostics.Error("E_SEMA_DUP_SYMBOL", "Duplicate symbol in module: " + function.name, ast_module.module_path,
                        {0, 0, 0, 0});
      continue;
    }
    symbol_seen[function.name] = true;
    sema.symbols.push_back(function.name);
  }

  return sema;
}

std::string DumpSema(const SemaModule& module) {
  std::ostringstream oss;
  oss << "sema module " << module.ast.module_path << "\n";
  oss << "symbols:\n";
  for (const auto& symbol : module.symbols) {
    oss << "  - " << symbol << "\n";
  }

  oss << "imports:\n";
  for (const auto& import : module.imports) {
    oss << "  - " << import.canonical_path;
    if (!import.alias.empty()) {
      oss << " as " << import.alias;
    }
    if (import.is_star) {
      oss << " (star)";
    }
    if (!import.unqualified_name.empty()) {
      oss << " unqualified=" << import.unqualified_name;
    }
    oss << "\n";
  }
  oss << "resolution-order: local -> current module -> imported modules\n";
  return oss.str();
}

}  // namespace snow::sema
