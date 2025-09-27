package org.jcnc.snow.vm.commands.system.control.sys;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;
import org.jcnc.snow.vm.io.EnvRegistry;

/**
 * {@code SetEnvHandler} 实现 SETENV (0x1003) 系统调用，
 * 设置虚拟机运行环境变量。
 *
 * <p><b>Stack：</b> 入参 {@code (key:String, val:String, overwrite:int)} → 出参 {@code (rc:int)}</p>
 *
 * <p><b>语义：</b>
 * 设置指定名称（key）的环境变量为 val。
 * <ul>
 *   <li>若环境变量已存在且 {@code overwrite} = 1，则覆盖原值</li>
 *   <li>若环境变量已存在且 {@code overwrite} = 0，则不做更改，直接返回</li>
 *   <li>val 允许为 null 或空字符串，视 EnvRegistry 具体实现</li>
 * </ul>
 * </p>
 *
 * <p><b>返回：</b>
 * <ul>
 *   <li>{@code 0} 设置成功或无需修改</li>
 *   <li>{@code 1} 已存在且未覆盖</li>
 * </ul>
 * 实际 rc 由 {@link EnvRegistry#set(String, String, int)} 决定。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>参数数量不足时抛出 {@link IllegalStateException}</li>
 *   <li>key、overwrite 任意为 null 或类型不合法时抛出 {@link IllegalArgumentException}</li>
 *   <li>overwrite 不是 0/1 时抛出 {@link IllegalArgumentException}</li>
 * </ul>
 * </p>
 */
public class SetEnvHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 检查参数数量
        if (stack.size() < 3) {
            throw new IllegalStateException("SETENV 需要 3 个参数 (key:string, val:string, overwrite:int)");
        }

        // 2. 取参数（栈顶到栈底顺序：overwrite, val, key）
        Object overwriteObj = stack.pop();
        Object valObj = stack.pop();
        Object keyObj = stack.pop();

        if (keyObj == null) {
            throw new IllegalArgumentException("SETENV: key 不能为空");
        }
        String key = keyObj.toString();
        String val = (valObj == null) ? null : valObj.toString();

        if (overwriteObj == null) {
            throw new IllegalArgumentException("SETENV: overwrite 不能为空");
        }

        // 3. 解析 overwrite 参数（仅允许 0 或 1）
        final int overwrite;
        if (overwriteObj instanceof Number) {
            overwrite = ((Number) overwriteObj).intValue();
        } else {
            try {
                overwrite = Integer.parseInt(overwriteObj.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("SETENV: overwrite 必须为整数 0 或 1", e);
            }
        }
        if (overwrite != 0 && overwrite != 1) {
            throw new IllegalArgumentException("SETENV: overwrite 只能为 0 或 1");
        }

        // 4. 调用 EnvRegistry 进行实际设置
        int rc = EnvRegistry.set(key, val, overwrite);

        // 5. 返回设置结果
        stack.push(rc);
    }
}
