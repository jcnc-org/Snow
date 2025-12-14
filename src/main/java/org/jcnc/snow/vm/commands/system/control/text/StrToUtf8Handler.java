package org.jcnc.snow.vm.commands.system.control.text;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

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
        Object obj = stack.pop();
        if (obj == null) {
            stack.push(new byte[0]);
            return;
        }
        if (!(obj instanceof String s)) {
            throw new IllegalArgumentException("STR_TO_UTF8: str must be a string");
        }
        stack.push(s.getBytes(StandardCharsets.UTF_8));
    }
}

