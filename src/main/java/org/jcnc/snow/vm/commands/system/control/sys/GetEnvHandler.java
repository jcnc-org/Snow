package org.jcnc.snow.vm.commands.system.control.sys;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * 实现 GETENV (key:string) -> val:string?
 */
public class GetEnvHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 需要一个参数：key
        if (stack.isEmpty()) {
            throw new IllegalStateException("GETENV requires 1 argument (key:string)");
        }

        Object keyObj = stack.pop();
        String key = (keyObj == null) ? null : keyObj.toString();

        String value = null;
        if (key != null) {
            value = System.getenv(key); // 可能返回 null
        }

        // 将结果（可能为 null）压回栈
        stack.push(value);
    }
}
