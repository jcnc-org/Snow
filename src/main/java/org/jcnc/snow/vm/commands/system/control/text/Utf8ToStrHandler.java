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
 * {@code Utf8ToStrHandler} implements UTF8_TO_STR (0x1A02).
 * <p><b>Stack</b>：in {@code (bytes:byte[])} → out {@code (str:string)}</p>
 * <p><b>Semantic</b>：decodes UTF-8. {@code null} returns empty string.</p>
 */
public final class Utf8ToStrHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) {
        Value v = stack.popValue();
        if (v == Value.NULL) {
            int id = SnowRuntime.get().heap().alloc(new SnowStringObject(""));
            stack.pushValue(new RefValue(id));
            return;
        }
        if (!(v instanceof RefValue(int id))) {
            throw new IllegalArgumentException("UTF8_TO_STR: expected bytes");
        }
        var obj = SnowRuntime.get().heap().get(id);
        if (!(obj instanceof SnowBytesObject bytes)) {
            throw new IllegalArgumentException("UTF8_TO_STR: expected bytes");
        }
        String s = new String(bytes.unsafeBytes(), StandardCharsets.UTF_8);
        int sid = SnowRuntime.get().heap().alloc(new SnowStringObject(s));
        stack.pushValue(new RefValue(sid));
    }
}
