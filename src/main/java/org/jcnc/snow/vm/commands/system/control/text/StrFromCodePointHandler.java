package org.jcnc.snow.vm.commands.system.control.text;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code StrFromCodePointHandler} implements STR_FROM_CODEPOINT (0x1A03).
 * <p><b>Stack</b>：in {@code (codePoint:int)} → out {@code (str:string)}</p>
 */
public final class StrFromCodePointHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) {
        Object obj = stack.pop();
        if (!(obj instanceof Number n)) {
            throw new IllegalArgumentException("STR_FROM_CODEPOINT: codePoint must be an int");
        }
        int codePoint = n.intValue();
        if (!Character.isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException("STR_FROM_CODEPOINT: invalid code point: " + codePoint);
        }
        stack.push(new String(Character.toChars(codePoint)));
    }
}

