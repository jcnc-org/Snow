package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code ForkHandler} 实现 FORK (0x1501) 系统调用，
 * 用于在虚拟机中模拟创建一个子进程。
 *
 * <p><b>Stack</b>：无入参 → 出参 {@code (pid:int)}</p>
 *
 * <p><b>语义</b>：通过启动新进程（此处简单以 {@code "java -version"} 为例）来模拟 fork，
 * 父进程返回新进程的 pid，子进程逻辑需由主入口处理。</p>
 *
 * <p><b>返回</b>：成功时返回子进程 pid（正整数）。若获取不到 pid，返回 -1。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>进程启动失败时抛出 {@link java.io.IOException}</li>
 * </ul>
 * </p>
 */
public class ForkHandler implements SyscallHandler {

    /**
     * 处理 FORK 调用。
     *
     * @param stack     操作数栈，用于返回子进程 pid
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception 进程启动失败时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 启动子进程（示例用 "java -version" ）
        ProcessBuilder pb = new ProcessBuilder("java", "-version");
        pb.inheritIO();
        Process child = pb.start();

        // 父进程返回子进程 pid
        long pid = -1;
        try {
            pid = child.pid(); // JDK 9+
        } catch (UnsupportedOperationException e) {
            // 老版本 Java 不支持
        }

        // 压回子进程 pid（模拟父进程分支）
        stack.push((int) pid);

        // 子进程逻辑通常需由 JVM 主入口自行处理
    }
}
