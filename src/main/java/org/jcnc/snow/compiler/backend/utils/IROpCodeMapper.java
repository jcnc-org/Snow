package org.jcnc.snow.compiler.backend.utils;

import org.jcnc.snow.compiler.ir.core.IROpCode;

import java.util.EnumMap;
import java.util.Map;

/**
 * IR 操作码与虚拟机指令名映射工具类。
 * <p>
 * 本类用于将 IR 层的操作码（{@link IROpCode}）映射为目标虚拟机的指令名（{@code String}）。
 * 该工具类在编译器后端阶段提供指令名转换功能，不处理参数或操作数，仅负责纯映射。
 * </p>
 * <p>
 * 若需扩展新的操作码或 VM 指令，应在本类中统一维护映射关系。
 * </p>
 */
public final class IROpCodeMapper {

    /**
     * IR 操作码到 VM 指令名的静态映射表。
     * <ul>
     *     <li>键：IR 操作码（{@link IROpCode} 枚举项）</li>
     *     <li>值：对应虚拟机指令名（字符串）</li>
     * </ul>
     * 使用 {@link EnumMap}，查找和存储高效。
     */
    private static final Map<IROpCode, String> opcodeMap = new EnumMap<>(IROpCode.class);

    // 静态代码块，初始化所有 IR 操作码到 VM 指令名的映射关系
    static {
        opcodeMap.put(IROpCode.CONV_I32_TO_F32, "I2F");
        opcodeMap.put(IROpCode.CONV_I32_TO_D64, "I2D");

        opcodeMap.put(IROpCode.CONV_F32_TO_I32, "F2I");
        opcodeMap.put(IROpCode.CONV_D64_TO_I32, "D2I");

        opcodeMap.put(IROpCode.CONV_F32_TO_D64, "F2D");
        opcodeMap.put(IROpCode.CONV_D64_TO_F32, "D2F");

        // 整形8位算术运算映射
        opcodeMap.put(IROpCode.ADD_B8, "B_ADD");
        opcodeMap.put(IROpCode.SUB_B8, "B_SUB");
        opcodeMap.put(IROpCode.MUL_B8, "B_MUL");
        opcodeMap.put(IROpCode.DIV_B8, "B_DIV");
        opcodeMap.put(IROpCode.NEG_B8, "B_NEG");

        // 整形16位算术运算映射
        opcodeMap.put(IROpCode.ADD_S16, "S_ADD");
        opcodeMap.put(IROpCode.SUB_S16, "S_SUB");
        opcodeMap.put(IROpCode.MUL_S16, "S_MUL");
        opcodeMap.put(IROpCode.DIV_S16, "S_DIV");
        opcodeMap.put(IROpCode.NEG_S16, "S_NEG");

        // 整形32位算术运算映射
        opcodeMap.put(IROpCode.ADD_I32, "I_ADD");
        opcodeMap.put(IROpCode.SUB_I32, "I_SUB");
        opcodeMap.put(IROpCode.MUL_I32, "I_MUL");
        opcodeMap.put(IROpCode.DIV_I32, "I_DIV");
        opcodeMap.put(IROpCode.NEG_I32, "I_NEG");

        // 整形64位算术运算映射
        opcodeMap.put(IROpCode.ADD_L64, "L_ADD");
        opcodeMap.put(IROpCode.SUB_L64, "L_SUB");
        opcodeMap.put(IROpCode.MUL_L64, "L_MUL");
        opcodeMap.put(IROpCode.DIV_L64, "L_DIV");
        opcodeMap.put(IROpCode.NEG_L64, "L_NEG");

        // --- 32-bit floating point ---

        opcodeMap.put(IROpCode.ADD_F32, "F_ADD");
        opcodeMap.put(IROpCode.SUB_F32, "F_SUB");
        opcodeMap.put(IROpCode.MUL_F32, "F_MUL");
        opcodeMap.put(IROpCode.DIV_F32, "F_DIV");
        opcodeMap.put(IROpCode.NEG_F32, "F_NEG");

        // --- 64-bit floating point ---
        opcodeMap.put(IROpCode.ADD_D64, "D_ADD");
        opcodeMap.put(IROpCode.SUB_D64, "D_SUB");
        opcodeMap.put(IROpCode.MUL_D64, "D_MUL");
        opcodeMap.put(IROpCode.DIV_D64, "D_DIV");
        opcodeMap.put(IROpCode.NEG_D64, "D_NEG");

        // 比较运算映射
        // 整形32位比较运算映射
        opcodeMap.put(IROpCode.CMP_IEQ, "IC_E");   // 相等
        opcodeMap.put(IROpCode.CMP_INE, "IC_NE");   // 不等
        opcodeMap.put(IROpCode.CMP_ILT, "IC_L");    // 小于
        opcodeMap.put(IROpCode.CMP_IGT, "IC_G");    // 大于
        opcodeMap.put(IROpCode.CMP_ILE, "IC_LE");   // 小于等于
        opcodeMap.put(IROpCode.CMP_IGE, "IC_GE");   // 大于等于

        // 整形64位比较运算映射
        opcodeMap.put(IROpCode.CMP_LEQ, "LC_E");   // 相等
        opcodeMap.put(IROpCode.CMP_LNE, "LC_NE");   // 不等
        opcodeMap.put(IROpCode.CMP_LLT, "LC_L");    // 小于
        opcodeMap.put(IROpCode.CMP_LGT, "LC_G");    // 大于
        opcodeMap.put(IROpCode.CMP_LLE, "LC_LE");   // 小于等于
        opcodeMap.put(IROpCode.CMP_LGE, "LC_GE");   // 大于等于

        // 加载与存储
        opcodeMap.put(IROpCode.LOAD, "I_LOAD");  // 加载
        opcodeMap.put(IROpCode.STORE, "I_STORE"); // 存储
        opcodeMap.put(IROpCode.CONST, "I_PUSH");  // 常量入栈

        // 跳转与标签
        opcodeMap.put(IROpCode.JUMP, "JMP");     // 无条件跳转
        opcodeMap.put(IROpCode.LABEL, "LABEL");   // 标签

        // 函数相关
        opcodeMap.put(IROpCode.CALL, "CALL");    // 调用
        opcodeMap.put(IROpCode.RET, "RET");     // 返回
    }

    /**
     * 工具类私有构造，禁止实例化。
     */
    private IROpCodeMapper() {
        // 禁止实例化
    }

    /**
     * 根据指定 IR 操作码获取对应的虚拟机指令名。
     *
     * @param irOp 需转换的 IR 操作码（{@link IROpCode} 枚举值）
     * @return 对应的虚拟机指令名（字符串）
     * @throws IllegalArgumentException 若 {@code irOp} 未定义映射关系，抛出异常
     */
    public static String toVMOp(IROpCode irOp) {
        String vmCode = opcodeMap.get(irOp);
        if (vmCode == null) {
            throw new IllegalArgumentException("未映射的 IR 操作码: " + irOp);
        }
        return vmCode;
    }
}
