package org.jcnc.snow.vm.commands.system.control.object;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;
import org.jcnc.snow.vm.runtime.SnowRuntime;
import org.jcnc.snow.vm.runtime.SnowStringObject;
import org.jcnc.snow.vm.runtime.SnowStructObject;
import org.jcnc.snow.vm.value.IntValue;
import org.jcnc.snow.vm.value.RefValue;
import org.jcnc.snow.vm.value.Value;

public final class ObjNewHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack, LocalVariableStore locals, CallStack callStack) {
        Value fieldCountV = stack.popValue();
        Value typeNameV = stack.popValue();

        int fieldCount = toInt(fieldCountV, "OBJ_NEW");
        if (fieldCount < 0) throw new IllegalArgumentException("OBJ_NEW: fieldCount must be >= 0");

        String typeName = toStringRef(typeNameV, "OBJ_NEW");
        int id = SnowRuntime.get().heap().alloc(new SnowStructObject(typeName, fieldCount));
        stack.pushValue(new RefValue(id));
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

    private static String toStringRef(Value v, String op) {
        if (!(v instanceof RefValue(int id))) {
            throw new IllegalArgumentException(op + ": expected string ref");
        }
        var obj = SnowRuntime.get().heap().get(id);
        if (!(obj instanceof SnowStringObject s)) {
            throw new IllegalArgumentException(op + ": expected string ref");
        }
        return s.value();
    }
}

