package org.jcnc.snow.vm.commands.system.control;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.engine.VMExitSignal;
import org.jcnc.snow.vm.engine.SyscallTable;
import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;
import org.jcnc.snow.vm.runtime.HeapObject;
import org.jcnc.snow.vm.runtime.HeapObjectKind;
import org.jcnc.snow.vm.runtime.SnowPanicException;
import org.jcnc.snow.vm.runtime.SnowRuntime;
import org.jcnc.snow.vm.value.ByteValue;
import org.jcnc.snow.vm.value.DoubleValue;
import org.jcnc.snow.vm.value.FloatValue;
import org.jcnc.snow.vm.value.IntValue;
import org.jcnc.snow.vm.value.LongValue;
import org.jcnc.snow.vm.value.RefValue;
import org.jcnc.snow.vm.value.ShortValue;
import org.jcnc.snow.vm.value.Value;

import java.util.Locale;

/**
 * {@code SyscallCommand} 实现虚拟机系统调用分发器，负责根据系统调用 opcode 路由到对应的 {@link SyscallHandler} 实现。
 * <p>
 * 用于在虚拟机指令流中处理所有系统调用相关的操作，并统一管理异常处理和错误状态记录。
 *
 * <p><b>工作流程：</b></p>
 * <ol>
 *   <li>从指令参数解析出 syscall opcode（支持 16 进制或 10 进制字符串）</li>
 *   <li>根据 opcode 查找 {@link SyscallHandler}</li>
 *   <li><b>强制校验</b>操作数栈上可用参数数量与 {@link SyscallTable.SyscallSpec#args()} 声明的期望参数个数是否严格匹配</li>
 *   <li>参数个数不匹配时，立即抛出 {@link SnowPanicException}，拒绝执行 handler</li>
 *   <li><b>强制校验</b>每个参数的 Value 类别是否与 ABI 声明匹配（I32/I64/F64/REF 等）</li>
 *   <li>对 REF 类型参数，验证 HeapObjectKind 是否与期望的 STRING/BYTES/ARRAY/DICT/STRUCT 匹配</li>
 *   <li>调用 handler 处理，成功时清除全局 errno/errstr，失败时压入错误并记录异常信息</li>
 * </ol>
 *
 * <p><b>异常管理：</b></p>
 * <ul>
 *   <li>如果指令参数不足，直接压入参数错误并返回</li>
 *   <li>如果 opcode 解析失败，抛出 {@link IllegalArgumentException}</li>
 *   <li><b>如果操作数栈上可用参数个数与 ABI 声明不匹配，抛出 {@link SnowPanicException}（VM 在 handler 执行前拒绝）</b></li>
 *   <li>系统调用 handler 抛出异常时，自动通过 {@link SyscallUtils#pushErr} 压入 -1 并记录错误串</li>
 * </ul>
 *
 * <p><b>返回：</b>始终返回下一个指令位置 {@code pc + 1}</p>
 */
public class SyscallCommand implements Command {

    /**
     * 执行系统调用分发。
     *
     * @param parts     指令参数（parts[1] 必须为 syscall opcode，支持 "0x..." 格式）
     * @param pc        当前程序计数器
     * @param stack     虚拟机操作数栈
     * @param locals    当前方法的本地变量表
     * @param callStack 当前调用栈
     * @return 下一个指令位置（pc + 1）
     * @throws IllegalArgumentException opcode 格式非法时抛出
     */
    @Override
    public int execute(String[] parts,
                       int pc,
                       OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) {

        if (parts.length < 2) {
            SyscallUtils.pushErr(stack, new IllegalArgumentException("Missing syscall opcode"));
            return pc + 1;
        }

        int opcode = SyscallTable.resolveOpcode(parts[1]);

        SyscallHandler handler = SyscallFactory.getHandler(opcode);
        int before = stack.size();

        try {
            SyscallTable.SyscallSpec spec = SyscallTable.spec(opcode);
            if (spec == null) {
                throw new SnowPanicException("No ABI spec registered for syscall opcode: 0x"
                        + Integer.toHexString(opcode).toUpperCase(Locale.ROOT)
                        + " (register it in SyscallTable before execution)");
            }
            String name = spec.name();
            
            // 强制校验参数个数（在调用 handler 前）
            int expectedArgCount = (spec.args() == null) ? 0 : spec.args().length;
            int availableArgCount = before; // before = stack.size() before handler execution
            if (availableArgCount < expectedArgCount) {
                throw new SnowPanicException("Syscall " + name + " (0x"
                        + Integer.toHexString(opcode).toUpperCase(Locale.ROOT)
                        + ") requires " + expectedArgCount + " argument(s), but only "
                        + availableArgCount + " available on stack");
            }
            if (availableArgCount > expectedArgCount) {
                throw new SnowPanicException("Syscall " + name + " (0x"
                        + Integer.toHexString(opcode).toUpperCase(Locale.ROOT)
                        + ") requires " + expectedArgCount + " argument(s), but "
                        + availableArgCount + " provided on stack");
            }
            
            // 强制校验参数类别（Value 层，不靠类型系统）
            validateArguments(spec, stack, opcode);
            
            try (AutoCloseable ignored = SnowRuntime.get().enterSyscall(opcode, name, handler.getClass().getName())) {
                handler.handle(stack, locals, callStack);
                // 成功时重置 errno/errstr
                SyscallUtils.clearErr();
                validateReturn(opcode, before, stack);
            }
        } catch (VMExitSignal exit) {
            // EXIT is a control-flow syscall: it terminates the Snow process/VM without being treated as an error.
            throw exit;
        } catch (SnowPanicException e) {
            throw e;
        } catch (Exception e) {
            if (isRuntimeBuiltin(opcode)) {
                SyscallTable.SyscallSpec spec = SyscallTable.spec(opcode);
                // spec cannot be null here since we already validated above
                String name = spec.name();
                throw new SnowPanicException("Runtime builtin syscall failed: " + name, e);
            }
            // 失败路径：记录 errno/errstr，并根据 ABI 返回类型压入“同类别”的失败哨兵值（或不返回值）。
            SyscallTable.SyscallSpec spec = SyscallTable.spec(opcode);
            // spec cannot be null here since we already validated above
            SyscallUtils.recordErr(e);
                    
            // 检查失败时的栈状态：必须严格消耗所有参数
            int afterFailure = stack.size();
            int argCount = (spec.args() == null) ? 0 : spec.args().length;
            int expectedAfterConsume = before - argCount;
            if (afterFailure != expectedAfterConsume) {
                throw new SnowPanicException("Syscall " + spec.name() + " (0x"
                        + Integer.toHexString(opcode).toUpperCase(Locale.ROOT)
                        + ") failed with incorrect stack consumption: expected to consume "
                        + argCount + " args (stack size " + before + " -> " + expectedAfterConsume
                        + "), but actual stack size is " + afterFailure
                        + ". Handler must consume all args even on failure.");
            }
                    
            // 压入失败哨兵值
            switch (spec.ret()) {
                case VOID -> {
                    // no return value
                }
                case I8 -> stack.pushValue(new ByteValue((byte) -1));
                case I16 -> stack.pushValue(new ShortValue((short) -1));
                case I32 -> stack.pushValue(new IntValue(-1));
                case I64 -> stack.pushValue(new LongValue(-1));
                case F32 -> stack.pushValue(new FloatValue(-1.0f));
                case F64 -> stack.pushValue(new DoubleValue(-1.0d));
                case STRING, BYTES, ARRAY, DICT, STRUCT, ANY -> stack.pushValue(Value.NULL);
            }
        }

        return pc + 1;
    }

    private static boolean isRuntimeBuiltin(int opcode) {
        // Array builtins live under 0x18xx; string/bytes builtins under 0x1Axx.
        return (opcode >= 0x1800 && opcode <= 0x18FF) || (opcode >= 0x1A00 && opcode <= 0x1A1F);
    }

    /**
     * 强制校验 syscall 参数类别（Value 层校验，不依赖类型系统）。
     * 
     * <p>此方法在调用 handler 前检查每个参数的 Value 类别是否与 ABI 声明匹配。</p>
     * 
     * <p><b>校验规则：</b></p>
     * <ul>
     *   <li>I8/I16/I32/I64/F32/F64 → 检查 Value 子类</li>
     *   <li>REF 类型 → 必须是 RefValue，并检查 HeapObjectKind</li>
     *   <li>STRING/BYTES/ARRAY/DICT/STRUCT → 检查 HeapObjectKind 是否匹配</li>
     *   <li>ANY → 允许任意类型（但不推荐，标记为 unsafe）</li>
     * </ul>
     * 
     * @param spec 系统调用 ABI 规范
     * @param stack 操作数栈
     * @param opcode 系统调用 opcode
     * @throws SnowPanicException 参数类型不匹配时抛出
     */
    private static void validateArguments(SyscallTable.SyscallSpec spec, OperandStack stack, int opcode) {
        if (spec.args() == null || spec.args().length == 0) {
            return; // 无参数，无需校验
        }
        
        // 从栈顶往下获取参数（不 pop，只 peek）
        // 注意：栈上参数顺序是反的，最后一个参数在栈顶
        SyscallTable.AbiType[] argTypes = spec.args();
        int stackSize = stack.size();
        
        for (int i = 0; i < argTypes.length; i++) {
            // 栈上第 i 个参数的位置（从栈顶往下数）
            int stackIndex = stackSize - argTypes.length + i;
            Value argValue = peekAt(stack, stackIndex);
            SyscallTable.AbiType expectedType = argTypes[i];
            
            validateSingleArgument(spec.name(), i, argValue, expectedType, opcode);
        }
    }
    
    /**
     * 从栈中获取指定位置的 Value（不修改栈）。
     * 
     * @param stack 操作数栈
     * @param index 从栈底开始的索引（0-based）
     * @return 指定位置的 Value
     */
    private static Value peekAt(OperandStack stack, int index) {
        // 保存当前栈状态
        java.util.Deque<Value> tempStack = new java.util.ArrayDeque<>();
        int size = stack.size();
        
        // pop 到目标位置
        for (int i = 0; i < size - index - 1; i++) {
            tempStack.push(stack.popValue());
        }
        
        // 获取目标 Value
        Value target = stack.peekValue();
        
        // 恢复栈
        while (!tempStack.isEmpty()) {
            stack.pushValue(tempStack.pop());
        }
        
        return target;
    }
    
    /**
     * 校验单个参数的类型。
     * 
     * @param syscallName 系统调用名称
     * @param argIndex 参数索引（0-based）
     * @param argValue 参数值
     * @param expectedType 期望的类型
     * @param opcode 系统调用 opcode
     * @throws SnowPanicException 类型不匹配时抛出
     */
    private static void validateSingleArgument(String syscallName, int argIndex, 
                                                Value argValue, SyscallTable.AbiType expectedType, 
                                                int opcode) {
        switch (expectedType) {
            case I8 -> {
                if (!(argValue instanceof ByteValue)) {
                    throw new SnowPanicException("Syscall " + syscallName + " (0x"
                            + Integer.toHexString(opcode).toUpperCase(Locale.ROOT)
                            + ") arg[" + argIndex + "] expected I8 (ByteValue), but got "
                            + argValue.getClass().getSimpleName());
                }
            }
            case I16 -> {
                if (!(argValue instanceof ShortValue)) {
                    throw new SnowPanicException("Syscall " + syscallName + " (0x"
                            + Integer.toHexString(opcode).toUpperCase(Locale.ROOT)
                            + ") arg[" + argIndex + "] expected I16 (ShortValue), but got "
                            + argValue.getClass().getSimpleName());
                }
            }
            case I32 -> {
                if (!(argValue instanceof IntValue)) {
                    throw new SnowPanicException("Syscall " + syscallName + " (0x"
                            + Integer.toHexString(opcode).toUpperCase(Locale.ROOT)
                            + ") arg[" + argIndex + "] expected I32 (IntValue), but got "
                            + argValue.getClass().getSimpleName());
                }
            }
            case I64 -> {
                if (!(argValue instanceof LongValue)) {
                    throw new SnowPanicException("Syscall " + syscallName + " (0x"
                            + Integer.toHexString(opcode).toUpperCase(Locale.ROOT)
                            + ") arg[" + argIndex + "] expected I64 (LongValue), but got "
                            + argValue.getClass().getSimpleName());
                }
            }
            case F32 -> {
                if (!(argValue instanceof FloatValue)) {
                    throw new SnowPanicException("Syscall " + syscallName + " (0x"
                            + Integer.toHexString(opcode).toUpperCase(Locale.ROOT)
                            + ") arg[" + argIndex + "] expected F32 (FloatValue), but got "
                            + argValue.getClass().getSimpleName());
                }
            }
            case F64 -> {
                if (!(argValue instanceof DoubleValue)) {
                    throw new SnowPanicException("Syscall " + syscallName + " (0x"
                            + Integer.toHexString(opcode).toUpperCase(Locale.ROOT)
                            + ") arg[" + argIndex + "] expected F64 (DoubleValue), but got "
                            + argValue.getClass().getSimpleName());
                }
            }
            case STRING, BYTES, ARRAY, DICT, STRUCT -> {
                // REF 类型：必须是 RefValue
                if (!(argValue instanceof RefValue(int id))) {
                    throw new SnowPanicException("Syscall " + syscallName + " (0x"
                            + Integer.toHexString(opcode).toUpperCase(Locale.ROOT)
                            + ") arg[" + argIndex + "] expected REF (RefValue), but got "
                            + argValue.getClass().getSimpleName());
                }
                
                // 检查 HeapObjectKind
                HeapObject obj = SnowRuntime.get().heap().get(id);
                HeapObjectKind actualKind = obj.kind();
                HeapObjectKind expectedKind = switch (expectedType) {
                    case STRING -> HeapObjectKind.STRING;
                    case BYTES -> HeapObjectKind.BYTES;
                    case ARRAY -> HeapObjectKind.ARRAY;
                    case DICT -> HeapObjectKind.DICT;
                    case STRUCT -> HeapObjectKind.STRUCT;
                    default -> throw new SnowPanicException("unreachable");
                };
                
                if (actualKind != expectedKind) {
                    throw new SnowPanicException("Syscall " + syscallName + " (0x"
                            + Integer.toHexString(opcode).toUpperCase(Locale.ROOT)
                            + ") arg[" + argIndex + "] expected HeapObjectKind." + expectedKind
                            + ", but got HeapObjectKind." + actualKind);
                }
            }
            case ANY -> {
                // ANY 允许任意类型，但这是 unsafe 的
                // 未来可以考虑警告或记录
            }
            case VOID -> {
                throw new SnowPanicException("Syscall " + syscallName + " (0x"
                        + Integer.toHexString(opcode).toUpperCase(Locale.ROOT)
                        + ") has VOID in args, which is invalid");
            }
        }
    }

    private static void validateReturn(int opcode, int beforeSize, OperandStack stack) {
        if (SyscallUtils.getErrno() != 0) return;
        SyscallTable.SyscallSpec spec = SyscallTable.spec(opcode);
        // spec must not be null (already validated in execute())
        if (spec == null) {
            throw new SnowPanicException("validateReturn: spec is null for opcode 0x"
                    + Integer.toHexString(opcode).toUpperCase(Locale.ROOT)
                    + " (should have been caught earlier)");
        }
        int argCount = (spec.args() == null) ? 0 : spec.args().length;
        int expectedDelta = (spec.ret() == SyscallTable.AbiType.VOID ? 0 : 1) - argCount;
        int actualDelta = stack.size() - beforeSize;
        if (actualDelta != expectedDelta) {
            throw new org.jcnc.snow.vm.runtime.SnowPanicException("Syscall ABI violation: " + spec.name()
                    + " expected stack delta=" + expectedDelta + " (args=" + argCount + ", ret=" + spec.ret()
                    + ") but got " + actualDelta);
        }
        if (spec.ret() == SyscallTable.AbiType.VOID) return;
        Value top = stack.peekValue();
        switch (spec.ret()) {
            case I8 -> {
                if (!(top instanceof org.jcnc.snow.vm.value.ByteValue)) {
                    throw new org.jcnc.snow.vm.runtime.SnowPanicException("Syscall ABI violation: " + spec.name() + " expected byte");
                }
            }
            case I16 -> {
                if (!(top instanceof org.jcnc.snow.vm.value.ShortValue)) {
                    throw new org.jcnc.snow.vm.runtime.SnowPanicException("Syscall ABI violation: " + spec.name() + " expected short");
                }
            }
            case I32 -> {
                if (!(top instanceof org.jcnc.snow.vm.value.IntValue)) {
                    throw new org.jcnc.snow.vm.runtime.SnowPanicException("Syscall ABI violation: " + spec.name() + " expected int");
                }
            }
            case I64 -> {
                if (!(top instanceof org.jcnc.snow.vm.value.LongValue)) {
                    throw new org.jcnc.snow.vm.runtime.SnowPanicException("Syscall ABI violation: " + spec.name() + " expected long");
                }
            }
            case F32 -> {
                if (!(top instanceof org.jcnc.snow.vm.value.FloatValue)) {
                    throw new org.jcnc.snow.vm.runtime.SnowPanicException("Syscall ABI violation: " + spec.name() + " expected float");
                }
            }
            case F64 -> {
                if (!(top instanceof org.jcnc.snow.vm.value.DoubleValue)) {
                    throw new org.jcnc.snow.vm.runtime.SnowPanicException("Syscall ABI violation: " + spec.name() + " expected double");
                }
            }
            case STRING, BYTES, ARRAY, DICT, STRUCT -> {
                if (!(top instanceof RefValue(int id))) {
                    throw new org.jcnc.snow.vm.runtime.SnowPanicException("Syscall ABI violation: " + spec.name() + " expected ref");
                }
                HeapObject obj = SnowRuntime.get().heap().get(id);
                HeapObjectKind kind = obj.kind();
                HeapObjectKind expected = switch (spec.ret()) {
                    case STRING -> HeapObjectKind.STRING;
                    case BYTES -> HeapObjectKind.BYTES;
                    case ARRAY -> HeapObjectKind.ARRAY;
                    case DICT -> HeapObjectKind.DICT;
                    case STRUCT -> HeapObjectKind.STRUCT;
                    default -> throw new org.jcnc.snow.vm.runtime.SnowPanicException("unreachable");
                };
                if (kind != expected) {
                    throw new org.jcnc.snow.vm.runtime.SnowPanicException("Syscall ABI violation: " + spec.name()
                            + " expected " + expected + " but got " + kind);
                }
            }
            case ANY -> {
            }
            case VOID -> {
            }
        }
    }
}
