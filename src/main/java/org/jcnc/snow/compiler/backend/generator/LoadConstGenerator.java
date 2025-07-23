package org.jcnc.snow.compiler.backend.generator;

import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.backend.utils.OpHelper;
import org.jcnc.snow.compiler.ir.instruction.LoadConstInstruction;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.Map;

/**
 * <b>LoadConstGenerator - 将 IR {@code LoadConstInstruction} 生成 VM 指令</b>
 *
 * <p>
 * 本类负责将 IR 层的常量加载指令 {@link LoadConstInstruction} 转换为对应的虚拟机指令。
 * 额外支持：如果常量类型为 {@code String}，会同步登记到
 * {@link CallGenerator} 的字符串常量池，方便 syscall 降级场景使用。
 * </p>
 */
public class LoadConstGenerator implements InstructionGenerator<LoadConstInstruction> {

    /**
     * 指定本生成器支持的 IR 指令类型（LoadConstInstruction）
     */
    @Override
    public Class<LoadConstInstruction> supportedClass() {
        return LoadConstInstruction.class;
    }

    /**
     * 生成 VM 指令主流程
     *
     * @param ins       当前常量加载指令
     * @param out       指令输出构建器
     * @param slotMap   虚拟寄存器与物理槽位映射
     * @param currentFn 当前函数名
     */
    @Override
    public void generate(LoadConstInstruction ins,
                         VMProgramBuilder out,
                         Map<IRVirtualRegister, Integer> slotMap,
                         String currentFn) {

        /* 1. 获取常量值 */
        IRConstant constant = (IRConstant) ins.operands().getFirst();
        Object value = constant.value();

        /* 2. 生成 PUSH 指令，将常量值入栈 */
        out.emit(OpHelper.pushOpcodeFor(value) + " " + value);

        /* 3. STORE 到目标槽位 */
        int slot = slotMap.get(ins.dest());
        out.emit(OpHelper.storeOpcodeFor(value) + " " + slot);

        /* 4. 标记槽位数据类型（用于后续类型推断和 LOAD/STORE 指令选择） */
        char prefix = switch (value) {
            case Integer _ -> 'I';   // 整型
            case Long    _ -> 'L';   // 长整型
            case Short   _ -> 'S';   // 短整型
            case Byte    _ -> 'B';   // 字节型
            case Double  _ -> 'D';   // 双精度
            case Float   _ -> 'F';   // 单精度
            case Boolean _ -> 'I';   // 布尔类型用 I 处理
            case String  _ -> 'R';   // 字符串常量
            case null, default ->    // 其它类型异常
                    throw new IllegalStateException("未知的常量类型: "
                            + (value != null ? value.getClass() : null));
        };
        out.setSlotType(slot, prefix);

        /* 5. 如果是字符串常量，则登记到 CallGenerator 的常量池，便于 syscall 字符串降级使用 */
        if (value instanceof String s) {
            CallGenerator.registerStringConst(ins.dest().id(), s);
        }
    }
}
