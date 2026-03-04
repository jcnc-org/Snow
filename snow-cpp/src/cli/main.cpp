#include <filesystem>
#include <fstream>
#include <cstdlib>
#include <iostream>
#include <string>
#include <vector>

#include "snow/driver/driver.h"

namespace {

void PrintUsage() {
  std::cout << "snowc <command> [options] [input]\n"
            << "commands: compile run build init clean version\n"
            << "shared options:\n"
            << "  --target <triple>\n"
            << "  --opt=0|2\n"
            << "  --emit-tokens --emit-ast --emit-sema --emit-sir --emit-cfg --emit-llvm\n"
            << "  --emit-object --emit-lib --emit-exe\n";
}

struct ParsedOptions {
  std::string target_triple;
  std::string output_path;
  snow::passes::OptLevel opt_level = snow::passes::OptLevel::O0;
  snow::driver::OutputKind output_kind = snow::driver::OutputKind::Executable;
  snow::driver::EmitOptions emit;
  std::vector<std::string> positional;
};

snow::passes::OptLevel ParseOpt(const std::string& arg) {
  if (arg == "--opt=2") {
    return snow::passes::OptLevel::O2;
  }
  return snow::passes::OptLevel::O0;
}

bool ParseCommonOptions(const std::vector<std::string>& args, ParsedOptions& parsed, std::string& error) {
  for (std::size_t i = 0; i < args.size(); ++i) {
    const auto& arg = args[i];

    if (arg == "--target") {
      if (i + 1 >= args.size()) {
        error = "missing target triple after --target";
        return false;
      }
      parsed.target_triple = args[++i];
      continue;
    }

    if (arg.rfind("--opt=", 0) == 0) {
      parsed.opt_level = ParseOpt(arg);
      continue;
    }

    if (arg == "-o" || arg == "--out") {
      if (i + 1 >= args.size()) {
        error = "missing output path after " + arg;
        return false;
      }
      parsed.output_path = args[++i];
      continue;
    }

    if (arg == "--emit-object") {
      parsed.output_kind = snow::driver::OutputKind::Object;
      continue;
    }
    if (arg == "--emit-lib") {
      parsed.output_kind = snow::driver::OutputKind::Library;
      continue;
    }
    if (arg == "--emit-exe") {
      parsed.output_kind = snow::driver::OutputKind::Executable;
      continue;
    }

    if (arg == "--emit-tokens") {
      parsed.emit.tokens = true;
      continue;
    }
    if (arg == "--emit-ast") {
      parsed.emit.ast = true;
      continue;
    }
    if (arg == "--emit-sema") {
      parsed.emit.sema = true;
      continue;
    }
    if (arg == "--emit-sir") {
      parsed.emit.sir = true;
      continue;
    }
    if (arg == "--emit-cfg") {
      parsed.emit.cfg = true;
      continue;
    }
    if (arg == "--emit-llvm") {
      parsed.emit.llvm = true;
      continue;
    }

    if (!arg.empty() && arg[0] == '-') {
      error = "unknown option: " + arg;
      return false;
    }

    parsed.positional.push_back(arg);
  }

  return true;
}

void PrintCompileDumps(const snow::driver::CompileResult& result, const snow::driver::CompileRequest& request) {
  if (!result.token_dump.empty()) {
    std::cout << result.token_dump;
  }
  if (!result.ast_dump.empty()) {
    std::cout << result.ast_dump;
  }
  if (!result.sema_dump.empty()) {
    std::cout << result.sema_dump;
  }
  if (!result.ownership_dump.empty() && request.emit.sema) {
    std::cout << result.ownership_dump;
  }
  if (!result.sir_dump.empty()) {
    std::cout << result.sir_dump;
  }
  if (!result.cfg_dump.empty()) {
    std::cout << result.cfg_dump;
  }
  if (!result.llvm_dump.empty()) {
    std::cout << result.llvm_dump;
  }
}

int PrintCompileResult(const snow::driver::CompileResult& result, const snow::driver::CompileRequest& request) {
  PrintCompileDumps(result, request);
  if (result.success && !result.artifact_path.empty()) {
    std::cout << "artifact: " << result.artifact_path << "\n";
    std::cout << "target: " << result.target_triple << "\n";
  }
  const auto diag_text = snow::driver::RenderDiagnostics(result.diagnostics);
  if (!diag_text.empty()) {
    std::cerr << diag_text;
  }

  return result.success ? 0 : 1;
}

int RunCompile(const std::vector<std::string>& args) {
  ParsedOptions parsed;
  std::string error;
  if (!ParseCommonOptions(args, parsed, error)) {
    std::cerr << error << "\n";
    return 2;
  }

  if (parsed.positional.empty()) {
    std::cerr << "missing input file\n";
    return 2;
  }

  snow::driver::CompileRequest request;
  request.input_path = parsed.positional[0];
  request.target_triple = parsed.target_triple;
  request.opt_level = parsed.opt_level;
  request.output_kind = parsed.output_kind;
  request.output_path = parsed.output_path;
  request.emit = parsed.emit;

  const snow::driver::Driver driver;
  const auto result = driver.Compile(request);
  return PrintCompileResult(result, request);
}

int RunBuild(const std::vector<std::string>& args) {
  ParsedOptions parsed;
  std::string error;
  if (!ParseCommonOptions(args, parsed, error)) {
    std::cerr << error << "\n";
    return 2;
  }

  snow::driver::BuildRequest request;
  request.project_root = parsed.positional.empty() ? "." : parsed.positional[0];
  request.target_triple = parsed.target_triple;
  request.output_path = parsed.output_path;
  request.opt_level = parsed.opt_level;
  request.output_kind = parsed.output_kind;
  request.emit = parsed.emit;

  const snow::driver::Driver driver;
  const auto result = driver.BuildProject(request);

  if (!result.summary.empty()) {
    std::cout << result.summary;
  }

  for (const auto& module_result : result.module_compiles) {
    snow::driver::CompileRequest module_request;
    module_request.emit = parsed.emit;
    PrintCompileDumps(module_result, module_request);
  }

  const auto diag_text = snow::driver::RenderDiagnostics(result.diagnostics);
  if (!diag_text.empty()) {
    std::cerr << diag_text;
  }

  return result.success ? 0 : 1;
}

int RunRun(const std::vector<std::string>& args) {
  ParsedOptions parsed;
  std::string error;
  if (!ParseCommonOptions(args, parsed, error)) {
    std::cerr << error << "\n";
    return 2;
  }

  if (parsed.positional.empty()) {
    std::cerr << "missing input file\n";
    return 2;
  }

  snow::driver::CompileRequest request;
  request.input_path = parsed.positional[0];
  request.target_triple = parsed.target_triple;
  request.opt_level = parsed.opt_level;
  request.output_kind = snow::driver::OutputKind::Executable;
  request.output_path = parsed.output_path;
  request.emit = parsed.emit;

  const snow::driver::Driver driver;
  const auto result = driver.Compile(request);
  const int compile_rc = PrintCompileResult(result, request);
  if (compile_rc != 0) {
    return compile_rc;
  }

  if (result.artifact_path.empty()) {
    std::cerr << "run: no executable artifact produced\n";
    return 1;
  }

  std::ifstream in(result.artifact_path, std::ios::in | std::ios::binary);
  if (in) {
    std::string first_line;
    std::getline(in, first_line);
    if (first_line.rfind("# snow artifact (bootstrap)", 0) == 0) {
      std::cerr << "run: executable artifact is bootstrap text (native toolchain unavailable)\n";
      return 1;
    }
  }

  const std::string command = "\"" + result.artifact_path + "\"";
  const int run_rc = std::system(command.c_str());
  if (run_rc < 0) {
    std::cerr << "run: failed to execute artifact\n";
    return 1;
  }

  std::cout << "run: exit-code=" << run_rc << "\n";
  return run_rc;
}

int RunInit(const std::vector<std::string>& args) {
  std::filesystem::path root = std::filesystem::current_path();
  if (!args.empty()) {
    root = args[0];
  }

  std::filesystem::create_directories(root / "src");

  const auto manifest = root / "snow.toml";
  if (!std::filesystem::exists(manifest)) {
    std::ofstream out(manifest);
    out << "[package]\n";
    out << "name = \"snow-app\"\n";
    out << "version = \"0.1.0\"\n";
    out << "edition = \"v2\"\n";
    out << "main = \"src/main.snow\"\n";
    out << "\n[dependencies]\n";
  }

  const auto main_file = root / "src" / "main.snow";
  if (!std::filesystem::exists(main_file)) {
    std::ofstream out(main_file);
    out << "fn main() -> i32 {\n";
    out << "  return 0;\n";
    out << "}\n";
  }

  std::cout << "initialized Snow project at " << root.string() << "\n";
  return 0;
}

int RunClean() {
  std::error_code ec;
  std::filesystem::remove_all("snow-build", ec);
  std::filesystem::remove_all("build", ec);
  std::filesystem::remove_all("snow-cpp/build", ec);
  std::cout << "cleaned build artifacts\n";
  return 0;
}

}  // namespace

int main(int argc, char** argv) {
  if (argc < 2) {
    PrintUsage();
    return 2;
  }

  const std::string command = argv[1];
  const std::vector<std::string> args(argv + 2, argv + argc);

  if (command == "version") {
    std::cout << "snowc v1.0.0-cpp-alpha\n";
    return 0;
  }

  if (command == "compile") {
    return RunCompile(args);
  }

  if (command == "build") {
    return RunBuild(args);
  }

  if (command == "run") {
    return RunRun(args);
  }

  if (command == "init") {
    return RunInit(args);
  }

  if (command == "clean") {
    return RunClean();
  }

  PrintUsage();
  return 2;
}
