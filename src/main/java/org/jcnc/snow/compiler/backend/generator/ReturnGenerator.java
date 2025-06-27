package org.jcnc.snow.compiler.backend.generator;

import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.backend.utils.OpHelper;
import org.jcnc.snow.compiler.ir.instruction.ReturnInstruction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.Map;

/**
 * 返回指令生成器
 * <p>
 * 本类实现了 {@link InstructionGenerator} 接口，用于将 IR 中的函数返回指令翻译为
 * 虚拟机可执行的返回（RET/HALT）相关指令。支持有返回值和无返回值两种情况。
 * </p>
 */
public class ReturnGenerator implements InstructionGenerator<ReturnInstruction> {

    /**
     * 返回该生成器支持的 IR 指令类型。
     *
     * @return {@link ReturnInstruction} 的类对象
     */
    @Override
    public Class<ReturnInstruction> supportedClass() {
        return ReturnInstruction.class;
    }

    /**
     * 生成对应的虚拟机返回指令。
     *
     * @param ins       当前 IR 返回指令
     * @param out       虚拟机程序构建器，用于输出 VM 指令
     * @param slotMap   虚拟寄存器到槽号的映射表
     * @param currentFn 当前函数名称（用于判断是否为主函数 main）
     */
    @Override
    public void generate(ReturnInstruction ins,
                         VMProgramBuilder out,
                         Map<IRVirtualRegister, Integer> slotMap,
                         String currentFn) {
        // 若存在返回值，先将返回值加载到虚拟机栈顶
        if (ins.value() != null) {
            int slotId = slotMap.get(ins.value());
            // 根据之前记录的槽类型前缀（I/L/S/B/D/F）选 LOAD
            char prefix = out.getSlotType(slotId);
            String loadOp = prefix + "_LOAD";
            out.emit(OpHelper.opcode(loadOp) + " " + slotId);
        }
        // 主函数返回使用 HALT，普通函数返回使用 RET
        String code = "main".equals(currentFn) ? "HALT" : "RET";
        out.emit(OpHelper.opcode(code));
    }
}
