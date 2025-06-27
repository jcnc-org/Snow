package org.jcnc.snow.compiler.backend.generator;

import org.jcnc.snow.compiler.backend.utils.OpHelper;
import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.ir.instruction.IRJumpInstruction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.Map;

/**
 * 无条件跳转指令生成器
 * <p>
 * 该类实现了 {@link InstructionGenerator} 接口，用于将 IR 中的无条件跳转指令
 * （即跳转到指定标签）翻译为虚拟机指令。
 * </p>
 */
public class JumpGenerator implements InstructionGenerator<IRJumpInstruction> {

    /**
     * 返回本生成器所支持的 IR 指令类型。
     *
     * @return {@link IRJumpInstruction} 的类对象
     */
    @Override
    public Class<IRJumpInstruction> supportedClass() {
        return IRJumpInstruction.class;
    }

    /**
     * 生成对应的虚拟机跳转指令。
     *
     * @param ins       当前 IR 跳转指令
     * @param out       虚拟机程序构建器，用于输出指令
     * @param slotMap   虚拟寄存器与槽号的映射表（本跳转指令未用到）
     * @param currentFn 当前函数名称（便于调试或作用域标识）
     */
    @Override
    public void generate(IRJumpInstruction ins,
                         VMProgramBuilder out,
                         Map<IRVirtualRegister, Integer> slotMap,
                         String currentFn) {
        // 生成无条件跳转到指定标签的虚拟机指令
        out.emitBranch(OpHelper.opcode("JUMP"), ins.label());
    }
}
