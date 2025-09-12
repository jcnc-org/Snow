package org.jcnc.snow.compiler.backend.core;

import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.Map;

/**
 * 通用指令生成器接口
 * <p>
 * 本接口规定了所有 IR 指令生成器（翻译器）必须实现的方法，负责将特定类型的 IR 指令
 * 翻译为虚拟机（VM）指令。每个具体的指令生成器都需要指定其支持的 IR 指令类型，并实现翻译生成的方法。
 *
 * @param <T> 指令生成器所支持的 IRInstruction 子类型
 */
public interface InstructionGenerator<T extends IRInstruction> {
    /**
     * 获取当前生成器支持的 IR 指令类型。
     *
     * @return 当前生成器支持的 IRInstruction 类对象
     */
    Class<T> supportedClass();

    /**
     * 将一条 IR 指令翻译为对应的 VM 指令序列。
     *
     * @param ins       当前待翻译的 IR 指令
     * @param out       虚拟机程序构建器，用于输出 VM 指令
     * @param slotMap   虚拟寄存器与实际槽号的映射关系
     * @param currentFn 当前函数名称（用于作用域或调试等）
     */
    void generate(T ins, VMProgramBuilder out, Map<IRVirtualRegister, Integer> slotMap, String currentFn);
}
