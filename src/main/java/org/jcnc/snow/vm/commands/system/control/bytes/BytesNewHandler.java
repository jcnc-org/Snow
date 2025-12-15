package org.jcnc.snow.vm.commands.system.control.bytes;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;
import org.jcnc.snow.vm.runtime.SnowBytesObject;
import org.jcnc.snow.vm.runtime.SnowRuntime;
import org.jcnc.snow.vm.value.ByteValue;
import org.jcnc.snow.vm.value.IntValue;
import org.jcnc.snow.vm.value.LongValue;
import org.jcnc.snow.vm.value.RefValue;
import org.jcnc.snow.vm.value.ShortValue;
import org.jcnc.snow.vm.value.Value;

public final class BytesNewHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) {
        Value nV = stack.popValue();
        int n = switch (nV) {
            case IntValue(int i) -> i;
            case ShortValue(short s) -> s;
            case ByteValue(byte b) -> b;
            case LongValue(long l) -> (int) l;
            default -> throw new IllegalArgumentException("BYTES_NEW: n must be int");
        };
        if (n < 0) throw new IllegalArgumentException("BYTES_NEW: n must be >= 0");
        int id = SnowRuntime.get().heap().alloc(new SnowBytesObject(new byte[n]));
        stack.pushValue(new RefValue(id));
    }
}