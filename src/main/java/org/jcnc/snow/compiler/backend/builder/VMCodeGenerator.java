package org.jcnc.snow.compiler.backend.builder;

import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.ir.core.IRFunction;
import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 虚拟机代码生成器（VMCodeGenerator）
 * <p>
 * 本类作为指令生成器调度中心，不负责任何具体 IR 指令到 VM 指令的转换实现，
 * 仅负责根据指令类型分发到对应的 {@link InstructionGenerator} 子类完成实际生成。
 * </p>
 * <p>
 * 工作流程简述: 
 * <ol>
 *   <li>接收一组已注册的 IR 指令生成器，并建立类型到生成器的映射表。</li>
 *   <li>遍历 IR 函数体的每条指令，根据类型找到对应的生成器，调用其 generate 方法生成 VM 指令。</li>
 *   <li>生成流程以函数为单位（beginFunction/endFunction）。</li>
 * </ol>
 */
public final class VMCodeGenerator {

    /**
     * 指令类型到生成器的注册表（调度表）。
     * <p>
     * 键: IR 指令类型（Class对象），
     * 值: 对应的指令生成器实例。
     * </p>
     */
    private final Map<Class<? extends IRInstruction>, InstructionGenerator<? extends IRInstruction>> registry;

    /**
     * 虚拟寄存器到槽号的映射表，由 RegisterAllocator 负责生成。
     */
    private final Map<IRVirtualRegister, Integer> slotMap;

    /**
     * 虚拟机程序构建器，用于输出 VM 指令。
     */
    private final VMProgramBuilder out;

    /**
     * 当前处理的函数名，用于部分指令生成逻辑（如主函数判断等）。
     */
    private String currentFn;

    /**
     * 构造方法
     *
     * @param slotMap    虚拟寄存器到槽号的映射
     * @param out        虚拟机程序构建器
     * @param generators 各类 IR 指令生成器集合，需预先构建
     */
    public VMCodeGenerator(Map<IRVirtualRegister, Integer> slotMap,
                           VMProgramBuilder out,
                           List<InstructionGenerator<? extends IRInstruction>> generators) {
        this.slotMap = slotMap;
        this.out = out;
        // 按类型注册各 IR 指令生成器，建立不可变类型-生成器映射表
        this.registry = generators.stream()
                .collect(Collectors.toUnmodifiableMap(InstructionGenerator::supportedClass, g -> g));
    }

    /**
     * 为一个 IR 函数生成虚拟机指令
     *
     * @param fn 待生成的 IR 函数对象
     * @throws IllegalStateException 若遇到不支持的 IR 指令类型
     */
    public void generate(IRFunction fn) {
        this.currentFn = fn.name();
        out.beginFunction(currentFn); // 输出函数起始
        for (IRInstruction ins : fn.body()) {
            @SuppressWarnings("unchecked")
            // 取得与当前 IR 指令类型匹配的生成器（泛型强转消除类型警告）
            InstructionGenerator<IRInstruction> gen =
                    (InstructionGenerator<IRInstruction>) registry.get(ins.getClass());
            if (gen == null) {
                throw new IllegalStateException("Unsupported IR: " + ins);
            }
            // 通过多态分发到实际生成器
            gen.generate(ins, out, slotMap, currentFn);
        }
        out.endFunction(); // 输出函数结束
    }
}
