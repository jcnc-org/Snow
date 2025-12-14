package org.jcnc.snow.vm.commands.system.control.text;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

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
        Object obj = stack.pop();
        if (obj == null) {
            stack.push(0);
            return;
        }
        if (!(obj instanceof String s)) {
            throw new IllegalArgumentException("STR_LEN: str must be a string");
        }
        stack.push(s.getBytes(StandardCharsets.UTF_8).length);
    }
}

