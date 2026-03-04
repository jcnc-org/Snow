#pragma once

#include <optional>
#include <string>
#include <vector>

namespace snow::sir {

enum class Opcode {
  Add,
  Sub,
  Mul,
  Div,
  Eq,
  Ne,
  Lt,
  Gt,
  Le,
  Ge,
  Phi,
  Br,
  CondBr,
  Ret,
  Unreachable,
  Alloc,
  Load,
  Store,
  Drop,
  Call,
  Extract,
  Insert,
};

enum class Linkage {
  External,
  Internal,
  Private,
};

struct Instruction {
  std::optional<std::string> result;
  std::string type;
  Opcode opcode = Opcode::Unreachable;
  std::vector<std::string> operands;
  bool is_terminator = false;
};

struct BasicBlock {
  std::string label;
  std::vector<Instruction> instructions;
};

struct Function {
  std::string original_name;
  std::string name;
  std::string return_type;
  Linkage linkage = Linkage::Internal;
  std::vector<BasicBlock> blocks;
};

struct Module {
  std::string module_path;
  std::vector<Function> functions;
};

std::string ToString(Opcode opcode);
std::string ToString(Linkage linkage);
std::string DumpSir(const Module& module);
std::string DumpCfg(const Module& module);

}  // namespace snow::sir
