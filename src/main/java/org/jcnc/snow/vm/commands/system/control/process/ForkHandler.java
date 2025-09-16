package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

public class ForkHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 使用 ProcessHandle API 启动子进程（这里简单用 "java -version" 示例）
        ProcessBuilder pb = new ProcessBuilder("java", "-version");
        pb.inheritIO(); // 继承父进程的输入输出
        Process child = pb.start();

        // 2. 父进程返回子进程 pid
        long pid = -1;
        try {
            pid = child.pid(); // JDK 9+
        } catch (UnsupportedOperationException e) {
            // 在老版本 Java 中不可用
        }

        // 压回子进程 pid（模拟父进程分支）
        stack.push((int) pid);

        // ⚠️ 注意：子进程逻辑通常由 JVM 主入口处理
        // 在这里我们不能直接让当前线程变成“子进程”
    }
}
