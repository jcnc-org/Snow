package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * SLEEP(ms:int) -> 0
 *
 * 让当前线程休眠指定的毫秒数。
 */
public class SleepHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 获取入参
        int ms = (int) stack.pop();

        // 2. 执行休眠
        Thread.sleep(ms);

        // 3. 压回返回值 0
        stack.push(0);
    }
}
