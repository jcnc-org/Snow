package org.jcnc.snow.vm.commands.system.control;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.engine.SyscallTable;
import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;
import org.jcnc.snow.vm.runtime.HeapObject;
import org.jcnc.snow.vm.runtime.HeapObjectKind;
import org.jcnc.snow.vm.runtime.SnowPanicException;
import org.jcnc.snow.vm.runtime.SnowRuntime;
import org.jcnc.snow.vm.value.RefValue;
import org.jcnc.snow.vm.value.Value;

/**
 * {@code SyscallCommand} 实现虚拟机系统调用分发器，负责根据系统调用 opcode 路由到对应的 {@link SyscallHandler} 实现。
 * <p>
 * 用于在虚拟机指令流中处理所有系统调用相关的操作，并统一管理异常处理和错误状态记录。
 *
 * <p><b>工作流程：</b></p>
 * <ol>
 *   <li>从指令参数解析出 syscall opcode（支持 16 进制或 10 进制字符串）</li>
 *   <li>根据 opcode 查找 {@link SyscallHandler}</li>
 *   <li>调用 handler 处理，成功时清除全局 errno/errstr，失败时压入错误并记录异常信息</li>
 * </ol>
 *
 * <p><b>异常管理：</b></p>
 * <ul>
 *   <li>如果指令参数不足，直接压入参数错误并返回</li>
 *   <li>如果 opcode 解析失败，抛出 {@link IllegalArgumentException}</li>
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

        try {
            int before = stack.size();
            SyscallTable.SyscallSpec spec = SyscallTable.spec(opcode);
            String name = spec == null ? String.format("0x%04X", opcode) : spec.name();
            try (AutoCloseable ignored = SnowRuntime.get().enterSyscall(opcode, name, handler.getClass().getName())) {
                handler.handle(stack, locals, callStack);
                // 成功时重置 errno/errstr
                SyscallUtils.clearErr();
                validateReturn(opcode, before, stack);
            }
        } catch (SnowPanicException e) {
            throw e;
        } catch (Exception e) {
            if (isRuntimeBuiltin(opcode)) {
                SyscallTable.SyscallSpec spec = SyscallTable.spec(opcode);
                String name = spec == null ? String.format("0x%04X", opcode) : spec.name();
                throw new SnowPanicException("Runtime builtin syscall failed: " + name, e);
            }
            // 失败时压入 -1（int）并记录错误串
            SyscallUtils.pushErr(stack, e);
        }

        return pc + 1;
    }

    private static boolean isRuntimeBuiltin(int opcode) {
        // Array builtins live under 0x18xx; string/bytes builtins under 0x1Axx.
        return (opcode >= 0x1800 && opcode <= 0x18FF) || (opcode >= 0x1A00 && opcode <= 0x1A1F);
    }

    private static void validateReturn(int opcode, int beforeSize, OperandStack stack) {
        if (SyscallUtils.getErrno() != 0) return;
        SyscallTable.SyscallSpec spec = SyscallTable.spec(opcode);
        if (spec == null) return;
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
            case STRING, BYTES, ARRAY, DICT -> {
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