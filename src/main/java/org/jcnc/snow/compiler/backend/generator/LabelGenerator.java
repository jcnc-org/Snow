package org.jcnc.snow.compiler.backend.generator;

import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.ir.instruction.IRLabelInstruction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.Map;

/**
 * 标签指令生成器
 * <p>
 * 本类实现了 {@link InstructionGenerator} 接口，用于将 IR 层的标签指令翻译为
 * 虚拟机（VM）中的标签标记。标签一般用于跳转或分支目的地的定位。
 * </p>
 */
public class LabelGenerator implements InstructionGenerator<IRLabelInstruction> {

    /**
     * 返回本生成器所支持的 IR 指令类型。
     *
     * @return {@link IRLabelInstruction} 的类对象
     */
    @Override
    public Class<IRLabelInstruction> supportedClass() {
        return IRLabelInstruction.class;
    }

    /**
     * 生成对应的虚拟机标签指令。
     *
     * @param ins       当前 IR 标签指令
     * @param out       虚拟机程序构建器，用于输出指令
     * @param slotMap   虚拟寄存器与槽号的映射表（标签指令未使用此参数）
     * @param currentFn 当前函数名称（用于调试或作用域标识，可选）
     */
    @Override
    public void generate(IRLabelInstruction ins,
                         VMProgramBuilder out,
                         Map<IRVirtualRegister, Integer> slotMap,
                         String currentFn) {
        // 生成标签（如 "label_name:"），用于虚拟机指令流中的位置标记
        out.emit(ins.name() + ":");
    }
}
