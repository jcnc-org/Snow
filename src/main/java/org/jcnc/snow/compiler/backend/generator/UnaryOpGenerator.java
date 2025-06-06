package org.jcnc.snow.compiler.backend.generator;

import org.jcnc.snow.compiler.backend.util.IROpCodeMapper;
import org.jcnc.snow.compiler.backend.util.OpHelper;
import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.ir.instruction.UnaryOperationInstruction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.Map;

/**
 * 一元运算指令生成器
 * <p>
 * 本类实现了 {@link InstructionGenerator} 接口，用于将 IR 中的一元运算指令
 * （如取负等）翻译为虚拟机（VM）可执行的指令序列。
 * </p>
 */
public class UnaryOpGenerator implements InstructionGenerator<UnaryOperationInstruction> {

    /**
     * 返回本生成器所支持的 IR 指令类型。
     *
     * @return {@link UnaryOperationInstruction} 的类对象
     */
    @Override
    public Class<UnaryOperationInstruction> supportedClass() {
        return UnaryOperationInstruction.class;
    }

    /**
     * 生成一元运算相关的虚拟机指令。
     *
     * @param ins       当前 IR 一元运算指令
     * @param out       虚拟机程序构建器，用于输出指令
     * @param slotMap   虚拟寄存器与槽号的映射表
     * @param currentFn 当前函数名称（用于作用域或调试，可选）
     */
    @Override
    public void generate(UnaryOperationInstruction ins,
                         VMProgramBuilder out,
                         Map<IRVirtualRegister, Integer> slotMap,
                         String currentFn) {
        // 获取操作数所在槽号
        int slotId = slotMap.get((IRVirtualRegister) ins.operands().getFirst());
        // 加载操作数到虚拟机栈顶
        out.emit(OpHelper.opcode("I_LOAD") + " " + slotId);
        // 生成对应的一元运算操作码（如取负等）
        out.emit(OpHelper.opcode(IROpCodeMapper.toVMOp(ins.op())));
        // 将结果存储到目标寄存器槽
        out.emit(OpHelper.opcode("I_STORE") + " " + slotMap.get(ins.dest()));
    }
}
