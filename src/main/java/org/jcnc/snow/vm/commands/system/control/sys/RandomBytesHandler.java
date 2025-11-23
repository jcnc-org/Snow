package org.jcnc.snow.vm.commands.system.control.sys;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.security.SecureRandom;

/**
 * {@code RandomBytesHandler} 实现 RANDOM_BYTES (0x1903) 系统调用，
 * 用于生成指定长度的随机字节数组。
 *
 * <p><b>Stack</b>：入参 {@code (n:int)} → 出参 {@code (bytes:byte[])}</p>
 *
 * <p><b>语义</b>：生成长度为 {@code n} 的随机字节数组并返回，使用安全随机数生成器（{@link java.security.SecureRandom}）。</p>
 *
 * <p><b>返回</b>：成功返回长度为 {@code n} 的 {@code byte[]}；当 {@code n==0} 返回空数组。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>若参数缺失、为 {@code null}、无法解析为整数、为负数、或超过允许的最大值（实现中默认上限为 10_000_000）时，抛出 {@link IllegalStateException} 或 {@link IllegalArgumentException}</li>
 * </ul>
 * </p>
 *
 * <p><b>注意</b>：为避免 OOM，调用方应避免请求过大的 {@code n}；实现可能对最大可请求字节数设定限制。</p>
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
