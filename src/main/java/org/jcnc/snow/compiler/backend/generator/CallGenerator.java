package org.jcnc.snow.compiler.backend.generator;

import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.backend.utils.OpHelper;
import org.jcnc.snow.compiler.ir.common.GlobalFunctionTable;
import org.jcnc.snow.compiler.ir.core.IRValue;
import org.jcnc.snow.compiler.ir.instruction.CallInstruction;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.vm.engine.VMOpCode;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CallGenerator 负责将 IR 层的 CallInstruction 转换为 VM 层的函数调用指令。
 * <p>
 * 功能包括：
 * <ul>
 *   <li>处理系统调用（syscall），支持字符串常量池</li>
 *   <li>数组元素访问及赋值（__index / __setindex 系列）</li>
 *   <li>普通函数调用，根据返回类型选择合适的 STORE 指令</li>
 * </ul>
 */
public class CallGenerator implements InstructionGenerator<CallInstruction> {

    /**
     * 字符串常量池，用于 syscall 子命令的字符串存储，
     * 键为虚拟寄存器 ID，值为字符串常量。
     */
    private static final Map<Integer, String> STRING_CONST_POOL = new ConcurrentHashMap<>();

    /**
     * 当前正在生成的函数名，用于错误上下文。
     */
    private String fn;

    /**
     * 注册字符串常量到常量池。
     *
     * @param regId 虚拟寄存器 ID
     * @param value 字符串常量值
     */
    public static void registerStringConst(int regId, String value) {
        STRING_CONST_POOL.put(regId, value);
    }

    /**
     * 将语言层类型名映射为 VM 指令的类型前缀字符。
     *
     * @param name 类型名称，如 "int", "double", "string"
     * @return 对应的 VM 类型前缀（'I','D','R' 等）
     */
    private static char normalizeTypePrefix(String name) {
        if (name == null) return 'I';
        String n = name.toLowerCase(Locale.ROOT);
        return switch (n) {
            case "byte" -> 'B';
            case "short" -> 'S';
            case "int", "integer", "bool", "boolean" -> 'I';
            case "long" -> 'L';
            case "float" -> 'F';
            case "double" -> 'D';
            case "string" -> 'R'; // 字符串为引用类型
            case "void" -> 'V';
            default -> 'R'; // 结构体、自定义对象均视为引用类型
        };
    }

    /**
     * 返回本生成器支持的 IR 指令类型，此处为 CallInstruction。
     */
    @Override
    public Class<CallInstruction> supportedClass() {
        return CallInstruction.class;
    }

    /**
     * 核心生成方法，根据函数名分发到 syscall、数组操作或普通调用的处理逻辑。
     *
     * @param ins       待转换的 IR 调用指令
     * @param out       VM 程序构建器，用于输出指令
     * @param slotMap   IR 虚拟寄存器到 VM 本地槽位的映射
     * @param currentFn 当前所在的函数名，用于上下文传递
     */
    @Override
    public void generate(CallInstruction ins,
                         VMProgramBuilder out,
                         Map<IRVirtualRegister, Integer> slotMap,
                         String currentFn) {
        String fn = ins.getFunctionName();

        // 处理特殊的 syscall 调用
        if ("syscall".equals(fn) || fn.endsWith(".syscall")) {
            generateSyscall(ins, out, slotMap, fn);
            return;
        }

        // 处理数组读取和写入的内置函数
        switch (fn) {
            case "__index_b" -> {
                generateIndexInstruction(ins, out, slotMap, 'B');
                return;
            }
            case "__index_s" -> {
                generateIndexInstruction(ins, out, slotMap, 'S');
                return;
            }
            case "__index_i" -> {
                generateIndexInstruction(ins, out, slotMap, 'I');
                return;
            }
            case "__index_l" -> {
                generateIndexInstruction(ins, out, slotMap, 'L');
                return;
            }
            case "__index_f" -> {
                generateIndexInstruction(ins, out, slotMap, 'F');
                return;
            }
            case "__index_d" -> {
                generateIndexInstruction(ins, out, slotMap, 'D');
                return;
            }
            case "__index_r" -> {
                generateIndexInstruction(ins, out, slotMap, 'R');
                return;
            }
            case "__setindex_b" -> {
                generateSetIndexInstruction(ins, out, slotMap, 'B');
                return;
            }
            case "__setindex_s" -> {
                generateSetIndexInstruction(ins, out, slotMap, 'S');
                return;
            }
            case "__setindex_i" -> {
                generateSetIndexInstruction(ins, out, slotMap, 'I');
                return;
            }
            case "__setindex_l" -> {
                generateSetIndexInstruction(ins, out, slotMap, 'L');
                return;
            }
            case "__setindex_f" -> {
                generateSetIndexInstruction(ins, out, slotMap, 'F');
                return;
            }
            case "__setindex_d" -> {
                generateSetIndexInstruction(ins, out, slotMap, 'D');
                return;
            }
            case "__setindex_r" -> {
                generateSetIndexInstruction(ins, out, slotMap, 'R');
                return;
            }
        }

        // 默认：普通函数调用
        generateNormalCall(ins, out, slotMap, fn);
    }

    /**
     * 生成数组元素赋值指令。
     *
     * @param ins     调用 __setindex_* 的 IR 指令
     * @param out     VM 指令构建器
     * @param slotMap 寄存器到槽位映射
     * @param valType 元素值的类型前缀（如 'I','R' 等）
     */
    private void generateSetIndexInstruction(CallInstruction ins,
                                             VMProgramBuilder out,
                                             Map<IRVirtualRegister, Integer> slotMap,
                                             char valType) {
        List<IRValue> args = ins.getArguments();
        if (args.size() != 3) {
            throw new IllegalStateException("[CallGenerator] __setindex_* 需要三个参数 (arr, idx, value)，实际: " + args);
        }
        loadArgument(out, slotMap, args.get(0), 'R', ins.getFunctionName()); // 数组引用
        loadArgument(out, slotMap, args.get(1), 'I', ins.getFunctionName()); // 索引
        loadArgument(out, slotMap, args.get(2), valType, ins.getFunctionName()); // 值
        out.emit(VMOpCode.SYSCALL + " ARR_SET");
    }

    /**
     * 生成数组元素读取指令。
     *
     * @param ins     调用 __index_* 的 IR 指令
     * @param out     VM 指令构建器
     * @param slotMap 寄存器到槽位映射
     * @param retType 返回值类型前缀
     */
    private void generateIndexInstruction(CallInstruction ins,
                                          VMProgramBuilder out,
                                          Map<IRVirtualRegister, Integer> slotMap,
                                          char retType) {
        String fn = ins.getFunctionName();
        List<IRValue> args = ins.getArguments();
        if (args.size() != 2) {
            throw new IllegalStateException("[CallGenerator] " + fn + " 需要两个参数 (arr, idx)，实际: " + args);
        }
        loadArgument(out, slotMap, args.get(0), 'R', fn); // 数组引用
        loadArgument(out, slotMap, args.get(1), 'I', fn); // 索引
        out.emit(VMOpCode.SYSCALL + " ARR_GET");

        IRVirtualRegister dest = ins.getDest();
        if (dest == null) {
            throw new IllegalStateException("[CallGenerator] " + fn + " 必须有返回值寄存器");
        }
        Integer destSlot = slotMap.get(dest);
        if (destSlot == null) {
            throw new IllegalStateException("[CallGenerator] " + fn + " 未找到目标槽位");
        }
        // 根据类型选择 STORE 指令
        switch (retType) {
            case 'B' -> out.emit(OpHelper.opcode("B_STORE") + " " + destSlot);
            case 'S' -> out.emit(OpHelper.opcode("S_STORE") + " " + destSlot);
            case 'I' -> out.emit(OpHelper.opcode("I_STORE") + " " + destSlot);
            case 'L' -> out.emit(OpHelper.opcode("L_STORE") + " " + destSlot);
            case 'F' -> out.emit(OpHelper.opcode("F_STORE") + " " + destSlot);
            case 'D' -> out.emit(OpHelper.opcode("D_STORE") + " " + destSlot);
            default -> out.emit(OpHelper.opcode("R_STORE") + " " + destSlot);
        }
        out.setSlotType(destSlot, retType);
    }

    /**
     * 生成系统调用（syscall）指令。
     * 首个参数为子命令，支持常量或寄存器引用；其余参数均以引用形式加载。
     * 若存在返回寄存器，则将结果存为整数类型。
     *
     * @param ins     syscall 对应的 IR 调用指令
     * @param out     VM 指令构建器
     * @param slotMap 寄存器到槽位的映射
     * @param fn      函数名（"syscall" 或以 ".syscall" 结尾）
     */
    private void generateSyscall(CallInstruction ins,
                                 VMProgramBuilder out,
                                 Map<IRVirtualRegister, Integer> slotMap,
                                 String fn) {
        List<IRValue> args = ins.getArguments();
        if (args.isEmpty()) {
            throw new IllegalStateException("[CallGenerator] syscall 至少需要一个子命令");
        }
        String subcmd = resolveSyscallSubcmd(args.get(0), fn);
        for (int i = 1; i < args.size(); i++) {
            loadArgument(out, slotMap, args.get(i), 'R', fn);
        }
        out.emit(VMOpCode.SYSCALL + " " + subcmd);

        IRVirtualRegister dest = ins.getDest();
        if (dest != null) {
            Integer slot = slotMap.get(dest);
            if (slot == null) throw new IllegalStateException("[CallGenerator] syscall 未找到目标槽位");
            out.emit(OpHelper.opcode("I_STORE") + " " + slot);
            out.setSlotType(slot, 'I');
        }
    }

    /**
     * 解析 syscall 子命令字符串：
     * 若为 IRConstant，则直接取字符串值；
     * 若为 IRVirtualRegister，则从常量池中获取。
     * 最终返回大写子命令名称。
     *
     * @param arg 第一个参数，常量或虚拟寄存器
     * @param fn  所属函数名，用于错误提示
     * @return 大写的子命令字符串
     * @throws IllegalStateException 参数类型或常量池缺失时抛出
     */
    private String resolveSyscallSubcmd(IRValue arg, String fn) {
        if (arg instanceof IRConstant(Object value)) {
            if (value instanceof String s) {
                return s.toUpperCase(Locale.ROOT);
            }
            throw new IllegalStateException("[CallGenerator] syscall 子命令必须是字符串 (函数: " + fn + ")");
        }
        if (arg instanceof IRVirtualRegister(int id)) {
            String s = STRING_CONST_POOL.get(id);
            if (s == null) throw new IllegalStateException("[CallGenerator] 未注册的 syscall 字符串常量");
            return s.toUpperCase(Locale.ROOT);
        }
        throw new IllegalStateException("[CallGenerator] syscall 子命令参数错误: " + arg);
    }

    /**
     * 生成普通函数调用指令：
     * 参数均以引用形式压栈，调用完成后，若返回类型非 void，
     * 则选择合适的 STORE 指令保存结果。
     *
     * @param ins     调用指令
     * @param out     指令构建器
     * @param slotMap 寄存器到槽位映射
     * @param fn      被调用函数名称
     */
    private void generateNormalCall(CallInstruction ins,
                                    VMProgramBuilder out,
                                    Map<IRVirtualRegister, Integer> slotMap,
                                    String fn) {
        String retTypeName = GlobalFunctionTable.getReturnType(fn);
        char retType = normalizeTypePrefix(retTypeName);

        for (IRValue arg : ins.getArguments()) {
            loadArgument(out, slotMap, arg, 'R', fn);
        }
        out.emitCall(fn, ins.getArguments().size());

        if ("void".equals(retTypeName)) {
            return;
        }
        IRVirtualRegister dest = ins.getDest();
        if (dest == null) {
            throw new IllegalStateException("[CallGenerator] 普通调用缺少返回寄存器");
        }
        Integer slot = slotMap.get(dest);
        if (slot == null) {
            throw new IllegalStateException("[CallGenerator] 未找到返回槽位");
        }
        out.emit(OpHelper.opcode(retType + "_STORE") + " " + slot);
        out.setSlotType(slot, retType);
    }

    /**
     * 加载参数：
     * 验证为虚拟寄存器后，获取槽位和类型，
     * 缺省时使用 defaultType，然后发出 LOAD 指令。
     *
     * @param out         指令构建器
     * @param slotMap     寄存器到槽位映射
     * @param arg         IR 参数值
     * @param defaultType 缺省类型前缀
     * @param fn          所属函数名，用于错误提示
     */
    private void loadArgument(VMProgramBuilder out,
                              Map<IRVirtualRegister, Integer> slotMap,
                              IRValue arg,
                              char defaultType,
                              String fn) {
        this.fn = fn;
        if (!(arg instanceof IRVirtualRegister vr)) {
            throw new IllegalStateException("[CallGenerator] 参数必须是 IRVirtualRegister");
        }
        Integer slot = slotMap.get(vr);
        if (slot == null) {
            throw new IllegalStateException("[CallGenerator] 未找到参数槽位");
        }
        char t = out.getSlotType(slot);
        if (t == '\0' || (t == 'I' && defaultType != 'I')) {
            t = defaultType;
        }
        out.emit(OpHelper.opcode(t + "_LOAD") + " " + slot);
    }

    /**
     * 返回最近一次处理的函数名，可用于调试。
     *
     * @return 函数名字符串
     */
    public String getFn() {
        return fn;
    }
}
