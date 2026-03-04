#include <filesystem>
#include <fstream>
#include <iostream>
#include <string>
#include <vector>

#include "snow/driver/driver.h"

namespace {

void PrintUsage() {
  std::cout << "snowc <command> [options] [input]\\n"
            << "commands: compile run build init clean version\\n"
            << "compile options:\\n"
            << "  --target <triple>\\n"
            << "  --opt=0|2\\n"
            << "  --emit-tokens --emit-ast --emit-sema --emit-sir --emit-cfg --emit-llvm\\n";
}

snow::passes::OptLevel ParseOpt(const std::string& arg) {
  if (arg == "--opt=2") {
    return snow::passes::OptLevel::O2;
  }
  return snow::passes::OptLevel::O0;
}

int RunCompileLike(const std::string& command, const std::vector<std::string>& args) {
  snow::driver::CompileRequest request;
  request.target_triple = "";

  for (std::size_t i = 0; i < args.size(); ++i) {
    const auto& arg = args[i];

    if (arg == "--target") {
      if (i + 1 >= args.size()) {
        std::cerr << "missing target triple after --target\\n";
        return 2;
      }
      request.target_triple = args[++i];
      continue;
    }

    if (arg.rfind("--opt=", 0) == 0) {
      request.opt_level = ParseOpt(arg);
      continue;
    }

    if (arg == "--emit-tokens") {
      request.emit.tokens = true;
      continue;
    }
    if (arg == "--emit-ast") {
      request.emit.ast = true;
      continue;
    }
    if (arg == "--emit-sema") {
      request.emit.sema = true;
      continue;
    }
    if (arg == "--emit-sir") {
      request.emit.sir = true;
      continue;
    }
    if (arg == "--emit-cfg") {
      request.emit.cfg = true;
      continue;
    }
    if (arg == "--emit-llvm") {
      request.emit.llvm = true;
      continue;
    }

    if (!arg.empty() && arg[0] == '-') {
      std::cerr << "unknown option: " << arg << "\\n";
      return 2;
    }

    if (request.input_path.empty()) {
      request.input_path = arg;
    }
  }

  if (request.input_path.empty()) {
    std::cerr << "missing input file\\n";
    return 2;
  }

  const snow::driver::Driver driver;
  const auto result = driver.Compile(request);

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

  const auto diag_text = snow::driver::RenderDiagnostics(result.diagnostics);
  if (!diag_text.empty()) {
    std::cerr << diag_text;
  }

  if (!result.success) {
    return 1;
  }

  if (command == "run") {
    std::cout << "run: code execution not implemented in bootstrap yet\\n";
  }

  return 0;
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
    out << "[package]\\n";
    out << "name = \"snow-app\"\\n";
    out << "version = \"0.1.0\"\\n";
    out << "edition = \"v2\"\\n";
  }

  const auto main_file = root / "src" / "main.snow";
  if (!std::filesystem::exists(main_file)) {
    std::ofstream out(main_file);
    out << "fn main() -> i32 {\\n";
    out << "  return 0;\\n";
    out << "}\\n";
  }

  std::cout << "initialized Snow project at " << root.string() << "\\n";
  return 0;
}

int RunClean() {
  std::error_code ec;
  std::filesystem::remove_all("snow-build", ec);
  std::filesystem::remove_all("build", ec);
  std::cout << "cleaned build artifacts\\n";
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
    std::cout << "snowc v1.0.0-cpp-alpha\\n";
    return 0;
  }

  if (command == "compile" || command == "run" || command == "build") {
    return RunCompileLike(command, args);
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
