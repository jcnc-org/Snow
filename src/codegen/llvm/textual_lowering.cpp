// Module: Textual LLVM IR lowering from validated Snow SIR.
// Contract: lowering output is deterministic for equal SIR and target options.

#include "textual_lowering_internal.h"

#include <algorithm>
#include <cctype>
#include <optional>
#include <sstream>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <utility>
#include <vector>

namespace snow::codegen::llvm_backend {

    namespace {

        bool IsLowerableOpcode(const snow::sir::Opcode opcode) {
            switch (opcode) {
                case snow::sir::Opcode::Add:
                case snow::sir::Opcode::Sub:
                case snow::sir::Opcode::Mul:
                case snow::sir::Opcode::Div:
                case snow::sir::Opcode::Eq:
                case snow::sir::Opcode::Ne:
                case snow::sir::Opcode::Lt:
                case snow::sir::Opcode::Gt:
                case snow::sir::Opcode::Le:
                case snow::sir::Opcode::Ge:
                case snow::sir::Opcode::Drop:
                case snow::sir::Opcode::Alloc:
                case snow::sir::Opcode::Load:
                case snow::sir::Opcode::Store:
                case snow::sir::Opcode::Phi:
                case snow::sir::Opcode::Br:
                case snow::sir::Opcode::CondBr:
                case snow::sir::Opcode::Call:
                case snow::sir::Opcode::Ret:
                    return true;
                default:
                    return false;
            }
        }

        std::string ToLlvmTypeText(const std::string &snow_type) {
            if (snow_type == "i1" || snow_type == "bool") {
                return "i1";
            }
            if (snow_type == "i32") {
                return "i32";
            }
            if (snow_type == "i64") {
                return "i64";
            }
            if (snow_type == "f32") {
                return "float";
            }
            if (snow_type == "f64") {
                return "double";
            }
            if (snow_type == "ptr") {
                return "ptr";
            }
            return "i32";
        }

        std::string ZeroValueText(const std::string &llvm_type) {
            if (llvm_type == "float" || llvm_type == "double") {
                return "0.0";
            }
            if (llvm_type == "ptr") {
                return "null";
            }
            return "0";
        }

        bool IsIntegerLiteral(const std::string &text) {
            if (text.empty()) {
                return false;
            }
            std::size_t i = 0;
            if (text[0] == '+' || text[0] == '-') {
                i = 1;
                if (i >= text.size()) {
                    return false;
                }
            }
            for (; i < text.size(); ++i) {
                if (!std::isdigit(static_cast<unsigned char>(text[i]))) {
                    return false;
                }
            }
            return true;
        }

        std::string NormalizeOperand(const std::string &operand) {
            if (operand.empty()) {
                return "0";
            }
            if (operand[0] == '%' || IsIntegerLiteral(operand)) {
                return operand;
            }
            if (operand == "true") {
                return "1";
            }
            if (operand == "false") {
                return "0";
            }
            if (operand == "null") {
                return "null";
            }
            return "0";
        }

        std::string InferOperandLlvmTypeText(const std::string &operand,
                                             const std::unordered_map<std::string, std::string> &value_types) {
            if (operand == "true" || operand == "false") {
                return "i1";
            }
            if (operand == "null") {
                return "ptr";
            }
            if (!operand.empty() && operand[0] == '%') {
                const auto it = value_types.find(operand);
                if (it != value_types.end()) {
                    return ToLlvmTypeText(it->second);
                }
                return "i32";
            }
            if (IsIntegerLiteral(operand)) {
                return "i32";
            }
            return "i32";
        }

        std::string ComparePredicateText(const snow::sir::Opcode opcode) {
            switch (opcode) {
                case snow::sir::Opcode::Eq:
                    return "eq";
                case snow::sir::Opcode::Ne:
                    return "ne";
                case snow::sir::Opcode::Lt:
                    return "slt";
                case snow::sir::Opcode::Gt:
                    return "sgt";
                case snow::sir::Opcode::Le:
                    return "sle";
                case snow::sir::Opcode::Ge:
                    return "sge";
                default:
                    return "eq";
            }
        }

        const snow::sir::Function *FindUserMain(const snow::sir::Module &module) {
            for (const auto &function: module.functions) {
                if (function.original_name == "main") {
                    return &function;
                }
            }
            return nullptr;
        }

        std::string LinkagePrefixText(const snow::sir::Linkage linkage) {
            switch (linkage) {
                case snow::sir::Linkage::External:
                    return "";
                case snow::sir::Linkage::Internal:
                    return "internal ";
                case snow::sir::Linkage::Private:
                    return "private ";
            }
            return "";
        }

    } // namespace

    std::optional<std::string> FirstUnsupportedOpcode(const snow::sir::Module &module) {
        for (const auto &function: module.functions) {
            for (const auto &block: function.blocks) {
                for (const auto &instr: block.instructions) {
                    if (!IsLowerableOpcode(instr.opcode)) {
                        return snow::sir::ToString(instr.opcode);
                    }
                }
            }
        }
        return std::nullopt;
    }

    std::string LowerTextual(const snow::sir::Module &module, const TargetConfig &target,
                             const snow::passes::OptLevel opt_level) {
        std::ostringstream oss;
        oss << "; snow llvm ir (textual lowering)\n";
        oss << "target triple = \"" << target.triple << "\"\n";
        oss << "; entry-wrapper = " << (target.executable_entry_wrapper ? "enabled" : "disabled") << "\n";
        oss << "; opt-level = " << (opt_level == snow::passes::OptLevel::O0 ? "O0" : "O2") << "\n\n";

        std::unordered_set<std::string> defined_functions;
        for (const auto &function: module.functions) {
            defined_functions.insert(function.name);
        }

        std::vector<snow::sir::ExternalFunction> externals = module.external_functions;
        std::sort(externals.begin(), externals.end(),
                  [](const auto &lhs, const auto &rhs) { return lhs.name < rhs.name; });
        for (const auto &external: externals) {
            if (external.name.empty() || defined_functions.contains(external.name)) {
                continue;
            }
            oss << "declare " << ToLlvmTypeText(external.return_type.empty() ? "i32" : external.return_type) << " @"
                << external.name << "(";
            for (std::size_t i = 0; i < external.param_types.size(); ++i) {
                if (i > 0) {
                    oss << ", ";
                }
                oss << ToLlvmTypeText(external.param_types[i]);
            }
            oss << ")\n";
        }

        bool needs_runtime_drop = false;
        for (const auto &function: module.functions) {
            for (const auto &block: function.blocks) {
                for (const auto &instr: block.instructions) {
                    if (instr.opcode == snow::sir::Opcode::Drop) {
                        needs_runtime_drop = true;
                        break;
                    }
                }
                if (needs_runtime_drop) {
                    break;
                }
            }
            if (needs_runtime_drop) {
                break;
            }
        }
        if (needs_runtime_drop) {
            oss << "define linkonce_odr void @snow_runtime_drop(ptr %value) {\n";
            oss << "entry:\n";
            oss << "  ret void\n";
            oss << "}\n";
        }

        if (!externals.empty() || needs_runtime_drop) {
            oss << "\n";
        }

        for (const auto &function: module.functions) {
            const std::string ret_ty = ToLlvmTypeText(function.return_type);
            std::unordered_map<std::string, std::string> value_types;
            std::unordered_map<std::string, std::string> pointer_element_types;
            oss << "define " << LinkagePrefixText(function.linkage) << ret_ty << " @" << function.name << "(";
            for (std::size_t i = 0; i < function.params.size(); ++i) {
                if (i > 0) {
                    oss << ", ";
                }
                const auto &param = function.params[i];
                oss << ToLlvmTypeText(param.type) << " %" << param.name;
                value_types["%" + param.name] = param.type;
            }
            oss << ") {\n";
            bool emitted_ret = false;
            bool emitted_block = false;

            for (const auto &block: function.blocks) {
                emitted_block = true;
                oss << block.label << ":\n";
                for (const auto &instr: block.instructions) {
                    switch (instr.opcode) {
                        case snow::sir::Opcode::Add:
                        case snow::sir::Opcode::Sub:
                        case snow::sir::Opcode::Mul:
                        case snow::sir::Opcode::Div: {
                            if (!instr.result.has_value() || instr.operands.size() != 2) {
                                oss << "  ; malformed arithmetic instruction\n";
                                break;
                            }
                            const std::string op_ty = ToLlvmTypeText(instr.type);
                            const std::string lhs = NormalizeOperand(instr.operands[0]);
                            const std::string rhs = NormalizeOperand(instr.operands[1]);
                            std::string op;
                            switch (instr.opcode) {
                                case snow::sir::Opcode::Add:
                                    op = "add";
                                    break;
                                case snow::sir::Opcode::Sub:
                                    op = "sub";
                                    break;
                                case snow::sir::Opcode::Mul:
                                    op = "mul";
                                    break;
                                case snow::sir::Opcode::Div:
                                    op = "sdiv";
                                    break;
                                default:
                                    op = "add";
                                    break;
                            }
                            oss << "  " << instr.result.value() << " = " << op << " " << op_ty << " " << lhs << ", "
                                << rhs << "\n";
                            break;
                        }

                        case snow::sir::Opcode::Eq:
                        case snow::sir::Opcode::Ne:
                        case snow::sir::Opcode::Lt:
                        case snow::sir::Opcode::Gt:
                        case snow::sir::Opcode::Le:
                        case snow::sir::Opcode::Ge: {
                            if (!instr.result.has_value() || instr.operands.size() != 2) {
                                oss << "  ; malformed compare instruction\n";
                                break;
                            }
                            const std::string lhs = NormalizeOperand(instr.operands[0]);
                            const std::string rhs = NormalizeOperand(instr.operands[1]);
                            const std::string cmp_ty = InferOperandLlvmTypeText(instr.operands[0], value_types);
                            oss << "  " << instr.result.value() << " = icmp " << ComparePredicateText(instr.opcode)
                                << " " << cmp_ty << " " << lhs << ", " << rhs << "\n";
                            break;
                        }

                        case snow::sir::Opcode::Drop:
                            if (!instr.operands.empty()) {
                                oss << "  call void @snow_runtime_drop(ptr " << NormalizeOperand(instr.operands[0])
                                    << ")\n";
                            } else {
                                oss << "  call void @snow_runtime_drop(ptr null)\n";
                            }
                            break;

                        case snow::sir::Opcode::Alloc: {
                            if (!instr.result.has_value()) {
                                oss << "  ; malformed alloc instruction\n";
                                break;
                            }
                            const std::string element_type = ToLlvmTypeText(
                                    (instr.operands.empty() || instr.operands[0].empty()) ? "i32" : instr.operands[0]);
                            oss << "  " << instr.result.value() << " = alloca " << element_type << "\n";
                            pointer_element_types[instr.result.value()] = element_type;
                            break;
                        }

                        case snow::sir::Opcode::Load: {
                            if (!instr.result.has_value() || instr.operands.size() != 1) {
                                oss << "  ; malformed load instruction\n";
                                break;
                            }
                            const std::string ptr = NormalizeOperand(instr.operands[0]);
                            const std::string element_type =
                                    pointer_element_types.contains(ptr)
                                            ? pointer_element_types[ptr]
                                            : ToLlvmTypeText(instr.type.empty() ? "i32" : instr.type);
                            oss << "  " << instr.result.value() << " = load " << element_type << ", ptr " << ptr
                                << "\n";
                            break;
                        }

                        case snow::sir::Opcode::Store: {
                            if (instr.operands.size() != 2) {
                                oss << "  ; malformed store instruction\n";
                                break;
                            }
                            const std::string value = NormalizeOperand(instr.operands[0]);
                            const std::string ptr = NormalizeOperand(instr.operands[1]);
                            const std::string element_type =
                                    pointer_element_types.contains(ptr)
                                            ? pointer_element_types[ptr]
                                            : InferOperandLlvmTypeText(instr.operands[0], value_types);
                            oss << "  store " << element_type << " " << value << ", ptr " << ptr << "\n";
                            break;
                        }

                        case snow::sir::Opcode::Phi: {
                            if (!instr.result.has_value() || instr.operands.size() < 4 ||
                                (instr.operands.size() % 2) != 0) {
                                oss << "  ; malformed phi instruction\n";
                                break;
                            }
                            const std::string phi_ty = ToLlvmTypeText(instr.type.empty() ? "i32" : instr.type);
                            oss << "  " << instr.result.value() << " = phi " << phi_ty << " ";
                            for (std::size_t i = 0; i + 1 < instr.operands.size(); i += 2) {
                                if (i > 0) {
                                    oss << ", ";
                                }
                                oss << "[ " << NormalizeOperand(instr.operands[i]) << ", %" << instr.operands[i + 1]
                                    << " ]";
                            }
                            oss << "\n";
                            break;
                        }

                        case snow::sir::Opcode::Br:
                            if (instr.operands.size() != 1) {
                                oss << "  ; malformed br instruction\n";
                                break;
                            }
                            oss << "  br label %" << instr.operands[0] << "\n";
                            break;

                        case snow::sir::Opcode::CondBr:
                            if (instr.operands.size() != 3) {
                                oss << "  ; malformed cond_br instruction\n";
                                break;
                            }
                            oss << "  br i1 " << NormalizeOperand(instr.operands[0]) << ", label %" << instr.operands[1]
                                << ", label %" << instr.operands[2] << "\n";
                            break;

                        case snow::sir::Opcode::Call: {
                            if (!instr.result.has_value() || instr.operands.empty()) {
                                oss << "  ; malformed call instruction\n";
                                break;
                            }
                            const std::string call_ret_ty = ToLlvmTypeText(instr.type.empty() ? "i32" : instr.type);
                            const std::string callee = instr.operands[0];
                            oss << "  " << instr.result.value() << " = call " << call_ret_ty << " @" << callee << "(";
                            for (std::size_t i = 1; i < instr.operands.size(); ++i) {
                                if (i > 1) {
                                    oss << ", ";
                                }
                                const std::string arg_ty = InferOperandLlvmTypeText(instr.operands[i], value_types);
                                oss << arg_ty << " " << NormalizeOperand(instr.operands[i]);
                            }
                            oss << ")\n";
                            break;
                        }

                        case snow::sir::Opcode::Ret: {
                            const std::string value = instr.operands.empty() ? ZeroValueText(ret_ty)
                                                                             : NormalizeOperand(instr.operands.front());
                            oss << "  ret " << ret_ty << " " << value << "\n";
                            emitted_ret = true;
                            break;
                        }

                        default:
                            oss << "  ; unsupported instruction: " << snow::sir::ToString(instr.opcode) << "\n";
                            break;
                    }

                    if (instr.result.has_value() && !instr.type.empty()) {
                        value_types[instr.result.value()] = instr.type;
                    }
                }
            }

            if (!emitted_block) {
                oss << "entry:\n";
            }
            if (!emitted_ret) {
                oss << "  ret " << ret_ty << " " << ZeroValueText(ret_ty) << "\n";
            }
            oss << "}\n\n";
        }

        if (target.executable_entry_wrapper) {
            const snow::sir::Function *user_main = FindUserMain(module);
            if (user_main != nullptr) {
                oss << "define linkonce_odr void @snow_runtime_init() {\n";
                oss << "entry:\n";
                oss << "  ret void\n";
                oss << "}\n\n";
                oss << "define linkonce_odr void @snow_runtime_shutdown() {\n";
                oss << "entry:\n";
                oss << "  ret void\n";
                oss << "}\n\n";
                oss << "define i32 @snow_runtime_start(ptr %user_main) {\n";
                oss << "entry:\n";
                oss << "  call void @snow_runtime_init()\n";
                oss << "  %0 = call i32 %user_main()\n";
                oss << "  call void @snow_runtime_shutdown()\n";
                oss << "  ret i32 %0\n";
                oss << "}\n\n";
                oss << "define i32 @main() {\n";
                oss << "entry:\n";
                oss << "  %1 = call i32 @snow_runtime_start(ptr @" << user_main->name << ")\n";
                oss << "  ret i32 %1\n";
                oss << "}\n\n";
            }
        }

        return oss.str();
    }

} // namespace snow::codegen::llvm_backend
