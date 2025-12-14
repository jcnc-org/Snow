package org.jcnc.snow.vm.commands.system.control.text;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.charset.StandardCharsets;
import java.util.List;

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
        Object obj = stack.pop();
        if (obj == null) {
            stack.push("");
            return;
        }
        byte[] bytes = switch (obj) {
            case byte[] b -> b;
            case List<?> list -> {
                byte[] out = new byte[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    Object v = list.get(i);
                    if (v == null) out[i] = 0;
                    else if (v instanceof Number n) out[i] = (byte) n.intValue();
                    else if (v instanceof Boolean b) out[i] = (byte) (b ? 1 : 0);
                    else throw new IllegalArgumentException("UTF8_TO_STR: list element must be number/bool/null");
                }
                yield out;
            }
            default -> throw new IllegalArgumentException("UTF8_TO_STR: bytes must be byte[] or List");
        };
        stack.push(new String(bytes, StandardCharsets.UTF_8));
    }
}

