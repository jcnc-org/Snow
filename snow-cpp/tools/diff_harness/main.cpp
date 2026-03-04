#include <array>
#include <cstdio>
#include <cstdlib>
#include <iostream>
#include <sstream>
#include <string>
#include <vector>

namespace {

std::string Quote(const std::string& text) {
  std::string out = "\"";
  for (const char ch : text) {
    if (ch == '"') {
      out += "\\\"";
    } else {
      out += ch;
    }
  }
  out += "\"";
  return out;
}

std::string EnsureQuoted(const std::string& text) {
  if (text.size() >= 2 && text.front() == '"' && text.back() == '"') {
    return text;
  }
  return Quote(text);
}

std::string ExecCapture(const std::string& command, int& exit_code) {
  std::array<char, 512> buffer{};
  std::string output;

#if defined(_WIN32)
  FILE* pipe = _popen((command + " 2>&1").c_str(), "r");
#else
  FILE* pipe = popen((command + " 2>&1").c_str(), "r");
#endif
  if (pipe == nullptr) {
    exit_code = -1;
    return "<failed to spawn command>";
  }

  while (fgets(buffer.data(), static_cast<int>(buffer.size()), pipe) != nullptr) {
    output += buffer.data();
  }

#if defined(_WIN32)
  exit_code = _pclose(pipe);
#else
  exit_code = pclose(pipe);
#endif
  return output;
}

std::string Trim(const std::string& in) {
  const auto first = in.find_first_not_of(" \t\r\n");
  if (first == std::string::npos) {
    return "";
  }
  const auto last = in.find_last_not_of(" \t\r\n");
  return in.substr(first, last - first + 1);
}

std::string ClassifyMismatch(const std::string& java_out, const std::string& cpp_out) {
  if (java_out == cpp_out) {
    return "MATCH";
  }

  const bool java_has_error = java_out.find("error") != std::string::npos || java_out.find("Exception") != std::string::npos;
  const bool cpp_has_error = cpp_out.find("error") != std::string::npos;

  if (java_has_error && !cpp_has_error) {
    return "JAVA_BUG";
  }
  if (!java_has_error && cpp_has_error) {
    return "LLVM_BUG";
  }
  return "SEMANTIC_UNSPECIFIED";
}

}  // namespace

int main(int argc, char** argv) {
  std::string java_cmd = "java -jar snow-java-cli.jar";
  std::string snowc_cmd = "snowc";
  std::string input;

  for (int i = 1; i < argc; ++i) {
    const std::string arg = argv[i];
    if (arg == "--java-cmd" && i + 1 < argc) {
      java_cmd = argv[++i];
      continue;
    }
    if (arg == "--snowc" && i + 1 < argc) {
      snowc_cmd = argv[++i];
      continue;
    }
    if (arg.rfind("--", 0) == 0) {
      std::cerr << "unknown option: " << arg << "\n";
      return 2;
    }
    input = arg;
  }

  if (input.empty()) {
    std::cerr << "usage: snow-diff-harness [--java-cmd <cmd>] [--snowc <path>] <input.snow>\n";
    return 2;
  }

  const std::string java_compile = EnsureQuoted(java_cmd) + " compile " + EnsureQuoted(input);
  const std::string cpp_compile = EnsureQuoted(snowc_cmd) + " compile --emit-llvm " + EnsureQuoted(input);

  int java_rc = 0;
  int cpp_rc = 0;
  const std::string java_out = ExecCapture(java_compile, java_rc);
  const std::string cpp_out = ExecCapture(cpp_compile, cpp_rc);

  const std::string classification = ClassifyMismatch(Trim(java_out), Trim(cpp_out));

  std::cout << "java_rc=" << java_rc << "\n";
  std::cout << "cpp_rc=" << cpp_rc << "\n";
  std::cout << "classification=" << classification << "\n";

  if (classification != "MATCH") {
    std::cout << "--- java output ---\n" << java_out << "\n";
    std::cout << "--- cpp output ---\n" << cpp_out << "\n";
  }

  return classification == "MATCH" ? 0 : 1;
}
