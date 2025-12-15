package org.jcnc.snow.vm.commands.system.control.text;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;
import org.jcnc.snow.vm.runtime.SnowRuntime;
import org.jcnc.snow.vm.runtime.SnowStringObject;
import org.jcnc.snow.vm.value.IntValue;
import org.jcnc.snow.vm.value.RefValue;
import org.jcnc.snow.vm.value.Value;

import java.nio.charset.StandardCharsets;

/**
 * {@code StrLenHandler} implements STR_LEN (0x1A00).
 * <p><b>Stack</b>：in {@code (str:string)} → out {@code (len:int)}</p>
 * <p><b>Semantic</b>：returns the UTF-8 byte length of {@code str}. {@code null} returns 0.</p>
 */
public final class StrLenHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) {
        Value v = stack.popValue();
        if (v == Value.NULL) {
            stack.pushValue(new IntValue(0));
            return;
        }
        if (!(v instanceof RefValue(int id))) {
            throw new IllegalArgumentException("STR_LEN: expected string");
        }
        var obj = SnowRuntime.get().heap().get(id);
        if (!(obj instanceof SnowStringObject s)) {
            throw new IllegalArgumentException("STR_LEN: expected string");
        }
        stack.pushValue(new IntValue(s.value().getBytes(StandardCharsets.UTF_8).length));
    }
}
