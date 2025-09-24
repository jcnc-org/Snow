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
 * 负责将 IR 层的 {@link CallInstruction} 转换为 VM 层的函数调用指令。
 * <p>
 * 本类实现如下功能：
 * <ul>
 *   <li>处理 VM 层的函数调用，包括：</li>
 *   <ul>
 *     <li>系统调用（syscall），支持子命令字符串常量池</li>
 *     <li>内置数组元素访问和赋值（如 <code>__index_* / __setindex_*</code>）</li>
 *     <li>普通函数调用，根据返回类型生成对应的 STORE 指令</li>
 *   </ul>
 *   <li>支持字符串常量的注册与查找，用于 syscall 子命令参数优化</li>
 *   <li>类型前缀统一映射，适配 VM 指令集</li>
 * </ul>
 */
public class CallGenerator implements InstructionGenerator<CallInstruction> {

    /**
     * 字符串常量池。
     * 用于存储 syscall 子命令所需的字符串常量，key 为虚拟寄存器 ID，value 为字符串常量。
     */
    private static final Map<Integer, String> STRING_CONST_POOL = new ConcurrentHashMap<>();

    /**
     * 最近一次生成时所处的函数名，可用于调试和错误信息。
     */
    private String fn;

    /**
     * 注册一个字符串常量到常量池，用于 syscall 子命令参数。
     *
     * @param regId 虚拟寄存器 ID
     * @param value 字符串常量内容
     */
    public static void registerStringConst(int regId, String value) {
        STRING_CONST_POOL.put(regId, value);
    }

    /**
     * 将类型名（如 "int", "double", "string"）映射为 VM 指令集的类型前缀字符。
     * 引用类型与自定义类型统一映射为 'R'。
     *
     * @param name 类型名称
     * @return 类型前缀字符，如 'I', 'D', 'R' 等
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
            case "string" -> 'R';
            case "void" -> 'V';

            default -> 'R';
        };
    }

    /**
     * 获取本生成器支持的 IR 指令类型。
     *
     * @return {@link CallInstruction}.class
     */
    @Override
    public Class<CallInstruction> supportedClass() {
        return CallInstruction.class;
    }

    /**
     * 将一条 IR 层函数调用指令生成对应的 VM 层指令。
     * <p>
     * 支持三类调用：
     * <ul>
     *   <li>系统调用（syscall）</li>
     *   <li>内置数组索引/赋值</li>
     *   <li>普通函数调用</li>
     * </ul>
     *
     * @param ins       IR 调用指令
     * @param out       VM 程序构建器
     * @param slotMap   IR 虚拟寄存器到 VM 槽位映射
     * @param currentFn 当前处理的函数名（调试用）
     */
    @Override
    public void generate(CallInstruction ins, VMProgramBuilder out, Map<IRVirtualRegister, Integer> slotMap, String currentFn) {
        String fn = ins.getFunctionName();

        // 1. 处理 syscall
        if ("syscall".equals(fn) || fn.endsWith(".syscall")) {
            generateSyscall(ins, out, slotMap, fn);
            return;
        }

        // 2. 处理内置数组读取和写入指令
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

        // 3. 其余为普通函数调用
        generateNormalCall(ins, out, slotMap, fn);
    }

    /**
     * 生成数组元素赋值的 VM 指令。参数个数应为 3（数组、索引、值）。
     *
     * @param ins     IR 调用指令
     * @param out     VM 指令构建器
     * @param slotMap 虚拟寄存器到槽位映射
     * @param valType 元素类型前缀
     */
    private void generateSetIndexInstruction(CallInstruction ins, VMProgramBuilder out, Map<IRVirtualRegister, Integer> slotMap, char valType) {
        List<IRValue> args = ins.getArguments();
        if (args.size() != 3) throw new IllegalStateException("[CallGenerator] __setindex_* 需要三个参数");

        loadArgument(out, slotMap, args.get(0), 'R', ins.getFunctionName());
        loadArgument(out, slotMap, args.get(1), 'I', ins.getFunctionName());
        loadArgument(out, slotMap, args.get(2), valType, ins.getFunctionName());
        // __setindex_* 映射到 VM 的 ARR_SET 对应的 syscall
        out.emit(VMOpCode.SYSCALL + " 0x1803");
    }

    /**
     * 生成数组元素读取的 VM 指令。参数个数应为 2（数组、索引）。
     * 结果写回到目标寄存器，根据类型选择合适的 STORE 指令。
     *
     * @param ins     IR 调用指令
     * @param out     VM 指令构建器
     * @param slotMap 虚拟寄存器到槽位映射
     * @param retType 返回值类型前缀
     */
    private void generateIndexInstruction(CallInstruction ins, VMProgramBuilder out, Map<IRVirtualRegister, Integer> slotMap, char retType) {
        String fn = ins.getFunctionName();
        List<IRValue> args = ins.getArguments();
        if (args.size() != 2) throw new IllegalStateException("[CallGenerator] " + fn + " 需要两个参数");

        loadArgument(out, slotMap, args.get(0), 'R', fn);
        loadArgument(out, slotMap, args.get(1), 'I', fn);

        out.emit(VMOpCode.SYSCALL + " 0x1802");

        IRVirtualRegister dest = ins.getDest();
        if (dest == null) throw new IllegalStateException("[CallGenerator] " + fn + " 必须有返回值寄存器");

        Integer slot = slotMap.get(dest);
        if (slot == null) throw new IllegalStateException("[CallGenerator] " + fn + " 未找到目标槽位");

        out.emit(OpHelper.opcode(retType + "_STORE") + " " + slot);
        out.setSlotType(slot, retType);
    }

    /**
     * 判断 syscall 子命令返回值类型的前缀。
     *
     * @param subcmd syscall 子命令字符串
     * @return 类型前缀，'R'（引用型）、'L'（长整型）或 'I'（整型）
     */
    private char syscallReturnPrefix(String subcmd) {
        String s = subcmd.toUpperCase(Locale.ROOT);
        return switch (s) {
            // 返回引用（字符串/字节数组/Map/数组等）
            case "0X1802", "ARR_GET",
                 "0X1001", "READ",
                 "0X1005", "STAT",
                 "0X1006", "FSTAT",
                 "0X1904", "ERRSTR",
                 "0X1200", "STDIN_READ",
                 "0X100A", "PIPE",
                 "0X1010", "READLINK",
                 "0X1103", "GETCWD",
                 "0X1104", "READDIR",
                 "0X1300", "SELECT",
                 "0X1303", "EPOLL_WAIT",
                 "0X1304", "IO_WAIT",
                 "0X1406", "RECV",
                 "0X1408", "RECVFROM",
                 "0X140C", "GETPEERNAME",
                 "0X140D", "GETSOCKNAME",
                 "0X140E", "GETADDRINFO",
                 "0X140B","GETSOCKOPT"
                    -> 'R';


            // 返回 long
            case "0X1003", "SEEK",
                 "0X1700", "CLOCK_GETTIME",
                 "0X1703", "TICK_MS" -> 'L';

            // 默认返回 int
            default -> 'I';
        };
    }

    /**
     * 生成 syscall 的 VM 指令。首参数为子命令字符串，剩余参数以引用形式压栈。
     * 若有返回值，根据子命令选择返回类型和 STORE 指令。
     *
     * @param ins     IR 调用指令
     * @param out     VM 指令构建器
     * @param slotMap 虚拟寄存器到槽位映射
     * @param fn      函数名
     */
    private void generateSyscall(CallInstruction ins, VMProgramBuilder out, Map<IRVirtualRegister, Integer> slotMap, String fn) {
        List<IRValue> args = ins.getArguments();
        if (args.isEmpty()) throw new IllegalStateException("[CallGenerator] syscall 至少需要一个子命令");

        String subcmd = resolveSyscallSubcmd(args.getFirst(), fn);

        // 压参数（全按引用）
        for (int i = 1; i < args.size(); i++) {
            loadArgument(out, slotMap, args.get(i), 'R', fn);
        }

        // 发出 SYSCALL
        out.emit(VMOpCode.SYSCALL + " " + subcmd);

        // 处理返回值
        IRVirtualRegister dest = ins.getDest();
        if (dest != null) {
            Integer slot = slotMap.get(dest);
            if (slot == null) throw new IllegalStateException("[CallGenerator] syscall 未找到目标槽位");
            char p = syscallReturnPrefix(subcmd);
            switch (p) {
                case 'R' -> out.emit(OpHelper.opcode("R_STORE") + " " + slot);
                case 'L' -> out.emit(OpHelper.opcode("L_STORE") + " " + slot);
                default -> out.emit(OpHelper.opcode("I_STORE") + " " + slot);
            }
            out.setSlotType(slot, p);
        }
    }

    /**
     * 解析 syscall 子命令参数。
     * 支持 IRConstant（直接字符串）和 IRVirtualRegister（常量池查找）。
     *
     * @param arg 第一个参数，子命令字符串
     * @param fn  所属函数名
     * @return 返回大写子命令字符串
     * @throws IllegalStateException 如果类型不符或常量池查找失败
     */
    private String resolveSyscallSubcmd(IRValue arg, String fn) {
        if (arg instanceof IRConstant(Object value)) {
            if (value instanceof String s) return s.toUpperCase(Locale.ROOT);
            throw new IllegalStateException("[CallGenerator] syscall 子命令必须是字符串");
        }
        if (arg instanceof IRVirtualRegister(int id)) {
            String s = STRING_CONST_POOL.get(id);
            if (s == null) throw new IllegalStateException("[CallGenerator] 未注册的 syscall 字符串常量");
            return s.toUpperCase(Locale.ROOT);
        }
        throw new IllegalStateException("[CallGenerator] syscall 子命令参数错误: " + arg);
    }

    /**
     * 生成普通函数调用指令。
     * 参数全部以引用类型压栈，调用完成后根据返回类型选择 STORE 指令保存结果。
     *
     * @param ins     IR 调用指令
     * @param out     VM 指令构建器
     * @param slotMap 虚拟寄存器到槽位映射
     * @param fn      被调用函数名称
     */
    private void generateNormalCall(CallInstruction ins, VMProgramBuilder out, Map<IRVirtualRegister, Integer> slotMap, String fn) {
        String retTypeName = GlobalFunctionTable.getReturnType(fn);
        char retType = normalizeTypePrefix(retTypeName);

        for (IRValue arg : ins.getArguments()) {
            loadArgument(out, slotMap, arg, 'R', fn);
        }
        out.emitCall(fn, ins.getArguments().size());

        if ("void".equals(retTypeName)) return;

        IRVirtualRegister dest = ins.getDest();
        if (dest == null) throw new IllegalStateException("[CallGenerator] 普通调用缺少返回寄存器");
        Integer slot = slotMap.get(dest);
        if (slot == null) throw new IllegalStateException("[CallGenerator] 未找到返回槽位");

        out.emit(OpHelper.opcode(retType + "_STORE") + " " + slot);
        out.setSlotType(slot, retType);
    }

    /**
     * 加载参数寄存器到操作数栈。
     * 检查参数类型，取槽位和类型，不存在时采用 defaultType。
     *
     * @param out         VM 指令构建器
     * @param slotMap     虚拟寄存器到槽位映射
     * @param arg         参数
     * @param defaultType 默认类型前缀
     * @param fn          所属函数名
     * @throws IllegalStateException 如果类型或槽位未找到
     */
    private void loadArgument(VMProgramBuilder out, Map<IRVirtualRegister, Integer> slotMap, IRValue arg, char defaultType, String fn) {
        this.fn = fn;
        if (!(arg instanceof IRVirtualRegister vr))
            throw new IllegalStateException("[CallGenerator] 参数必须是 IRVirtualRegister");

        Integer slot = slotMap.get(vr);
        if (slot == null) throw new IllegalStateException("[CallGenerator] 未找到参数槽位");

        char t = out.getSlotType(slot);
        if (t == '\0' || (t == 'I' && defaultType != 'I')) {
            t = defaultType;
        }
        out.emit(OpHelper.opcode(t + "_LOAD") + " " + slot);
    }

    /**
     * 获取最近一次处理的函数名。
     *
     * @return 函数名字符串
     */
    public String getFn() {
        return fn;
    }
}
