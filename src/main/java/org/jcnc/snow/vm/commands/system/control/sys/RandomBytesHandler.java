package org.jcnc.snow.vm.commands.system.control.sys;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.security.SecureRandom;

/**
 * 实现 RANDOM_BYTES (n:int) -> bytes:byte[]
 */
public class RandomBytesHandler implements SyscallHandler {
    // 防止用户请求过大的分配导致 OOM；可根据需要调整或移除此上限
    private static final int MAX_BYTES = 10_000_000; // 10 MB

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        if (stack.isEmpty()) {
            throw new IllegalStateException("RANDOM_BYTES requires 1 argument (n:int)");
        }

        Object nObj = stack.pop();
        if (nObj == null) {
            throw new IllegalArgumentException("RANDOM_BYTES: argument n is null");
        }

        final int n;
        if (nObj instanceof Number) {
            n = ((Number) nObj).intValue();
        } else if (nObj instanceof String) {
            try {
                n = Integer.parseInt((String) nObj);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("RANDOM_BYTES: cannot parse integer from string argument", e);
            }
        } else {
            throw new IllegalArgumentException("RANDOM_BYTES: unsupported argument type: " + nObj.getClass().getName());
        }

        if (n < 0) {
            throw new IllegalArgumentException("RANDOM_BYTES: n must be non-negative");
        }
        if (n > MAX_BYTES) {
            throw new IllegalArgumentException("RANDOM_BYTES: requested byte count exceeds allowed maximum of " + MAX_BYTES);
        }

        byte[] bytes = new byte[n];
        SecureRandom rng = new SecureRandom();
        rng.nextBytes(bytes);

        stack.push(bytes);
    }
}
