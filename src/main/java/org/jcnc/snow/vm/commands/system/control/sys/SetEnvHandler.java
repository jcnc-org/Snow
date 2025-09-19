package org.jcnc.snow.vm.commands.system.control.sys;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.Map;

public class SetEnvHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 需要 3 个参数（key, val, overwrite），栈是 LIFO，pop 顺序为 overwrite, val, key
        if (stack.size() < 3) {
            throw new IllegalStateException("SETENV requires 3 arguments (key:string, val:string, overwrite:int)");
        }

        Object overwriteObj = stack.pop();
        Object valObj = stack.pop();
        Object keyObj = stack.pop();

        if (keyObj == null) {
            throw new IllegalArgumentException("SETENV: key is null");
        }

        String key = keyObj.toString();
        String val = (valObj == null) ? null : valObj.toString();

        if (overwriteObj == null) {
            throw new IllegalArgumentException("SETENV: overwrite is null");
        }

        final int overwrite;
        if (overwriteObj instanceof Number) {
            overwrite = ((Number) overwriteObj).intValue();
        } else {
            try {
                overwrite = Integer.parseInt(overwriteObj.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("SETENV: overwrite must be integer 0 or 1", e);
            }
        }

        if (overwrite != 0 && overwrite != 1) {
            throw new IllegalArgumentException("SETENV: overwrite must be 0 or 1");
        }

        // 如果 overwrite==0 且 env 已存在，则不修改（直接返回 0）
        Map<String, String> currentEnv = System.getenv();
        if (overwrite == 0 && currentEnv.containsKey(key)) {
            stack.push(0);
            return;
        }

        // 使用 ProcessBuilder.environment() —— 仅影响子进程，最安全跨 JVM 版本的方法
        Map<String, String> pbEnv = new ProcessBuilder().environment();
        if (val == null) {
            // 删除环境变量（如果存在）
            pbEnv.remove(key);
        } else {
            // 设置或覆盖环境变量
            pbEnv.put(key, val);
        }

        // 返回 0 表示成功
        stack.push(0);
    }
}
