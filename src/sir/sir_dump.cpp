// Module: SIR opcode/linkage stringification and deterministic text dumps.

#include "snow/sir/sir.h"

#include <sstream>

namespace snow::sir {

    std::string ToString(const Opcode opcode) {
        switch (opcode) {
            case Opcode::Add:
                return "add";
            case Opcode::Sub:
                return "sub";
            case Opcode::Mul:
                return "mul";
            case Opcode::Div:
                return "div";
            case Opcode::Eq:
                return "eq";
            case Opcode::Ne:
                return "ne";
            case Opcode::Lt:
                return "lt";
            case Opcode::Gt:
                return "gt";
            case Opcode::Le:
                return "le";
            case Opcode::Ge:
                return "ge";
            case Opcode::Phi:
                return "phi";
            case Opcode::Br:
                return "br";
            case Opcode::CondBr:
                return "cond_br";
            case Opcode::Ret:
                return "ret";
            case Opcode::Unreachable:
                return "unreachable";
            case Opcode::Alloc:
                return "alloc";
            case Opcode::Load:
                return "load";
            case Opcode::Store:
                return "store";
            case Opcode::Drop:
                return "drop";
            case Opcode::Call:
                return "call";
            case Opcode::Extract:
                return "extract";
            case Opcode::Insert:
                return "insert";
        }
        return "unknown";
    }

    std::string ToString(const Linkage linkage) {
        switch (linkage) {
            case Linkage::External:
                return "external";
            case Linkage::Internal:
                return "internal";
            case Linkage::Private:
                return "private";
        }
        return "internal";
    }

    std::string DumpSir(const Module &module) {
        std::ostringstream oss;
        oss << "sir module " << module.module_path << "\n";
        for (const auto &function: module.functions) {
            oss << "fn " << function.name;
            oss << " ; linkage=" << ToString(function.linkage);
            if (!function.original_name.empty()) {
                oss << " ; original=" << function.original_name;
            }
            oss << "\n";
            for (const auto &block: function.blocks) {
                oss << block.label << ":\n";
                for (const auto &instr: block.instructions) {
                    oss << "  ";
                    if (instr.result.has_value()) {
                        oss << instr.result.value() << " = ";
                    }
                    oss << ToString(instr.opcode);
                    if (!instr.operands.empty()) {
                        oss << " ";
                        for (std::size_t i = 0; i < instr.operands.size(); ++i) {
                            if (i > 0) {
                                oss << ", ";
                            }
                            oss << instr.operands[i];
                        }
                    }
                    if (!instr.type.empty()) {
                        oss << " : " << instr.type;
                    }
                    oss << "\n";
                }
            }
        }
        return oss.str();
    }

    std::string DumpCfg(const Module &module) {
        std::ostringstream oss;
        oss << "cfg module " << module.module_path << "\n";
        for (const auto &function: module.functions) {
            oss << "function " << function.name << "\n";
            for (const auto &block: function.blocks) {
                oss << "  block " << block.label << " -> ";
                if (block.instructions.empty()) {
                    oss << "<invalid>\n";
                    continue;
                }
                const auto &term = block.instructions.back();
                if (term.opcode == Opcode::Br && !term.operands.empty()) {
                    oss << term.operands[0];
                } else if (term.opcode == Opcode::CondBr && term.operands.size() >= 3) {
                    oss << term.operands[1] << ", " << term.operands[2];
                } else if (term.opcode == Opcode::Ret) {
                    oss << "<ret>";
                } else {
                    oss << "<term:" << ToString(term.opcode) << ">";
                }
                oss << "\n";
            }
        }
        return oss.str();
    }

} // namespace snow::sir
