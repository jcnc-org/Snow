#include <fstream>
#include <iostream>
#include <map>
#include <sstream>
#include <string>

namespace {

std::string Trim(const std::string& in) {
  const auto first = in.find_first_not_of(" \t\r\n");
  if (first == std::string::npos) {
    return "";
  }
  const auto last = in.find_last_not_of(" \t\r\n");
  return in.substr(first, last - first + 1);
}

}  // namespace

int main(int argc, char** argv) {
  const std::string in_path = argc > 1 ? argv[1] : "project.cloud";
  const std::string out_path = argc > 2 ? argv[2] : "snow.toml";

  std::ifstream in(in_path);
  if (!in) {
    std::cerr << "cannot read input: " << in_path << "\n";
    return 1;
  }

  std::map<std::string, std::string> kv;
  std::string line;
  while (std::getline(in, line)) {
    const std::string t = Trim(line);
    if (t.empty() || t[0] == '#') {
      continue;
    }
    const auto pos = t.find(':');
    if (pos == std::string::npos) {
      continue;
    }
    const auto key = Trim(t.substr(0, pos));
    const auto value = Trim(t.substr(pos + 1));
    kv[key] = value;
  }

  std::ofstream out(out_path);
  if (!out) {
    std::cerr << "cannot write output: " << out_path << "\n";
    return 1;
  }

  const std::string name = kv.contains("name") ? kv["name"] : "snow-app";
  const std::string version = kv.contains("version") ? kv["version"] : "0.1.0";

  out << "[package]\n";
  out << "name = \"" << name << "\"\n";
  out << "version = \"" << version << "\"\n";
  out << "edition = \"v2\"\n\n";
  out << "[dependencies]\n";

  if (kv.contains("dependencies")) {
    std::stringstream ss(kv["dependencies"]);
    std::string item;
    while (std::getline(ss, item, ',')) {
      const auto dep = Trim(item);
      if (!dep.empty()) {
        out << dep << " = \"*\"\n";
      }
    }
  }

  std::cout << "converted " << in_path << " -> " << out_path << "\n";
  return 0;
}
