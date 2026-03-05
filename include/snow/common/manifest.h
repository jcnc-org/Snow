#pragma once

#include <string>
#include <unordered_map>

namespace snow::common {

struct SnowManifest {
  std::string name = "snow-app";
  std::string version = "0.1.0";
  std::string edition = "v2";
  std::string main = "src/main.snow";
  std::string target;
  std::unordered_map<std::string, std::string> dependencies;
};

bool ParseSnowToml(const std::string& manifest_path, SnowManifest& out_manifest, std::string& error);

}  // namespace snow::common
