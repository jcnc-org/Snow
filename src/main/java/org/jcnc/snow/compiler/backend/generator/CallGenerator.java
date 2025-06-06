package org.jcnc.snow.compiler.backend.generator;

import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.backend.util.OpHelper;
import org.jcnc.snow.compiler.ir.instruction.CallInstruction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.Map;

/**
 * 函数调用指令生成器。
 * <p>
 * 该类实现了函数调用（CallInstruction）的指令翻译逻辑，
 * 负责将 IR 层的函数调用转换为虚拟机可执行的低级指令。
 */
public class CallGenerator implements InstructionGenerator<CallInstruction> {

    /**
     * 返回本指令生成器支持的 IR 指令类型（CallInstruction）。
     *
     * @return 指令类型的 Class 对象
     */
    @Override
    public Class<CallInstruction> supportedClass() {
        return CallInstruction.class;
    }

    /**
     * 生成函数调用相关的虚拟机指令。
     * <p>
     * 步骤如下：
     * <ol>
     *     <li>预测返回值类型（采用首个实参槽的类型作为近似）</li>
     *     <li>为每个参数根据实际类型发出加载指令</li>
     *     <li>生成 CALL 调用指令</li>
     *     <li>将返回值存储到目标槽，并记录类型信息</li>
     * </ol>
     *
     * @param ins        待翻译的 CallInstruction 指令对象
     * @param out        指令输出与类型槽管理器
     * @param slotMap    IR 寄存器到槽号的映射
     * @param currentFn  当前函数名（未用，可用于递归/闭包等复杂场景）
     */
    @Override
    public void generate(CallInstruction ins,
                         VMProgramBuilder out,
                         Map<IRVirtualRegister, Integer> slotMap,
                         String currentFn) {

        // —— 1. 预测返回值类型（用首个实参槽类型作为近似推断） ——
        char retType = 'I'; // 默认整型
        if (!ins.getArguments().isEmpty()) {
            int firstSlot = slotMap.get((IRVirtualRegister) ins.getArguments().getFirst());
            retType = out.getSlotType(firstSlot); // 获取槽位实际类型
            if (retType == '\0') retType = 'I';   // 默认整型
        }

        // —— 2. 按真实类型加载每个参数到虚拟机操作栈 ——
        for (var arg : ins.getArguments()) {
            int slotId = slotMap.get((IRVirtualRegister) arg); // 获取参数槽号
            char t = out.getSlotType(slotId);                  // 获取参数类型
            if (t == '\0') t = 'I';                            // 类型未知时默认整型
            // 生成类型相关的加载指令，如 I_LOAD、F_LOAD 等
            out.emit(OpHelper.opcode(String.valueOf(t) + "_LOAD") + " " + slotId);
        }

        // —— 3. 生成 CALL 调用指令 ——
        out.emitCall(ins.getFunctionName(), ins.getArguments().size());

        // —— 4. 将返回值存入目标槽，并记录槽的类型 ——
        int destSlot = slotMap.get(ins.getDest()); // 目标寄存器槽
        out.emit(OpHelper.opcode(String.valueOf(retType) + "_STORE") + " " + destSlot);
        out.setSlotType(destSlot, retType);        // 标记返回值类型
    }
}
