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

/**
 * {@code StrFromCodePointHandler} implements STR_FROM_CODEPOINT (0x1A03).
 * <p><b>Stack</b>：in {@code (codePoint:int)} → out {@code (str:string)}</p>
 */
public final class StrFromCodePointHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) {
        Value v = stack.popValue();
        if (v == Value.NULL) {
            throw new IllegalArgumentException("STR_FROM_CODEPOINT: codePoint is null");
        }
        int codePoint = switch (v) {
            case IntValue(int i) -> i;
            default -> throw new IllegalArgumentException("STR_FROM_CODEPOINT: codePoint must be an int");
        };
        if (!Character.isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException("STR_FROM_CODEPOINT: invalid code point: " + codePoint);
        }
        String s = new String(Character.toChars(codePoint));
        int id = SnowRuntime.get().heap().alloc(new SnowStringObject(s));
        stack.pushValue(new RefValue(id));
    }
}
