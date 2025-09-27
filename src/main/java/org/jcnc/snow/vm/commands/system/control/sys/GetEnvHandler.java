package org.jcnc.snow.vm.commands.system.control.sys;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.EnvRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code GetEnvHandler} 实现 GETENV (0x1004) 系统调用，
 * 用于读取当前环境变量值（含 VM 覆盖层）。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (key:String)} → 出参 {@code (val:String?)}
 * </p>
 *
 * <p><b>语义：</b>
 * <ul>
 *   <li>优先从 VM 环境变量覆盖层（{@link EnvRegistry}）查找指定 key</li>
 *   <li>若无，则回退查找 System.getenv（系统环境变量）</li>
 *   <li>如变量不存在，返回 {@code null}，不抛出异常</li>
 * </ul>
 * </p>
 *
 * <p><b>返回：</b>
 * <ul>
 *   <li>指定环境变量的值（{@code String}），不存在则为 {@code null}</li>
 * </ul>
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>参数不足时抛出 {@link IllegalStateException}</li>
 *   <li>其它参数类型错误时不抛出异常，自动转字符串</li>
 * </ul>
 * </p>
 */
public class GetEnvHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 检查参数
        if (stack.isEmpty()) {
            throw new IllegalStateException("GETENV: 需要参数 key");
        }
        Object keyObj = stack.pop();
        String key = (keyObj == null) ? null : keyObj.toString();

        // 2. 查找环境变量
        String value = EnvRegistry.get(key);

        // 3. 返回变量值，不存在为 null
        stack.push(value);
    }
}
