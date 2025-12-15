package org.jcnc.snow.vm.commands.system.control.object;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;
import org.jcnc.snow.vm.runtime.SnowRuntime;
import org.jcnc.snow.vm.runtime.SnowStructObject;
import org.jcnc.snow.vm.value.IntValue;
import org.jcnc.snow.vm.value.RefValue;
import org.jcnc.snow.vm.value.Value;

public final class ObjGetHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack, LocalVariableStore locals, CallStack callStack) {
        Value idxV = stack.popValue();
        Value objV = stack.popValue();

        int idx = toInt(idxV, "OBJ_GET");
        if (!(objV instanceof RefValue(int id))) {
            throw new IllegalArgumentException("OBJ_GET: not a struct");
        }
        var obj = SnowRuntime.get().heap().get(id);
        if (!(obj instanceof SnowStructObject s)) {
            throw new IllegalArgumentException("OBJ_GET: not a struct");
        }
        stack.pushValue(s.get(idx));
    }

    private static int toInt(Value v, String op) {
        return switch (v) {
            case IntValue(int i) -> i;
            case org.jcnc.snow.vm.value.ShortValue(short s) -> s;
            case org.jcnc.snow.vm.value.ByteValue(byte b) -> b;
            case org.jcnc.snow.vm.value.LongValue(long l) -> (int) l;
            default -> throw new IllegalArgumentException(op + ": expected int");
        };
    }
}

