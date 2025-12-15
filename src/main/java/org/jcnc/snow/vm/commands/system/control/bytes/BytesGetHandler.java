package org.jcnc.snow.vm.commands.system.control.bytes;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;
import org.jcnc.snow.vm.runtime.SnowBytesObject;
import org.jcnc.snow.vm.runtime.SnowRuntime;
import org.jcnc.snow.vm.value.ByteValue;
import org.jcnc.snow.vm.value.IntValue;
import org.jcnc.snow.vm.value.RefValue;
import org.jcnc.snow.vm.value.Value;

public final class BytesGetHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) {
        Value idxV = stack.popValue();
        Value bytesV = stack.popValue();

        int idx = switch (idxV) {
            case IntValue(int i) -> i;
            case ByteValue(byte b) -> b;
            default -> throw new IllegalArgumentException("BYTES_GET: index must be int");
        };

        if (!(bytesV instanceof RefValue(int id))) {
            throw new IllegalArgumentException("BYTES_GET: expected bytes");
        }
        var obj = SnowRuntime.get().heap().get(id);
        if (!(obj instanceof SnowBytesObject bytes)) {
            throw new IllegalArgumentException("BYTES_GET: expected bytes");
        }
        stack.pushValue(new ByteValue(bytes.get(idx)));
    }
}