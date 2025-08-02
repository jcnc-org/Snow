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
 * {@code CallGenerator} 负责将 IR 层的 {@link CallInstruction} 生成对应的 VM 层函数调用指令。
 * <p>
 * 支持 syscall、普通函数调用、一维/多维数组下标访问、以及字符串常量池绑定等功能。
 * </p>
 *
 * <ul>
 *   <li>syscall: 支持字符串常量与寄存器到子命令的绑定与解析</li>
 *   <li>数组下标访问: 支持所有主流基础类型 “__index_b/s/i/l/f/d/r”</li>
 *   <li>普通函数: 支持自动推断返回值类型与槽位类型</li>
 * </ul>
 */
public class CallGenerator implements InstructionGenerator<CallInstruction> {

    /**
     * 字符串常量池：用于绑定虚拟寄存器 id 到字符串值（供 syscall 子命令使用）。
     * <br>
     * 使用 ConcurrentHashMap 保证并发安全，所有 registerStringConst 的注册和读取都是线程安全的。
     */
    private static final Map<Integer, String> STRING_CONST_POOL = new ConcurrentHashMap<>();

    /**
     * 注册一个字符串常量，绑定到虚拟寄存器 id。
     *
     * @param regId 虚拟寄存器 id
     * @param value 字符串常量
     */
    public static void registerStringConst(int regId, String value) {
        STRING_CONST_POOL.put(regId, value);
    }

    /**
     * 返回本生成器支持的 IR 指令类型。
     *
     * @return {@link CallInstruction} 类型的 Class 对象
     */
    @Override
    public Class<CallInstruction> supportedClass() {
        return CallInstruction.class;
    }

    /**
     * 生成指定的调用指令，包括 syscall、数组下标、普通函数。
     *
     * @param ins       IR 层的调用指令
     * @param out       VM 程序构建器
     * @param slotMap   寄存器到槽位的映射表
     * @param currentFn 当前函数名
     */
    @Override
    public void generate(CallInstruction ins,
                         VMProgramBuilder out,
                         Map<IRVirtualRegister, Integer> slotMap,
                         String currentFn) {
        String fn = ins.getFunctionName();

        // 特殊处理 syscall 调用
        if ("syscall".equals(fn) || fn.endsWith(".syscall")) {
            generateSyscall(ins, out, slotMap, fn);
            return;
        }

        // 各种一维数组类型（byte/short/int/long/float/double/boolean）读取
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

        // 普通函数调用
        generateNormalCall(ins, out, slotMap, fn);
    }

    // ========== 私有辅助方法 ==========

    /**
     * 生成数组元素赋值指令（arr[idx] = value），无返回值。
     * <p>
     * 调用栈压栈顺序为：arr (引用类型, R)，idx (整型, I)，value (元素类型, T)。
     * 执行 SYSCALL ARR_SET 指令以完成数组赋值操作。
     * </p>
     *
     * @param ins     当前的调用指令（包含参数信息）
     * @param out     VM 指令生成器（用于输出 VM 指令）
     * @param slotMap 虚拟寄存器与槽位的映射表
     * @param valType 待写入的 value 的类型标识（'B': byte, 'S': short, 'I': int, 'L': long, 'F': float, 'D': double, 其余为引用类型'R'）
     * @throws IllegalStateException 如果参数数量不为3，则抛出异常
     */
    private void generateSetIndexInstruction(CallInstruction ins,
                                             VMProgramBuilder out,
                                             Map<IRVirtualRegister, Integer> slotMap,
                                             char valType) {
        List<IRValue> args = ins.getArguments();
        if (args.size() != 3) {
            // 参数数量错误，抛出异常并输出实际参数列表
            throw new IllegalStateException(
                    "[CallGenerator] __setindex_* 需要三个参数(arr, idx, value)，实际: " + args);
        }

        // 第一个参数为数组对象，压入引用类型寄存器（'R'），如 arr
        loadArgument(out, slotMap, args.get(0), 'R', ins.getFunctionName());

        // 第二个参数为索引值，压入整型寄存器（'I'），如 idx
        loadArgument(out, slotMap, args.get(1), 'I', ins.getFunctionName());

        // 第三个参数为待赋值元素，根据元素类型压入相应类型寄存器
        // 支持类型：'B'(byte), 'S'(short), 'I'(int), 'L'(long), 'F'(float), 'D'(double)
        // 其他情况（如引用类型），按'R'处理
        switch (valType) {
            case 'B' -> loadArgument(out, slotMap, args.get(2), 'B', ins.getFunctionName());
            case 'S' -> loadArgument(out, slotMap, args.get(2), 'S', ins.getFunctionName());
            case 'I' -> loadArgument(out, slotMap, args.get(2), 'I', ins.getFunctionName());
            case 'L' -> loadArgument(out, slotMap, args.get(2), 'L', ins.getFunctionName());
            case 'F' -> loadArgument(out, slotMap, args.get(2), 'F', ins.getFunctionName());
            case 'D' -> loadArgument(out, slotMap, args.get(2), 'D', ins.getFunctionName());
            default -> loadArgument(out, slotMap, args.get(2), 'R', ins.getFunctionName());
        }

        // 输出 VM 指令：SYSCALL ARR_SET，完成数组元素写入操作
        out.emit(VMOpCode.SYSCALL + " " + "ARR_SET");
    }


    /**
     * 解析 syscall 子命令（第一个参数），支持字符串常量与已绑定字符串的虚拟寄存器。
     *
     * @param arg 子命令参数（应为字符串常量或寄存器）
     * @param fn  当前函数名（仅用于报错信息）
     * @return 子命令（大写字符串）
     * @throws IllegalStateException 如果参数不是字符串常量或已绑定寄存器
     */
    private String resolveSyscallSubcmd(IRValue arg, String fn) {
        switch (arg) {
            case IRConstant(String s) -> {
                return s.toUpperCase(Locale.ROOT);
            }
            case IRConstant(Object value) -> throw new IllegalStateException(
                    "[CallGenerator] syscall 第一个参数必须是字符串常量 (function: %s, value: %s)"
                            .formatted(fn, value)
            );
            case IRVirtualRegister(int id) -> {
                String s = STRING_CONST_POOL.get(id);
                if (s == null) {
                    throw new IllegalStateException(
                            "[CallGenerator] 未找到 syscall 字符串常量绑定 (function: %s, regId: %d)"
                                    .formatted(fn, id)
                    );
                }
                return s.toUpperCase(Locale.ROOT);
            }
            case null, default -> throw new IllegalStateException(
                    "[CallGenerator] syscall 第一个参数必须是字符串常量或已绑定字符串的寄存器 (function: %s, arg: %s)"
                            .formatted(fn, arg)
            );
        }
    }

    /**
     * 加载一个参数到栈，支持指定默认类型。如果未设置类型则采用默认类型。
     *
     * @param out         VM 程序构建器
     * @param slotMap     寄存器到槽位的映射
     * @param arg         参数值（应为虚拟寄存器）
     * @param defaultType 默认类型
     * @param fn          当前函数名（用于错误提示）
     * @throws IllegalStateException 如果参数不是虚拟寄存器，或槽位未找到
     */
    private void loadArgument(VMProgramBuilder out, Map<IRVirtualRegister, Integer> slotMap, IRValue arg, char defaultType, String fn) {
        if (!(arg instanceof IRVirtualRegister vr)) {
            throw new IllegalStateException(
                    "[CallGenerator] 参数必须为虚拟寄存器 (function: %s, arg: %s)".formatted(fn, arg));
        }
        Integer slot = slotMap.get(vr);
        if (slot == null) {
            throw new IllegalStateException(
                    "[CallGenerator] 未找到虚拟寄存器的槽位映射 (function: %s, reg: %s)".formatted(fn, vr));
        }
        char t = out.getSlotType(slot);
        if (t == '\0') t = defaultType;
        out.emit(OpHelper.opcode(t + "_LOAD") + " " + slot);
    }

    /**
     * 生成一维/多维数组的下标访问指令（支持类型分派）。
     * <p>
     * 本方法用于将 IR 层的数组下标访问表达式（如 arr[idx]），生成对应的 VM 指令序列。
     * 支持 byte/short/int/long/float/double/reference 等所有基础类型的数组元素访问。
     * </p>
     *
     * <ul>
     *   <li>1. 依次加载数组参数（arr）和下标参数（idx）到操作数栈</li>
     *   <li>2. 发出 ARR_GET 系统调用指令（SYSCALL ARR_GET），通过 VM 访问对应元素</li>
     *   <li>3. 根据元素声明类型，将结果写入目标槽位，支持类型分派（B/S/I/L/F/D/R）</li>
     *   <li>4. 若参数或返回寄存器缺失，则抛出异常提示</li>
     * </ul>
     *
     * @param ins     下标访问对应的 IR 调用指令，函数名通常为 __index_x
     * @param out     VM 程序构建器，用于发出 VM 指令
     * @param slotMap IR 虚拟寄存器到 VM 槽位的映射表
     * @param retType 元素类型标识（'B' byte, 'S' short, 'I' int, 'L' long, 'F' float, 'D' double, 'R' ref/obj）
     * @throws IllegalStateException 参数个数不符、缺少目标寄存器、未找到槽位等情况
     */
    private void generateIndexInstruction(CallInstruction ins, VMProgramBuilder out, Map<IRVirtualRegister, Integer> slotMap, char retType) {
        String fn = ins.getFunctionName();
        List<IRValue> args = ins.getArguments();
        if (args.size() != 2) {
            throw new IllegalStateException(
                    "[CallGenerator] %s 需要两个参数(arr, idx)，实际: %s".formatted(fn, args));
        }
        // 加载数组参数（寄存器类型按 R/ref 处理，默认对象槽位）
        loadArgument(out, slotMap, args.get(0), 'R', fn);
        // 加载下标参数（寄存器类型按 I/int 处理）
        loadArgument(out, slotMap, args.get(1), 'I', fn);

        // 发出 ARR_GET 系统调用（元素访问由 VM 完成类型分派）
        out.emit(VMOpCode.SYSCALL + " " + "ARR_GET");

        // 保存返回值到目标寄存器
        IRVirtualRegister dest = ins.getDest();
        if (dest == null) {
            throw new IllegalStateException(
                    "[CallGenerator] %s 需要有目标寄存器用于保存返回值".formatted(fn));
        }
        Integer destSlot = slotMap.get(dest);
        if (destSlot == null) {
            throw new IllegalStateException(
                    "[CallGenerator] %s 未找到目标寄存器的槽位映射 (dest: %s)".formatted(fn, dest));
        }

        // 按元素类型分派写入 VM 槽位
        switch (retType) {
            case 'B' -> {
                out.emit(OpHelper.opcode("B_STORE") + " " + destSlot);
                out.setSlotType(destSlot, 'B');
            }
            case 'S' -> {
                out.emit(OpHelper.opcode("S_STORE") + " " + destSlot);
                out.setSlotType(destSlot, 'S');
            }
            case 'I' -> {
                out.emit(OpHelper.opcode("I_STORE") + " " + destSlot);
                out.setSlotType(destSlot, 'I');
            }
            case 'L' -> {
                out.emit(OpHelper.opcode("L_STORE") + " " + destSlot);
                out.setSlotType(destSlot, 'L');
            }
            case 'F' -> {
                out.emit(OpHelper.opcode("F_STORE") + " " + destSlot);
                out.setSlotType(destSlot, 'F');
            }
            case 'D' -> {
                out.emit(OpHelper.opcode("D_STORE") + " " + destSlot);
                out.setSlotType(destSlot, 'D');
            }
            default -> {
                out.emit(OpHelper.opcode("R_STORE") + " " + destSlot);
                out.setSlotType(destSlot, 'R');
            }
        }
    }

    /**
     * 生成 syscall 指令分支逻辑。
     * <ol>
     *   <li>解析 syscall 子命令</li>
     *   <li>压栈剩余参数</li>
     *   <li>发出 SYSCALL 指令</li>
     *   <li>若有返回值则保存至目标槽位</li>
     * </ol>
     *
     * @param ins     调用指令
     * @param out     VM 程序构建器
     * @param slotMap 寄存器到槽位映射
     * @param fn      当前函数名
     */
    private void generateSyscall(CallInstruction ins, VMProgramBuilder out, Map<IRVirtualRegister, Integer> slotMap, String fn) {
        List<IRValue> args = ins.getArguments();
        if (args.isEmpty()) {
            throw new IllegalStateException(
                    "[CallGenerator] syscall 需要子命令参数 (function: %s)".formatted(fn));
        }

        // 0. 解析 syscall 子命令（第一个参数）
        String subcmd = resolveSyscallSubcmd(args.getFirst(), fn);

        // 1. 压栈其余 syscall 参数（从 index 1 开始）
        for (int i = 1; i < args.size(); i++) {
            loadArgument(out, slotMap, args.get(i), 'R', fn);
        }

        // 2. 生成 SYSCALL 指令
        out.emit(VMOpCode.SYSCALL + " " + subcmd);

        // 3. 有返回值则保存到目标槽位
        IRVirtualRegister dest = ins.getDest();
        if (dest != null) {
            Integer destSlot = slotMap.get(dest);
            if (destSlot == null) {
                throw new IllegalStateException(
                        "[CallGenerator] syscall 未找到目标寄存器的槽位映射 (function: %s, dest: %s)"
                                .formatted(fn, dest));
            }
            out.emit(OpHelper.opcode("I_STORE") + " " + destSlot);
            out.setSlotType(destSlot, 'I');
        }
    }

    /**
     * 生成普通函数调用指令：
     * <ol>
     *   <li>推断返回值类型</li>
     *   <li>压栈所有参数</li>
     *   <li>生成 CALL 指令</li>
     *   <li>保存返回值（若非 void）</li>
     * </ol>
     *
     * @param ins     调用指令
     * @param out     VM 程序构建器
     * @param slotMap 寄存器到槽位映射
     * @param fn      当前函数名
     */
    private void generateNormalCall(CallInstruction ins, VMProgramBuilder out, Map<IRVirtualRegister, Integer> slotMap, String fn) {
        // 1. 推断返回值类型（首字母大写，缺省为 'I'）
        String retTypeName = GlobalFunctionTable.getReturnType(fn);
        char retType = (retTypeName != null && !retTypeName.isEmpty()) ? Character.toUpperCase(retTypeName.charAt(0)) : 'I';

        // 2. 压栈所有参数
        for (IRValue arg : ins.getArguments()) {
            loadArgument(out, slotMap, arg, 'R', fn);
        }

        // 3. 发出 CALL 指令
        out.emitCall(fn, ins.getArguments().size());

        // 3.5 void 返回直接结束
        if ("void".equals(retTypeName)) {
            return;
        }

        // 4. 保存返回值到目标寄存器
        IRVirtualRegister dest = ins.getDest();
        if (dest == null) {
            throw new IllegalStateException(
                    "[CallGenerator] 普通函数调用未找到目标寄存器 (function: %s)".formatted(fn));
        }
        Integer destSlot = slotMap.get(dest);
        if (destSlot == null) {
            throw new IllegalStateException(
                    "[CallGenerator] 普通函数调用未找到目标寄存器的槽位映射 (function: %s, dest: %s)".formatted(fn, dest));
        }
        out.emit(OpHelper.opcode(retType + "_STORE") + " " + destSlot);
        out.setSlotType(destSlot, retType);
    }
}
