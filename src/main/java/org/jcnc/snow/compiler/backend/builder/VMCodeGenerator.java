package org.jcnc.snow.compiler.backend.builder;

import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.backend.utils.OpHelper;
import org.jcnc.snow.compiler.ir.core.IRFunction;
import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.ir.common.GlobalFunctionTable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Locale;

/**
 * 负责将 IRFunction 转换为虚拟机（VM）可执行指令序列的核心生成器。
 *
 * <p>
 * 每条 IR 指令会根据其类型分发到对应的 {@link InstructionGenerator}，
 * 并通过 {@link VMProgramBuilder} 输出为目标 VM 指令。
 * 通过内部的注册表（registry）实现 IR 指令到生成器的快速映射。
 * </p>
 */
public final class VMCodeGenerator {

    /**
     * IR 指令类型到指令生成器的映射表。
     * 每种 IR 指令类型在此表中必须有唯一的生成器实例。
     */
    private final Map<Class<? extends IRInstruction>, InstructionGenerator<? extends IRInstruction>> registry;

    /**
     * 虚拟寄存器至槽位编号的映射表。
     * 用于在指令生成阶段定位操作数存储位置。
     */
    private final Map<IRVirtualRegister, Integer> slotMap;

    /**
     * VM 程序构建器。用于写入最终生成的指令序列。
     */
    private final VMProgramBuilder out;

    /**
     * 构造函数。
     *
     * @param slotMap    虚拟寄存器到槽位的映射表
     * @param out        VM 程序输出构建器
     * @param generators 指令生成器列表，每类 IR 指令应有唯一对应的生成器
     */
    public VMCodeGenerator(Map<IRVirtualRegister, Integer> slotMap,
                           VMProgramBuilder out,
                           List<InstructionGenerator<? extends IRInstruction>> generators) {
        this.slotMap = slotMap;
        this.out = out;

        // 为指令类型映射到对应的生成器，构建不可变注册表
        this.registry = generators.stream()
                .collect(Collectors.toUnmodifiableMap(InstructionGenerator::supportedClass, g -> g));
    }

    /**
     * 将指定 IRFunction 编译为 VM 指令。
     *
     * <p>
     * 具体流程如下：
     * </p>
     * <ol>
     *     <li>调用 {@code out.beginFunction} 标记函数开始</li>
     *     <li>预设参数槽位的类型前缀，用于生成正确的 LOAD/STORE 指令</li>
     *     <li>遍历函数体 IR 指令，分发给对应生成器输出 VM 指令</li>
     *     <li>根据函数是否为 main，补充末尾 HALT 或 RET 指令</li>
     *     <li>调用 {@code out.endFunction} 结束函数</li>
     * </ol>
     *
     * @param fn 待生成的 IRFunction
     * @throws IllegalStateException 当遇到未注册的 IR 指令类型时抛出
     */
    public void generate(IRFunction fn) {
        String currentFn = fn.name();
        out.beginFunction(currentFn);

        // 预先标注参数槽位类型前缀，避免生成错误的 LOAD/STORE 指令
        List<String> paramTypes = GlobalFunctionTable.getParamTypes(currentFn);
        if (paramTypes != null) {
            List<IRVirtualRegister> params = fn.parameters();
            for (int i = 0; i < params.size() && i < paramTypes.size(); i++) {
                Integer slot = slotMap.get(params.get(i));
                if (slot != null) {
                    out.setSlotType(slot, normalizeTypePrefix(paramTypes.get(i)));
                }
            }
        }

        // 处理函数体每条 IR 指令
        for (IRInstruction ins : fn.body()) {
            @SuppressWarnings("unchecked")
            InstructionGenerator<IRInstruction> gen =
                    (InstructionGenerator<IRInstruction>) registry.get(ins.getClass());

            if (gen == null) {
                throw new IllegalStateException("Unsupported IR: " + ins);
            }

            gen.generate(ins, out, slotMap, currentFn);
        }

        // 主函数末尾必须执行 HALT；其他函数默认 RET
        String retOpcode =
                ("main".equals(currentFn) || currentFn.endsWith(".main")) ? "HALT" : "RET";
        out.emit(OpHelper.opcode(retOpcode));

        out.endFunction();
    }

    /**
     * 将类型名映射为 VM 槽位类型前缀。
     *
     * @param name 类型名称
     * @return 单字符类型前缀
     */
    private char normalizeTypePrefix(String name) {
        if (name == null) {
            return 'I';
        }
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "byte" -> 'B';
            case "short" -> 'S';
            case "int", "integer", "bool", "boolean" -> 'I';
            case "long" -> 'L';
            case "float" -> 'F';
            case "double" -> 'D';
            case "string" -> 'R';
            case "void" -> 'V';
            default -> 'R';
        };
    }
}