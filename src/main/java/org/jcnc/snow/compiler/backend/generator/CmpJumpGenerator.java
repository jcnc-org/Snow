package org.jcnc.snow.compiler.backend.generator;

import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.backend.utils.IROpCodeMapper;
import org.jcnc.snow.compiler.backend.utils.OpHelper;
import org.jcnc.snow.compiler.backend.utils.TypePromoteUtils;
import org.jcnc.snow.compiler.ir.instruction.IRCompareJumpInstruction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.Map;

/**
 * 条件比较跳转指令生成器
 * <p>
 * 该类实现了 {@link InstructionGenerator} 接口，
 * 负责将 IR 中的 {@link IRCompareJumpInstruction}（条件比较并跳转指令）
 * 转换为目标虚拟机（VM）可执行的指令序列。
 * </p>
 * <p>
 * 主要功能
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
     * 生成 IR 条件比较跳转指令的 VM 指令序列。
     * <ol>
     *     <li>确定左右操作数的槽位及静态类型</li>
     *     <li>加载并按需类型提升转换</li>
     *     <li>根据公共类型调整比较指令前缀</li>
     *     <li>发出最终比较和跳转指令</li>
     * </ol>
     *
     * @param ins       IR 条件比较跳转指令
     * @param out       VMProgramBuilder: 用于发出 VM 指令
     * @param slotMap   虚拟寄存器到 VM 槽位的映射表
     * @param currentFn 当前处理的函数名（调试用，当前未使用）
     */
    @Override
    public void generate(IRCompareJumpInstruction ins,
                         VMProgramBuilder out,
                         Map<IRVirtualRegister, Integer> slotMap,
                         String currentFn) {

        // 1. 获取左右操作数的槽位与静态类型
        int leftSlot = slotMap.get(ins.left());
        int rightSlot = slotMap.get(ins.right());
        char lType = out.getSlotType(leftSlot);
        char rType = out.getSlotType(rightSlot);
        char tType = TypePromoteUtils.promote(lType, rType);        // 公共类型提升

        // 2. 加载左右操作数并按需类型转换
        // 左操作数
        out.emit(OpHelper.opcode(TypePromoteUtils.str(lType) + "_LOAD") + " " + leftSlot);
        String cvt = TypePromoteUtils.convert(lType, tType);
        if (cvt != null) {
            out.emit(OpHelper.opcode(cvt));
        }

        // 右操作数
        out.emit(OpHelper.opcode(TypePromoteUtils.str(rType) + "_LOAD") + " " + rightSlot);
        cvt = TypePromoteUtils.convert(rType, tType);
        if (cvt != null) {
            out.emit(OpHelper.opcode(cvt));
        }

        // 3. 选择正确的比较指令前缀
        String cmpOp = IROpCodeMapper.toVMOp(ins.op());
        /*
         * 指令前缀（如 int 类型要用 I_C*, long 类型要用 L_C*）
         */
        if (tType == 'I' && cmpOp.startsWith("L_C")) {
            cmpOp = "I_C" + cmpOp.substring(3);
        } else if (tType == 'L' && cmpOp.startsWith("I_C")) {
            cmpOp = "L_C" + cmpOp.substring(3);
        }

        // 4. 发出比较与跳转指令
        out.emitBranch(OpHelper.opcode(cmpOp), ins.label());
    }
}
