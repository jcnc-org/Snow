package org.jcnc.snow.vm.commands.system.control;

import org.jcnc.snow.vm.commands.system.control.array.ArrGetHandler;
import org.jcnc.snow.vm.commands.system.control.array.ArrLoadHandler;
import org.jcnc.snow.vm.commands.system.control.array.ArrSetHandler;
import org.jcnc.snow.vm.commands.system.control.array.ArrStoreHandler;
import org.jcnc.snow.vm.commands.system.control.console.PrintHandler;
import org.jcnc.snow.vm.commands.system.control.console.PrintlnHandler;
import org.jcnc.snow.vm.commands.system.control.fd.*;
import org.jcnc.snow.vm.commands.system.control.file.*;
import org.jcnc.snow.vm.commands.system.control.memory.FreeHandler;
import org.jcnc.snow.vm.commands.system.control.memory.MallocHandler;
import org.jcnc.snow.vm.commands.system.control.multiplex.EpollCreateHandler;
import org.jcnc.snow.vm.commands.system.control.multiplex.EpollCtlHandler;
import org.jcnc.snow.vm.commands.system.control.multiplex.EpollWaitHandler;
import org.jcnc.snow.vm.commands.system.control.multiplex.SelectHandler;
import org.jcnc.snow.vm.commands.system.control.process.*;
import org.jcnc.snow.vm.commands.system.control.socket.*;
import org.jcnc.snow.vm.commands.system.control.stdio.StderrWriteHandler;
import org.jcnc.snow.vm.commands.system.control.stdio.StdinReadHandler;
import org.jcnc.snow.vm.commands.system.control.stdio.StdoutWriteHandler;
import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * SyscallCommand —— 虚拟机系统调用分发器
 * 解析 SYSCALL 子命令，调用对应 SyscallHandler。
 */
public class SyscallCommand implements Command {

    private static final Map<String, SyscallHandler> handlers = new HashMap<>();

    // 注册所有子命令处理器
    static {
        // ================= 文件 =================
        handlers.put("OPEN_READ", new OpenReadHandler());
        handlers.put("OPEN_WRITE", new OpenWriteHandler());
        handlers.put("OPEN_RDWR", new OpenRdWrHandler());
        handlers.put("CREATE", new CreateHandler());
        handlers.put("TRUNCATE", new TruncateHandler());
        handlers.put("APPEND", new AppendHandler());
        handlers.put("READ", new ReadHandler());
        handlers.put("WRITE", new WriteHandler());
        handlers.put("SEEK_SET", new SeekSetHandler());
        handlers.put("SEEK_CUR", new SeekCurHandler());
        handlers.put("SEEK_END", new SeekEndHandler());
        handlers.put("CLOSE", new CloseHandler());
        handlers.put("STAT", new StatHandler());
        handlers.put("UNLINK", new UnlinkHandler());

        // ================= FD / 管道 =================
        handlers.put("PIPE", new PipeHandler());
        handlers.put("DUP", new DupHandler());
        handlers.put("DUP2", new Dup2Handler());
        handlers.put("CLOSE_READ", new CloseReadHandler());
        handlers.put("CLOSE_WRITE", new CloseWriteHandler());

        // ================= 套接字 =================
        handlers.put("SOCKET_TCP", new SocketTcpHandler());
        handlers.put("SOCKET_UDP", new SocketUdpHandler());
        handlers.put("BIND", new BindHandler());
        handlers.put("LISTEN", new ListenHandler());
        handlers.put("ACCEPT", new AcceptHandler());
        handlers.put("CONNECT", new ConnectHandler());
        handlers.put("SEND", new SendHandler());
        handlers.put("RECV", new RecvHandler());
        handlers.put("SENDTO", new SendToHandler());
        handlers.put("RECVFROM", new RecvFromHandler());
        handlers.put("SHUTDOWN_READ", new ShutdownReadHandler());
        handlers.put("SHUTDOWN_WRITE", new ShutdownWriteHandler());

        // ================= 多路复用 =================
        handlers.put("SELECT", new SelectHandler());
        handlers.put("EPOLL_CREATE", new EpollCreateHandler());
        handlers.put("EPOLL_CTL", new EpollCtlHandler());
        handlers.put("EPOLL_WAIT", new EpollWaitHandler());

        // ================= 数组 =================
        handlers.put("ARR_LOAD", new ArrLoadHandler());
        handlers.put("ARR_STORE", new ArrStoreHandler());
        handlers.put("ARR_GET", new ArrGetHandler());
        handlers.put("ARR_SET", new ArrSetHandler());

        // ================= 控制台 =================
        handlers.put("PRINT", new PrintHandler());
        handlers.put("PRINTLN", new PrintlnHandler());

        // ================= 内存 =================
        handlers.put("MALLOC", new MallocHandler());
        handlers.put("FREE", new FreeHandler());

        // ================= 标准流 =================
        handlers.put("STDIN_READ", new StdinReadHandler());
        handlers.put("STDOUT_WRITE", new StdoutWriteHandler());
        handlers.put("STDERR_WRITE", new StderrWriteHandler());

        // ================= 进程 / 线程 =================
        handlers.put("EXIT", new ExitHandler());
        handlers.put("FORK", new ForkHandler());
        handlers.put("EXEC", new ExecHandler());
        handlers.put("WAIT", new WaitHandler());
        handlers.put("THREAD_CREATE", new ThreadCreateHandler());
        handlers.put("THREAD_JOIN", new ThreadJoinHandler());
    }

    @Override
    public int execute(String[] parts, int pc, OperandStack stack, LocalVariableStore locals, CallStack callStack) {

        if (parts.length < 2) {
            throw new IllegalArgumentException("SYSCALL missing subcommand");
        }

        String cmd = parts[1].toUpperCase(Locale.ROOT);
        SyscallHandler handler = handlers.get(cmd);

        if (handler == null) {
            throw new UnsupportedOperationException("Unsupported SYSCALL subcommand: " + cmd);
        }

        try {
            handler.handle(stack, locals, callStack);
        } catch (Exception e) {
            SyscallUtils.pushErr(stack, e);
        }

        return pc + 1;
    }
}
