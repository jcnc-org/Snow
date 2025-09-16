package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.channels.ServerSocketChannel;

public class ListenHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 从栈中取参数 (顺序: backlog → fd)
        int backlog = (int) stack.pop();
        int fd = (int) stack.pop();

        // 2. 获取 ServerSocketChannel
        ServerSocketChannel server = (ServerSocketChannel) SocketRegistry.get(fd);
        if (server == null) {
            throw new IllegalArgumentException("Invalid socket fd: " + fd);
        }

        // 3. 在 Java 中，bind() 时就已经进入监听状态。
        // backlog 只能在 bind() 时指定，因此这里通常无法再修改。
        // 我们可以选择直接忽略 backlog，只返回成功。
        // （如果需要严格模拟，可以在 BindHandler 里延迟到这里再调用 bind(addr, backlog)）

        // 4. 返回 0
        stack.push(0);
    }
}
