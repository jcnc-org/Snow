#include "snow/common/manifest.h"

#include <fstream>

namespace snow::common {

namespace {

std::string Trim(const std::string& in) {
  const auto first = in.find_first_not_of(" \t\r\n");
  if (first == std::string::npos) {
    return "";
  }
  const auto last = in.find_last_not_of(" \t\r\n");
  return in.substr(first, last - first + 1);
}

std::string Unquote(std::string value) {
  value = Trim(value);
  if (value.size() >= 2 && value.front() == '"' && value.back() == '"') {
    return value.substr(1, value.size() - 2);
  }
  return value;
}

}  // namespace

bool ParseSnowToml(const std::string& manifest_path, SnowManifest& out_manifest, std::string& error) {
  std::ifstream in(manifest_path);
  if (!in) {
    error = "cannot open manifest: " + manifest_path;
    return false;
  }

  std::string current_section;
  std::string line;

  while (std::getline(in, line)) {
    const std::string t = Trim(line);
    if (t.empty() || t[0] == '#') {
      continue;
    }

    if (t.front() == '[' && t.back() == ']') {
      current_section = Trim(t.substr(1, t.size() - 2));
      continue;
    }

    const auto eq = t.find('=');
    if (eq == std::string::npos) {
      continue;
    }

    const std::string key = Trim(t.substr(0, eq));
    const std::string value = Unquote(t.substr(eq + 1));

    if (current_section == "package") {
      if (key == "name") {
        out_manifest.name = value;
      } else if (key == "version") {
        out_manifest.version = value;
      } else if (key == "edition") {
        out_manifest.edition = value;
      } else if (key == "main") {
        out_manifest.main = value;
      }
      continue;
    }

    if (current_section == "build") {
      if (key == "target") {
        out_manifest.target = value;
      }
      continue;
    }

    if (current_section == "dependencies") {
      out_manifest.dependencies[key] = value;
      continue;
    }

    // Unknown sections are ignored in bootstrap parser.
  }

  return true;
}

}  // namespace snow::common
