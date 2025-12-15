package org.jcnc.snow.vm.commands.system.control.bytes;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;
import org.jcnc.snow.vm.runtime.SnowBytesObject;
import org.jcnc.snow.vm.runtime.SnowRuntime;
import org.jcnc.snow.vm.value.RefValue;
import org.jcnc.snow.vm.value.Value;

public final class BytesConcatHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) {
        Value bV = stack.popValue();
        Value aV = stack.popValue();

        if (!(aV instanceof RefValue(int aId))) throw new IllegalArgumentException("BYTES_CONCAT: expected bytes");
        if (!(bV instanceof RefValue(int bId))) throw new IllegalArgumentException("BYTES_CONCAT: expected bytes");

        var aObj = SnowRuntime.get().heap().get(aId);
        var bObj = SnowRuntime.get().heap().get(bId);
        if (!(aObj instanceof SnowBytesObject a)) throw new IllegalArgumentException("BYTES_CONCAT: expected bytes");
        if (!(bObj instanceof SnowBytesObject b)) throw new IllegalArgumentException("BYTES_CONCAT: expected bytes");

        byte[] out = new byte[a.length() + b.length()];
        System.arraycopy(a.unsafeBytes(), 0, out, 0, a.length());
        System.arraycopy(b.unsafeBytes(), 0, out, a.length(), b.length());
        int id = SnowRuntime.get().heap().alloc(new SnowBytesObject(out));
        stack.pushValue(new RefValue(id));
    }
}