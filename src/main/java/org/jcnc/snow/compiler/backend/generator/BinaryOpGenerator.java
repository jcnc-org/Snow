package org.jcnc.snow.compiler.backend.generator;

import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.backend.util.OpHelper;
import org.jcnc.snow.compiler.ir.instruction.BinaryOperationInstruction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.Map;

/**
 * 二元运算指令生成器
 * 支持二元运算指令的自动类型提升。
 * <p>类型提升优先级为：D > F > L > I > S > B</p>
 */
public class BinaryOpGenerator implements InstructionGenerator<BinaryOperationInstruction> {

    /* ---------- 类型优先级工具 ---------- */

    /**
     * 返回类型前缀的优先级数值。数值越大，类型“越宽”。
     * D: 6, F: 5, L: 4, I: 3, S: 2, B: 1
     */
    private static int rank(char p) {
        return switch (p) {
            case 'D' -> 6;
            case 'F' -> 5;
            case 'L' -> 4;
            case 'I' -> 3;
            case 'S' -> 2;
            case 'B' -> 1;
            default -> 0;
        };
    }

    /**
     * 返回a和b中优先级更高的类型前缀（即类型提升结果）。
     */
    private static char promote(char a, char b) {
        return rank(a) >= rank(b) ? a : b;
    }

    /**
     * 类型前缀转字符串，方便拼接。
     */
    private static String str(char p) {
        return String.valueOf(p);
    }

    /* ---------- 类型转换指令工具 ---------- */

    /**
     * 根据源类型和目标类型前缀，返回相应的类型转换指令助记符。
     *
     * @param from 源类型前缀
     * @param to   目标类型前缀
     * @return 转换指令字符串，如 "I2L" 或 "F2D"，若无需转换则返回null
     */
    private static String convert(char from, char to) {
        if (from == to) return null;  // 类型一致，无需转换
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
            default -> null;   // 其它组合暂未用到
        };
    }

    /**
     * 返回本生成器支持的指令类型，即 BinaryOperationInstruction。
     */
    @Override
    public Class<BinaryOperationInstruction> supportedClass() {
        return BinaryOperationInstruction.class;
    }

    /**
     * 生成二元运算的虚拟机指令，实现自动类型提升和必要的类型转换。
     *
     * @param ins       当前二元运算IR指令
     * @param out       虚拟机程序构建器
     * @param slotMap   IR虚拟寄存器到实际槽位编号的映射
     * @param currentFn 当前函数名
     */
    @Override
    public void generate(BinaryOperationInstruction ins,
                         VMProgramBuilder out,
                         Map<IRVirtualRegister, Integer> slotMap,
                         String currentFn) {

        /* ------- 1. 获取左右操作数的槽位编号和类型 ------- */
        int lSlot = slotMap.get((IRVirtualRegister) ins.operands().get(0)); // 左操作数槽位
        int rSlot = slotMap.get((IRVirtualRegister) ins.operands().get(1)); // 右操作数槽位
        int dSlot = slotMap.get(ins.dest());                                // 目标槽位
        char lType = out.getSlotType(lSlot);  // 左操作数类型前缀
        char rType = out.getSlotType(rSlot);  // 右操作数类型前缀

        // 类型提升，确定本次二元运算的目标类型（优先级较高的那一个）
        char tType = promote(lType, rType);
        String tPref = str(tType);  // 用于拼接指令字符串

        /* ------- 2. 加载左操作数，并自动进行类型转换（如有必要） ------- */
        out.emit(OpHelper.opcode(str(lType) + "_LOAD") + " " + lSlot); // LOAD指令
        String cvt = convert(lType, tType);                            // 如需类型提升
        if (cvt != null) out.emit(OpHelper.opcode(cvt));               // 插入类型转换指令

        /* ------- 3. 加载右操作数，并自动进行类型转换（如有必要） ------- */
        out.emit(OpHelper.opcode(str(rType) + "_LOAD") + " " + rSlot); // LOAD指令
        cvt = convert(rType, tType);                                   // 如需类型提升
        if (cvt != null) out.emit(OpHelper.opcode(cvt));               // 插入类型转换指令

        /* ------- 4. 生成具体的二元运算指令 ------- */
        // 获取IR指令中的操作名（如ADD、SUB、MUL等，去掉结尾的"_"后缀）
        String opName = ins.op().name().split("_")[0];
        // 例如生成 "I_ADD", "D_MUL" 等虚拟机指令
        out.emit(OpHelper.opcode(tPref + "_" + opName));

        /* ------- 5. 结果存入目标槽位，并更新槽位类型 ------- */
        out.emit(OpHelper.opcode(tPref + "_STORE") + " " + dSlot);
        out.setSlotType(dSlot, tType); // 记录运算结果的类型前缀，便于后续指令正确处理
    }
}
