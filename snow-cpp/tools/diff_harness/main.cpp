#include <algorithm>
#include <array>
#include <cctype>
#include <cstdio>
#include <cstdlib>
#include <fstream>
#include <iostream>
#include <sstream>
#include <string>

namespace {

struct ExecResult {
  int exit_code = -1;
  bool spawn_failed = false;
  std::string output;
};

struct ClassificationResult {
  std::string label;
  bool classified = false;
  std::string reason;
};

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

std::string Trim(const std::string& in) {
  const auto first = in.find_first_not_of(" \t\r\n");
  if (first == std::string::npos) {
    return "";
  }
  const auto last = in.find_last_not_of(" \t\r\n");
  return in.substr(first, last - first + 1);
}

std::string EscapeJson(const std::string& input) {
  std::ostringstream oss;
  for (const unsigned char ch : input) {
    switch (ch) {
      case '\\':
        oss << "\\\\";
        break;
      case '"':
        oss << "\\\"";
        break;
      case '\n':
        oss << "\\n";
        break;
      case '\r':
        oss << "\\r";
        break;
      case '\t':
        oss << "\\t";
        break;
      default:
        if (ch < 0x20) {
          oss << "\\u00" << "0123456789abcdef"[ch >> 4] << "0123456789abcdef"[ch & 0xF];
        } else {
          oss << static_cast<char>(ch);
        }
        break;
    }
  }
  return oss.str();
}

ExecResult ExecCapture(const std::string& command) {
  std::array<char, 512> buffer{};
  ExecResult result;

#if defined(_WIN32)
  FILE* pipe = _popen((command + " 2>&1").c_str(), "r");
#else
  FILE* pipe = popen((command + " 2>&1").c_str(), "r");
#endif
  if (pipe == nullptr) {
    result.spawn_failed = true;
    result.exit_code = -1;
    result.output = "<failed to spawn command>";
    return result;
  }

  while (fgets(buffer.data(), static_cast<int>(buffer.size()), pipe) != nullptr) {
    result.output += buffer.data();
  }

#if defined(_WIN32)
  result.exit_code = _pclose(pipe);
#else
  result.exit_code = pclose(pipe);
#endif
  return result;
}

ClassificationResult ClassifyMismatch(const ExecResult& java_result, const ExecResult& cpp_result) {
  if (java_result.spawn_failed || cpp_result.spawn_failed) {
    return ClassificationResult{
        .label = "UNCLASSIFIED",
        .classified = false,
        .reason = "process spawn failure",
    };
  }

  const std::string java_trimmed = Trim(java_result.output);
  const std::string cpp_trimmed = Trim(cpp_result.output);
  if (java_result.exit_code == cpp_result.exit_code && java_trimmed == cpp_trimmed) {
    return ClassificationResult{
        .label = "MATCH",
        .classified = true,
        .reason = "outputs and exit codes match",
    };
  }

  if (java_result.exit_code != 0 && cpp_result.exit_code == 0) {
    return ClassificationResult{
        .label = "JAVA_BUG",
        .classified = true,
        .reason = "java failed while c++ succeeded",
    };
  }
  if (java_result.exit_code == 0 && cpp_result.exit_code != 0) {
    return ClassificationResult{
        .label = "LLVM_BUG",
        .classified = true,
        .reason = "c++ failed while java succeeded",
    };
  }

  if (java_trimmed != cpp_trimmed || java_result.exit_code != cpp_result.exit_code) {
    return ClassificationResult{
        .label = "SEMANTIC_UNSPECIFIED",
        .classified = true,
        .reason = "behavior differs but ownership is ambiguous",
    };
  }

  return ClassificationResult{
      .label = "UNCLASSIFIED",
      .classified = false,
      .reason = "unable to classify mismatch",
  };
}

bool EndsWithLower(std::string text, const std::string& suffix_lower) {
  std::transform(text.begin(), text.end(), text.begin(),
                 [](const unsigned char c) { return static_cast<char>(std::tolower(c)); });
  if (text.size() < suffix_lower.size()) {
    return false;
  }
  return text.compare(text.size() - suffix_lower.size(), suffix_lower.size(), suffix_lower) == 0;
}

bool WriteJsonReport(const std::string& path, const std::string& java_command, const std::string& cpp_command,
                     const ExecResult& java_result, const ExecResult& cpp_result,
                     const ClassificationResult& classification) {
  std::ofstream out(path, std::ios::out | std::ios::trunc);
  if (!out) {
    return false;
  }

  out << "{\n";
  out << "  \"classification\": \"" << EscapeJson(classification.label) << "\",\n";
  out << "  \"classified\": " << (classification.classified ? "true" : "false") << ",\n";
  out << "  \"reason\": \"" << EscapeJson(classification.reason) << "\",\n";
  out << "  \"java\": {\n";
  out << "    \"command\": \"" << EscapeJson(java_command) << "\",\n";
  out << "    \"exit_code\": " << java_result.exit_code << ",\n";
  out << "    \"spawn_failed\": " << (java_result.spawn_failed ? "true" : "false") << ",\n";
  out << "    \"output\": \"" << EscapeJson(java_result.output) << "\"\n";
  out << "  },\n";
  out << "  \"cpp\": {\n";
  out << "    \"command\": \"" << EscapeJson(cpp_command) << "\",\n";
  out << "    \"exit_code\": " << cpp_result.exit_code << ",\n";
  out << "    \"spawn_failed\": " << (cpp_result.spawn_failed ? "true" : "false") << ",\n";
  out << "    \"output\": \"" << EscapeJson(cpp_result.output) << "\"\n";
  out << "  }\n";
  out << "}\n";
  return true;
}

bool WriteTextReport(const std::string& path, const std::string& java_command, const std::string& cpp_command,
                     const ExecResult& java_result, const ExecResult& cpp_result,
                     const ClassificationResult& classification) {
  std::ofstream out(path, std::ios::out | std::ios::trunc);
  if (!out) {
    return false;
  }

  out << "classification=" << classification.label << "\n";
  out << "classified=" << (classification.classified ? "true" : "false") << "\n";
  out << "reason=" << classification.reason << "\n";
  out << "java_command=" << java_command << "\n";
  out << "java_exit=" << java_result.exit_code << "\n";
  out << "cpp_command=" << cpp_command << "\n";
  out << "cpp_exit=" << cpp_result.exit_code << "\n";
  out << "--- java output ---\n" << java_result.output << "\n";
  out << "--- cpp output ---\n" << cpp_result.output << "\n";
  return true;
}

bool WriteReport(const std::string& path, const std::string& java_command, const std::string& cpp_command,
                 const ExecResult& java_result, const ExecResult& cpp_result,
                 const ClassificationResult& classification) {
  if (path.empty()) {
    return true;
  }
  if (EndsWithLower(path, ".txt")) {
    return WriteTextReport(path, java_command, cpp_command, java_result, cpp_result, classification);
  }
  return WriteJsonReport(path, java_command, cpp_command, java_result, cpp_result, classification);
}

}  // namespace

int main(int argc, char** argv) {
  std::string java_cmd = "java -jar snow-java-cli.jar";
  std::string snowc_cmd = "snowc";
  std::string report_path;
  bool fail_on_unclassified = false;
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
    if (arg == "--report" && i + 1 < argc) {
      report_path = argv[++i];
      continue;
    }
    if (arg == "--fail-on-unclassified") {
      fail_on_unclassified = true;
      continue;
    }
    if (arg.rfind("--", 0) == 0) {
      std::cerr << "unknown option: " << arg << "\n";
      return 2;
    }
    input = arg;
  }

  if (input.empty()) {
    std::cerr << "usage: snow-diff-harness [--java-cmd <cmd>] [--snowc <path>] [--report <path>] "
                 "[--fail-on-unclassified] <input.snow>\n";
    return 2;
  }

  const std::string java_compile = java_cmd + " compile " + Quote(input);
  const std::string cpp_compile = snowc_cmd + " compile --emit-llvm " + Quote(input);

  const ExecResult java_result = ExecCapture(java_compile);
  const ExecResult cpp_result = ExecCapture(cpp_compile);
  const ClassificationResult classification = ClassifyMismatch(java_result, cpp_result);

  std::cout << "java_rc=" << java_result.exit_code << "\n";
  std::cout << "cpp_rc=" << cpp_result.exit_code << "\n";
  std::cout << "classification=" << classification.label << "\n";
  std::cout << "classified=" << (classification.classified ? "true" : "false") << "\n";
  std::cout << "reason=" << classification.reason << "\n";

  if (!WriteReport(report_path, java_compile, cpp_compile, java_result, cpp_result, classification)) {
    std::cerr << "failed to write report: " << report_path << "\n";
    return 2;
  }

  if (classification.label != "MATCH") {
    std::cout << "--- java output ---\n" << java_result.output << "\n";
    std::cout << "--- cpp output ---\n" << cpp_result.output << "\n";
  }

  if (classification.label == "MATCH") {
    return 0;
  }
  if (classification.label == "UNCLASSIFIED") {
    return fail_on_unclassified ? 1 : 0;
  }
  return 1;
}
