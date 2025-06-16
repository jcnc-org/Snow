package org.jcnc.snow.compiler.backend.generator;

import org.jcnc.snow.compiler.backend.util.IROpCodeMapper;
import org.jcnc.snow.compiler.backend.util.OpHelper;
import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.ir.instruction.IRCompareJumpInstruction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.Map;

/**
 * 条件比较跳转指令生成器
 * <p>
 * 该类实现了 {@link InstructionGenerator} 接口，用于将 IR 中的条件比较跳转指令
 * 转换为虚拟机可执行的指令序列。主要流程是先将比较操作数加载到虚拟机栈中，生成比较操作码，
 * 并发出跳转到目标标签的指令。
 */
public class CmpJumpGenerator implements InstructionGenerator<IRCompareJumpInstruction> {

    /**
     * 返回该生成器所支持的指令类型。
     *
     * @return {@link IRCompareJumpInstruction} 的类对象
     */
    @Override
    public Class<IRCompareJumpInstruction> supportedClass() {
        return IRCompareJumpInstruction.class;
    }

    /**
     * 生成条件比较跳转相关的虚拟机指令。
     *
     * @param ins        需要生成的条件比较跳转中间指令（IR）
     * @param out        虚拟机程序构建器，用于输出生成的指令
     * @param slotMap    虚拟寄存器到实际槽（slot）编号的映射表
     * @param currentFn  当前处理的函数名（可用于调试或作用域标识）
     */
    @Override
    public void generate(IRCompareJumpInstruction ins,
                         VMProgramBuilder out,
                         Map<IRVirtualRegister, Integer> slotMap,
                         String currentFn) {
        // 获取左操作数所在的寄存器槽编号
        int leftSlot = slotMap.get(ins.left());
        // 获取右操作数所在的寄存器槽编号
        int rightSlot = slotMap.get(ins.right());
        // 加载左操作数到虚拟机栈
        out.emit(OpHelper.opcode("L_LOAD") + " " + leftSlot);
        // 加载右操作数到虚拟机栈
        out.emit(OpHelper.opcode("L_LOAD") + " " + rightSlot);
        // 获取与当前比较操作对应的虚拟机操作码
        String cmpOp = IROpCodeMapper.toVMOp(ins.op());
        // 生成分支跳转指令，如果比较成立则跳转到目标标签
        out.emitBranch(OpHelper.opcode(cmpOp), ins.label());
    }
}
