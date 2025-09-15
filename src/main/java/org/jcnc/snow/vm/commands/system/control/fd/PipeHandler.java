package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.channels.Pipe;

/**
 * {@code PipeHandler} 用于实现系统调用 PIPE。
 *
 * <p>
 * 功能：创建一个管道（pipe），返回一对虚拟文件描述符：
 * <ul>
 *   <li>fd[0] → 读端</li>
 *   <li>fd[1] → 写端</li>
 * </ul>
 * </p>
 *
 * <p>调用约定：</p>
 * <ul>
 *   <li>入参：无</li>
 *   <li>出参：{@code [readfd:int, writefd:int]}</li>
 * </ul>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>读端和写端都注册在 {@link FDTable} 中。</li>
 *   <li>多个进程/线程可以共享 fd 进行通信。</li>
 *   <li>如果写端关闭，读端会读到 EOF。</li>
 *   <li>如果读端关闭，再写入会抛出异常。</li>
 * </ul>
 *
 * <p>异常：</p>
 * <ul>
 *   <li>I/O 初始化失败时，抛出 {@link java.io.IOException}</li>
 * </ul>
 */
public class PipeHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 使用 NIO 创建一个管道
        Pipe pipe = Pipe.open();

        // 将读端和写端注册到 FDTable
        int readfd = FDTable.register(pipe.source()); // 读端
        int writefd = FDTable.register(pipe.sink());   // 写端

        // 将 fd 顺序压回栈：先写端，后读端（可根据 VM 约定调整）
        stack.push(readfd);
        stack.push(writefd);
    }
}
