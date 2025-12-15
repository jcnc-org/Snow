package org.jcnc.snow.vm.commands.system.control.bytes;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;
import org.jcnc.snow.vm.runtime.SnowBytesObject;
import org.jcnc.snow.vm.runtime.SnowRuntime;
import org.jcnc.snow.vm.value.RefValue;
import org.jcnc.snow.vm.value.Value;

public final class BytesLenHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) {
        Value v = stack.popValue();
        if (v == Value.NULL) {
            stack.pushValue(new org.jcnc.snow.vm.value.IntValue(0));
            return;
        }
        if (!(v instanceof RefValue(int id))) {
            throw new IllegalArgumentException("BYTES_LEN: expected bytes");
        }
        var obj = SnowRuntime.get().heap().get(id);
        if (!(obj instanceof SnowBytesObject bytes)) {
            throw new IllegalArgumentException("BYTES_LEN: expected bytes");
        }
        stack.pushValue(new org.jcnc.snow.vm.value.IntValue(bytes.length()));
    }
}