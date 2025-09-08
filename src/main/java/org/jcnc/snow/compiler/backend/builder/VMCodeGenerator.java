package org.jcnc.snow.compiler.backend.builder;

import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.backend.utils.OpHelper;
import org.jcnc.snow.compiler.ir.core.IRFunction;
import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * VMCodeGenerator 负责将中间表示（IR）函数转换为目标虚拟机（VM）的指令序列。
 * <p>
 * 每个 IR 指令根据类型由对应的 InstructionGenerator 处理，并将结果输出到 VMProgramBuilder。
 * 该类通过注册表（registry）实现 IR 到 VM 指令生成器的快速分发。
 */
public final class VMCodeGenerator {

    /**
     * IR 指令类型到指令生成器的映射。
     * 每种 IRInstruction 都有对应的 InstructionGenerator 处理。
     */
    private final Map<Class<? extends IRInstruction>, InstructionGenerator<? extends IRInstruction>> registry;

    /**
     * 虚拟寄存器到 VM 局部槽位的映射表。
     * 用于寄存器分配与指令生成。
     */
    private final Map<IRVirtualRegister, Integer> slotMap;

    /**
     * 输出目标 VM 程序的构建器。
     * 提供 emit、beginFunction、endFunction 等接口。
     */
    private final VMProgramBuilder out;

    /**
     * 构造 VMCodeGenerator。
     *
     * @param slotMap    虚拟寄存器到 VM 局部槽位的分配表
     * @param out        输出 VM 程序的 builder
     * @param generators 可用的 IR 指令生成器列表，每个类型只应有一个
     */
    public VMCodeGenerator(Map<IRVirtualRegister, Integer> slotMap,
                           VMProgramBuilder out,
                           List<InstructionGenerator<? extends IRInstruction>> generators) {
        this.slotMap = slotMap;
        this.out = out;
        // 构建不可变的类型到生成器的注册表
        this.registry = generators.stream()
                .collect(Collectors.toUnmodifiableMap(InstructionGenerator::supportedClass, g -> g));
    }

    /**
     * 将 IRFunction 生成对应 VM 代码，并写入输出。
     *
     * <ol>
     *   <li>调用 {@code out.beginFunction} 标记函数起始。</li>
     *   <li>遍历函数体的每条 IR 指令，查找对应 InstructionGenerator 并生成目标代码。</li>
     *   <li>对 main/main.xxx 函数追加 HALT 指令，其它函数追加 RET。</li>
     *   <li>调用 {@code out.endFunction} 结束函数。</li>
     * </ol>
     *
     * @param fn 需要生成 VM 代码的 IRFunction
     * @throws IllegalStateException 如果遇到不支持的 IR 指令类型
     */
    public void generate(IRFunction fn) {
        String currentFn = fn.name();
        out.beginFunction(currentFn);
        for (IRInstruction ins : fn.body()) {
            @SuppressWarnings("unchecked")
            // 查找合适的指令生成器
            InstructionGenerator<IRInstruction> gen =
                    (InstructionGenerator<IRInstruction>) registry.get(ins.getClass());
            if (gen == null) {
                throw new IllegalStateException("Unsupported IR: " + ins);
            }
            // 调用生成器生成对应的 VM 指令
            gen.generate(ins, out, slotMap, currentFn);
        }
        // 结尾指令：main 函数统一用 HALT，其他函数用 RET
        String retOpcode = ("main".equals(currentFn) || currentFn.endsWith(".main")) ? "HALT" : "RET";
        out.emit(OpHelper.opcode(retOpcode));
        out.endFunction();
    }
}
