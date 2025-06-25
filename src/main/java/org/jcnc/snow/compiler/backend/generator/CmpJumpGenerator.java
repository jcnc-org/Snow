package org.jcnc.snow.compiler.backend.generator;

import org.jcnc.snow.compiler.backend.util.IROpCodeMapper;
import org.jcnc.snow.compiler.backend.util.OpHelper;
import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.ir.instruction.IRCompareJumpInstruction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.Map;

/**
 * <b>条件比较跳转指令生成器</b>
 * <p>
 * 该类实现了 {@link InstructionGenerator} 接口，
 * 负责将 IR 中的 {@link IRCompareJumpInstruction}（条件比较并跳转指令）
 * 转换为目标虚拟机（VM）可执行的指令序列。
 * </p>
 *
 * <b>主要功能</b>
 * <ul>
 *     <li>根据 IR 比较指令左右操作数的类型，自动进行类型提升与转换</li>
 *     <li>生成相应的 VM 加载、类型转换、比较与跳转指令</li>
 *     <li>保证指令的类型前缀与操作数类型一致，提升兼容性与正确性</li>
 * </ul>
 */
public class CmpJumpGenerator implements InstructionGenerator<IRCompareJumpInstruction> {

    /**
     * 返回本生成器支持的 IR 指令类型。
     *
     * @return IRCompareJumpInstruction 的类对象
     */
    @Override
    public Class<IRCompareJumpInstruction> supportedClass() {
        return IRCompareJumpInstruction.class;
    }

    /**
     * <b>类型宽度优先级</b>：D > F > L > I > S > B
     * <ul>
     *     <li>D（double）：6</li>
     *     <li>F（float）：5</li>
     *     <li>L（long）：4</li>
     *     <li>I（int）：3</li>
     *     <li>S（short）：2</li>
     *     <li>B（byte）：1</li>
     *     <li>未识别类型：0</li>
     * </ul>
     *
     * @param p 类型标记字符
     * @return 优先级数值（越大类型越宽）
     */
    private static int rank(char p) {
        return switch (p) {
            case 'D' -> 6;
            case 'F' -> 5;
            case 'L' -> 4;
            case 'I' -> 3;
            case 'S' -> 2;
            case 'B' -> 1;
            default  -> 0;
        };
    }

    /**
     * 返回更“宽”的公共类型（即优先级高的类型）。
     *
     * @param a 类型标记字符 1
     * @param b 类型标记字符 2
     * @return 宽度更高的类型标记字符
     */
    private static char promote(char a, char b) {
        return rank(a) >= rank(b) ? a : b;
    }

    /**
     * 单字符类型标记转字符串。
     *
     * @param p 类型标记字符
     * @return 类型字符串
     */
    private static String str(char p) {
        return String.valueOf(p);
    }

    /**
     * 获取 {@code from → to} 的类型转换指令名（如不需转换则返回 {@code null}）。
     * <p>
     * 仅覆盖目前常见的整数与浮点类型提升与转换，后续有新类型可补充。
     * </p>
     *
     * @param from 源类型标记字符
     * @param to   目标类型标记字符
     * @return 转换指令名，如“L2I”；无转换返回 {@code null}
     */
    private static String convert(char from, char to) {
        if (from == to) return null;
        return switch ("" + from + to) {
            case "IL" -> "I2L";
            case "ID" -> "I2D";
            case "IF" -> "I2F";
            case "LI" -> "L2I";
            case "LD" -> "L2D";
            case "LF" -> "L2F";
            case "FI" -> "F2I";
            case "FL" -> "F2L";
            case "FD" -> "F2D";
            case "DI" -> "D2I";
            case "DL" -> "D2L";
            case "DF" -> "D2F";
            case "SI" -> "S2I";
            case "BI" -> "B2I";
            default   -> null;
        };
    }

    /**
     * 生成 IR 条件比较跳转指令的 VM 指令序列。
     * <ol>
     *     <li>确定左右操作数的槽位及静态类型</li>
     *     <li>加载并按需类型提升转换</li>
     *     <li>根据公共类型调整比较指令前缀</li>
     *     <li>发出最终比较和跳转指令</li>
     * </ol>
     *
     * @param ins       IR 条件比较跳转指令
     * @param out       VMProgramBuilder：用于发出 VM 指令
     * @param slotMap   虚拟寄存器到 VM 槽位的映射表
     * @param currentFn 当前处理的函数名（调试用，当前未使用）
     */
    @Override
    public void generate(IRCompareJumpInstruction  ins,
                         VMProgramBuilder          out,
                         Map<IRVirtualRegister,Integer> slotMap,
                         String                    currentFn) {

        // 1. 获取左右操作数的槽位与静态类型
        int  leftSlot  = slotMap.get(ins.left());
        int  rightSlot = slotMap.get(ins.right());
        char lType     = out.getSlotType(leftSlot);   // 若未登记则默认 'I'
        char rType     = out.getSlotType(rightSlot);
        char tType     = promote(lType, rType);        // 公共类型提升

        // 2. 加载左右操作数并按需类型转换
        // 左操作数
        out.emit(OpHelper.opcode(str(lType) + "_LOAD") + " " + leftSlot);
        String cvt = convert(lType, tType);
        if (cvt != null) {
            out.emit(OpHelper.opcode(cvt));
        }

        // 右操作数
        out.emit(OpHelper.opcode(str(rType) + "_LOAD") + " " + rightSlot);
        cvt = convert(rType, tType);
        if (cvt != null) {
            out.emit(OpHelper.opcode(cvt));
        }

        // 3. 选择正确的比较指令前缀
        String cmpOp = IROpCodeMapper.toVMOp(ins.op());
        /*
         * 指令前缀（如 int 类型要用 IC_*, long 类型要用 LC_*）
         */
        if (tType == 'I' && cmpOp.startsWith("LC_")) {
            cmpOp = "IC_" + cmpOp.substring(3);
        } else if (tType == 'L' && cmpOp.startsWith("IC_")) {
            cmpOp = "LC_" + cmpOp.substring(3);
        }

        // 4. 发出比较与跳转指令
        out.emitBranch(OpHelper.opcode(cmpOp), ins.label());
    }
}
