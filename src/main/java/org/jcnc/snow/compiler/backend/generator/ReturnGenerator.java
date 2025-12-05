package org.jcnc.snow.compiler.backend.generator;

import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.backend.utils.OpHelper;
import org.jcnc.snow.compiler.backend.utils.TypePromoteUtils;
import org.jcnc.snow.compiler.ir.common.GlobalFunctionTable;
import org.jcnc.snow.compiler.ir.instruction.ReturnInstruction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.Locale;
import java.util.Map;

/**
 * 返回指令生成器。
 *
 * <p>
 * 本类实现 {@link InstructionGenerator} 接口，用于将 IR 中的函数返回指令翻译为
 * 虚拟机可执行的返回类指令（RET/HALT），支持：
 * </p>
 * <ul>
 *     <li>带返回值的函数返回处理</li>
 *     <li>无返回值函数返回处理</li>
 *     <li>主函数使用 HALT 终止虚拟机</li>
 *     <li>根据返回值类型自动执行必要的数值转换</li>
 * </ul>
 */
public class ReturnGenerator implements InstructionGenerator<ReturnInstruction> {

    /**
     * 获取该生成器所支持的 IR 指令类型。
     *
     * @return {@link ReturnInstruction} 类型对象
     */
    @Override
    public Class<ReturnInstruction> supportedClass() {
        return ReturnInstruction.class;
    }

    /**
     * 生成虚拟机返回指令。
     *
     * <p>处理流程：</p>
     * <ol>
     *     <li>若存在返回值，则从槽位加载返回值到栈顶</li>
     *     <li>必要时根据函数声明的返回类型执行数值类型转换</li>
     *     <li>区分主函数（HALT）与普通函数（RET）输出对应指令</li>
     * </ol>
     *
     * @param ins       IR 中的返回指令对象
     * @param out       虚拟机程序构建器，用于生成 VM 指令
     * @param slotMap   虚拟寄存器到槽号的映射
     * @param currentFn 当前函数名称，用于判断是否为主函数
     */
    @Override
    public void generate(ReturnInstruction ins,
                         VMProgramBuilder out,
                         Map<IRVirtualRegister, Integer> slotMap,
                         String currentFn) {

        // 若有返回值，则加载返回值
        if (ins.value() != null) {
            int slotId = slotMap.get(ins.value());

            // 根据槽类型推导 LOAD 指令前缀（I/L/S/B/D/F）
            char srcPrefix = out.getSlotType(slotId);
            String loadOp = srcPrefix + "_LOAD";
            out.emit(OpHelper.opcode(loadOp) + " " + slotId);

            // 若返回值类型与函数声明不一致，则进行显式类型转换
            char declared = normalizeReturnPrefix(GlobalFunctionTable.getReturnType(currentFn));
            if (needsNumericConvert(srcPrefix, declared)) {
                String conv = TypePromoteUtils.convert(srcPrefix, declared);
                if (conv != null) {
                    out.emit(OpHelper.opcode(conv));
                }
            }
        }

        // 主函数终止虚拟机，普通函数执行返回
        String code = "main".equals(currentFn) ? "HALT" : "RET";
        out.emit(OpHelper.opcode(code));
    }

    /**
     * 将返回类型字符串转换为虚拟机类型前缀字符。
     *
     * @param retType 函数声明的返回类型名称
     * @return 虚拟机返回类型前缀
     */
    private char normalizeReturnPrefix(String retType) {
        if (retType == null || retType.isBlank()) {
            return 'V';
        }
        return switch (retType.toLowerCase(Locale.ROOT)) {
            case "byte" -> 'B';
            case "short" -> 'S';
            case "int", "integer", "bool", "boolean" -> 'I';
            case "long" -> 'L';
            case "float" -> 'F';
            case "double" -> 'D';
            case "void" -> 'V';
            default -> 'R';
        };
    }

    /**
     * 判断是否需要进行数值类型转换。
     *
     * @param from 源类型前缀
     * @param to   目标类型前缀
     * @return 若需要转换则返回 true，否则 false
     */
    private boolean needsNumericConvert(char from, char to) {
        if (from == to) {
            return false;
        }
        // 仅处理数值类型之间（B/S/I/L/F/D）的转换
        return switch (to) {
            case 'B', 'S', 'I', 'L', 'F', 'D' -> switch (from) {
                case 'B', 'S', 'I', 'L', 'F', 'D' -> true;
                default -> false;
            };
            default -> false;
        };
    }
}