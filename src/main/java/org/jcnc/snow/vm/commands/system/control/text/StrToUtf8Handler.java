package org.jcnc.snow.vm.commands.system.control.text;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;
import org.jcnc.snow.vm.runtime.SnowBytesObject;
import org.jcnc.snow.vm.runtime.SnowRuntime;
import org.jcnc.snow.vm.runtime.SnowStringObject;
import org.jcnc.snow.vm.value.RefValue;
import org.jcnc.snow.vm.value.Value;

import java.nio.charset.StandardCharsets;

/**
 * {@code StrToUtf8Handler} implements STR_TO_UTF8 (0x1A01).
 * <p><b>Stack</b>：in {@code (str:string)} → out {@code (bytes:byte[])}</p>
 * <p><b>Semantic</b>：UTF-8 encodes {@code str}. {@code null} returns empty bytes.</p>
 */
public final class StrToUtf8Handler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) {
        Value v = stack.popValue();
        if (v == Value.NULL) {
            int bid = SnowRuntime.get().heap().alloc(new SnowBytesObject(new byte[0]));
            stack.pushValue(new RefValue(bid));
            return;
        }
        if (!(v instanceof RefValue(int id))) {
            throw new IllegalArgumentException("STR_TO_UTF8: expected string");
        }
        var obj = SnowRuntime.get().heap().get(id);
        if (!(obj instanceof SnowStringObject s)) {
            throw new IllegalArgumentException("STR_TO_UTF8: expected string");
        }
        byte[] bytes = s.value().getBytes(StandardCharsets.UTF_8);
        int bid = SnowRuntime.get().heap().alloc(new SnowBytesObject(bytes));
        stack.pushValue(new RefValue(bid));
    }
}
