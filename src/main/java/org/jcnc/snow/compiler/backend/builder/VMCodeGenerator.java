package org.jcnc.snow.compiler.backend.builder;

import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.backend.utils.OpHelper;
import org.jcnc.snow.compiler.ir.core.IRFunction;
import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 将中间表示（IR）函数转换为目标虚拟机（VM）的指令序列。
 * <p>
 * 每条 IR 指令由与其类型匹配的 {@link InstructionGenerator} 处理并输出到
 * {@link VMProgramBuilder}。本类通过注册表将 IR 指令类型映射到对应的生成器，
 * 以实现快速分发与解耦。
 */
public final class VMCodeGenerator {

    /**
     * IR 指令类型到指令生成器的映射表。
     * 每种 {@link IRInstruction} 需要有且仅有一个对应的生成器。
     */
    private final Map<Class<? extends IRInstruction>, InstructionGenerator<? extends IRInstruction>> registry;

    /**
     * 虚拟寄存器到 VM 局部槽位（local slot）的映射。
     * 用于生成目标指令时查找操作数的槽位索引。
     */
    private final Map<IRVirtualRegister, Integer> slotMap;

    /**
     * 目标 VM 程序的构建器。
     * 提供 {@code emit}、{@code beginFunction}、{@code endFunction} 等接口。
     */
    private final VMProgramBuilder out;

    /**
     * 创建代码生成器实例。
     *
     * @param slotMap    虚拟寄存器与槽位的映射
     * @param out        目标 VM 程序输出构建器
     * @param generators 可用的 IR 指令生成器列表（每个 IR 类型应唯一）
     * @throws IllegalStateException 当列表中存在重复的 {@link InstructionGenerator#supportedClass()} 时
     */
    public VMCodeGenerator(Map<IRVirtualRegister, Integer> slotMap,
                           VMProgramBuilder out,
                           List<InstructionGenerator<? extends IRInstruction>> generators) {
        this.slotMap = slotMap;
        this.out = out;
        // 构建不可变的类型 → 生成器注册表
        this.registry = generators.stream()
                .collect(Collectors.toUnmodifiableMap(InstructionGenerator::supportedClass, g -> g));
    }

    /**
     * 将给定 {@link IRFunction} 生成对应的 VM 指令并写入输出。
     * <p>
     * 生成流程：
     * <ol>
     *   <li>调用 {@code out.beginFunction} 标记函数起始；</li>
     *   <li>初始化槽位类型信息（用于 VM 类型前缀）；</li>
     *   <li>按顺序遍历 IR 指令，分发到对应的 {@link InstructionGenerator} 生成目标指令；</li>
     *   <li>尾部追加 {@code HALT}（针对 main/main.xxx）或 {@code RET}；</li>
     *   <li>调用 {@code out.endFunction} 结束函数；</li>
     * </ol>
     *
     * @param fn 需要生成 VM 代码的 IR 函数
     * @throws IllegalStateException 遇到未注册或不支持的 IR 指令类型时抛出
     */
    public void generate(IRFunction fn) {
        String currentFn = fn.name();
        out.beginFunction(currentFn);
        primeSlotTypes(fn);
        for (IRInstruction ins : fn.body()) {
            @SuppressWarnings("unchecked")
            InstructionGenerator<IRInstruction> gen =
                    (InstructionGenerator<IRInstruction>) registry.get(ins.getClass());
            if (gen == null) {
                throw new IllegalStateException("Unsupported IR: " + ins);
            }
            gen.generate(ins, out, slotMap, currentFn);
        }
        // 结尾：main 使用 HALT，其他函数使用 RET
        String retOpcode = ("main".equals(currentFn) || currentFn.endsWith(".main")) ? "HALT" : "RET";
        out.emit(OpHelper.opcode(retOpcode));
        out.endFunction();
    }

    /**
     * 预设各槽位的类型前缀信息，便于后续生成器据此选择合适的 VM 指令形式。
     * <p>
     * 从函数的寄存器类型表中读取类型，并写入 {@link VMProgramBuilder#setSlotType(int, char)}。
     *
     * @param fn 当前函数
     */
    private void primeSlotTypes(IRFunction fn) {
        for (Map.Entry<IRVirtualRegister, String> entry : fn.registerTypes().entrySet()) {
            IRVirtualRegister reg = entry.getKey();
            Integer slot = slotMap.get(reg);
            if (slot == null) continue;
            char prefix = typePrefix(entry.getValue());
            if (prefix != 0) {
                out.setSlotType(slot, prefix);
            }
        }
    }

    /**
     * 将高层的类型名称映射为 VM 使用的类型前缀字符。
     * <ul>
     *   <li>{@code byte -> 'B'}</li>
     *   <li>{@code short -> 'S'}</li>
     *   <li>{@code int/integer/bool/boolean -> 'I'}</li>
     *   <li>{@code long -> 'L'}</li>
     *   <li>{@code float -> 'F'}</li>
     *   <li>{@code double -> 'D'}</li>
     *   <li>{@code string -> 'R'}</li>
     *   <li>未知类型 → 默认 {@code 'R'}</li>
     * </ul>
     *
     * @param type 类型名（大小写不敏感），可为空
     * @return 对应的类型前缀字符；若无法确定返回 {@code 'R'}；当 {@code type} 为 {@code null} 时返回 0
     */
    private char typePrefix(String type) {
        if (type == null) return 0;
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "byte" -> 'B';
            case "short" -> 'S';
            case "int", "integer", "bool", "boolean" -> 'I';
            case "long" -> 'L';
            case "float" -> 'F';
            case "double" -> 'D';
            case "string" -> 'R';
            default -> 'R';
        };
    }
}
