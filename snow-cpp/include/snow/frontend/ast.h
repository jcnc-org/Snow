#pragma once

#include <string>
#include <vector>

namespace snow::frontend {

enum class Visibility {
  Private,
  Internal,
  Public,
};

struct ImportDecl {
  std::vector<std::string> path_segments;
  std::string alias;
  bool is_star = false;
};

struct ParamDecl {
  std::string name;
  std::string type;
};

struct FunctionDecl {
  Visibility visibility = Visibility::Private;
  std::string name;
  std::vector<ParamDecl> params;
  std::string return_type;
};

struct AstModule {
  std::string module_path;
  std::vector<ImportDecl> imports;
  std::vector<FunctionDecl> functions;
};

std::string ToString(Visibility visibility);
std::string DumpAst(const AstModule& module);

}  // namespace snow::frontend
