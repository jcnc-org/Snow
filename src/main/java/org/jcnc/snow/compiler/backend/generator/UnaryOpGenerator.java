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

        /* -------- 1. 源槽位与类型 -------- */
        int srcSlot  = slotMap.get((IRVirtualRegister) ins.operands().getFirst());
        char prefix  = out.getSlotType(srcSlot);          // 未登记则返回默认 'I'

        String loadOp  = prefix + "_LOAD";
        String storeOp = prefix + "_STORE";

        /* -------- 2. 指令序列 -------- */
        // 2-A. 加载操作数
        out.emit(OpHelper.opcode(loadOp) +
                " " + srcSlot);

        // 2-B. 执行具体一元运算（NEG、NOT…）
        out.emit(OpHelper.opcode(
                IROpCodeMapper.toVMOp(ins.op())));

        // 2-C. 存结果到目标槽
        int destSlot = slotMap.get(ins.dest());
        out.emit(OpHelper.opcode(storeOp) +
                " " + destSlot);

        /* -------- 3. 更新目标槽类型 -------- */
        out.setSlotType(destSlot, prefix);
    }
}
