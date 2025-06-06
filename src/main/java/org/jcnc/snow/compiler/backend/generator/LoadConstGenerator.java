package org.jcnc.snow.compiler.backend.generator;

import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.backend.util.OpHelper;
import org.jcnc.snow.compiler.ir.instruction.LoadConstInstruction;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.Map;

/**
 * 常量加载指令生成器
 * 该类用于生成将常量加载到虚拟机寄存器的指令，包括 PUSH 常量值和 STORE 到指定槽位，
 * 并为每个槽位设置正确的类型前缀（如 'I', 'L', 'F' 等）。
 */
public class LoadConstGenerator implements InstructionGenerator<LoadConstInstruction> {

    /**
     * 返回本生成器支持的指令类型，即 LoadConstInstruction。
     *
     * @return 支持的指令类型的 Class 对象
     */
    @Override
    public Class<LoadConstInstruction> supportedClass() {
        return LoadConstInstruction.class;
    }

    /**
     * 生成一条常量加载指令的目标虚拟机代码。
     *
     * @param ins       当前要生成的 LoadConstInstruction 指令
     * @param out       VMProgramBuilder，用于输出生成的虚拟机指令
     * @param slotMap   IR 虚拟寄存器到实际槽位编号的映射表
     * @param currentFn 当前函数名（如有需要可使用）
     */
    @Override
    public void generate(LoadConstInstruction ins,
                         VMProgramBuilder out,
                         Map<IRVirtualRegister, Integer> slotMap,
                         String currentFn) {
        // 1. 获取常量值（第一个操作数必为常量）
        IRConstant constant = (IRConstant) ins.operands().getFirst();
        Object value = constant.value();

        // 2. 生成 PUSH 指令，将常量值推入操作数栈
        // 通过 OpHelper 辅助方法获取合适的数据类型前缀
        String pushOp = OpHelper.pushOpcodeFor(value);
        out.emit(pushOp + " " + value);

        // 3. 生成 STORE 指令，将栈顶的值存入对应槽位（寄存器）
        // 同样通过 OpHelper 获取对应类型的 STORE 指令
        String storeOp = OpHelper.storeOpcodeFor(value);
        // 获取目标虚拟寄存器对应的槽位编号
        int slot = slotMap.get(ins.dest());
        out.emit(storeOp + " " + slot);

        // 4. 根据常量的 Java 类型，为槽位设置正确的前缀字符
        // 这样在后续类型检查/运行时可用，常见前缀如 'I', 'L', 'F', 'D', 'S', 'B'
        char prefix = switch (value) {
            case Integer _ -> 'I';  // 整型
            case Long _ -> 'L';  // 长整型
            case Short _ -> 'S';  // 短整型
            case Byte _ -> 'B';  // 字节型
            case Double _ -> 'D';  // 双精度浮点型
            case Float _ -> 'F';  // 单精度浮点型
            case null, default ->
                    throw new IllegalStateException("Unknown const type: " + (value != null ? value.getClass() : null));
        };

        // 写入槽位类型映射表
        out.setSlotType(slot, prefix);
    }
}
