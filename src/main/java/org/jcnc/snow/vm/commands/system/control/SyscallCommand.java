package org.jcnc.snow.vm.commands.system.control;

import org.jcnc.snow.vm.commands.system.control.array.ArrGetHandler;
import org.jcnc.snow.vm.commands.system.control.array.ArrLoadHandler;
import org.jcnc.snow.vm.commands.system.control.array.ArrSetHandler;
import org.jcnc.snow.vm.commands.system.control.array.ArrStoreHandler;
import org.jcnc.snow.vm.commands.system.control.console.*;
import org.jcnc.snow.vm.commands.system.control.fd.*;
import org.jcnc.snow.vm.commands.system.control.fs.*;
import org.jcnc.snow.vm.commands.system.control.memory.FreeHandler;
import org.jcnc.snow.vm.commands.system.control.memory.MallocHandler;
import org.jcnc.snow.vm.commands.system.control.multiplex.EpollCreateHandler;
import org.jcnc.snow.vm.commands.system.control.multiplex.EpollCtlHandler;
import org.jcnc.snow.vm.commands.system.control.multiplex.EpollWaitHandler;
import org.jcnc.snow.vm.commands.system.control.multiplex.SelectHandler;
import org.jcnc.snow.vm.commands.system.control.process.*;
import org.jcnc.snow.vm.commands.system.control.socket.*;
import org.jcnc.snow.vm.commands.system.control.sys.*;
import org.jcnc.snow.vm.commands.system.control.sync.*;
import org.jcnc.snow.vm.commands.system.control.time.*;
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
        // ================= 文件 & FD =================
        handlers.put("OPEN", new OpenHandler());
        handlers.put("READ", new ReadHandler());
        handlers.put("WRITE", new WriteHandler());
        handlers.put("SEEK", new SeekHandler());
        handlers.put("CLOSE", new CloseHandler());
        handlers.put("STAT", new StatHandler());
        handlers.put("FSTAT", new FstatHandler());
        handlers.put("UNLINK", new UnlinkHandler());
        handlers.put("DUP", new DupHandler());
        handlers.put("DUP2", new Dup2Handler());
        handlers.put("PIPE", new PipeHandler());
        handlers.put("TRUNCATE", new TruncateHandler());
        handlers.put("FTRUNCATE", new FtruncateHandler());
        handlers.put("RENAME", new RenameHandler());
        handlers.put("LINK", new LinkHandler());
        handlers.put("SYMLINK", new SymlinkHandler());
        handlers.put("READLINK", new ReadlinkHandler());
        handlers.put("SET_NONBLOCK", new SetNonblockHandler());

        // ================= 目录 & FS =================
        handlers.put("MKDIR", new MkdirHandler());
        handlers.put("RMDIR", new RmdirHandler());
        handlers.put("CHDIR", new ChdirHandler());
        handlers.put("GETCWD", new GetcwdHandler());
        handlers.put("READDIR", new ReaddirHandler());
        handlers.put("CHMOD", new ChmodHandler());
        handlers.put("FCHMOD", new FchmodHandler());
        handlers.put("UTIME", new UtimeHandler());

        // ================= 标准 IO / 控制台 =================
        handlers.put("PRINT", new PrintHandler());
        handlers.put("PRINTLN", new PrintlnHandler());
        handlers.put("STDIN_READ", new StdinReadHandler());
        handlers.put("STDOUT_WRITE", new StdoutWriteHandler());
        handlers.put("STDERR_WRITE", new StderrWriteHandler());

        // ================= 多路复用 =================
        handlers.put("SELECT", new SelectHandler());
        handlers.put("EPOLL_CREATE", new EpollCreateHandler());
        handlers.put("EPOLL_CTL", new EpollCtlHandler());
        handlers.put("EPOLL_WAIT", new EpollWaitHandler());

        // ================= 套接字 / 网络 =================
        handlers.put("SOCKET", new SocketHandler());
        handlers.put("BIND", new BindHandler());
        handlers.put("LISTEN", new ListenHandler());
        handlers.put("ACCEPT", new AcceptHandler());
        handlers.put("CONNECT", new ConnectHandler());
        handlers.put("SEND", new SendHandler());
        handlers.put("RECV", new RecvHandler());
        handlers.put("SENDTO", new SendToHandler());
        handlers.put("RECVFROM", new RecvFromHandler());
        handlers.put("SHUTDOWN", new ShutdownHandler());
        handlers.put("SETSOCKOPT", new SetSockOptHandler());
        handlers.put("GETSOCKOPT", new GetSockOptHandler());
        handlers.put("GETPEERNAME", new GetPeerNameHandler());
        handlers.put("GETSOCKNAME", new GetSockNameHandler());
        handlers.put("GETADDRINFO", new GetAddrInfoHandler());

        // ================= 进程 / 线程 =================
        handlers.put("EXIT", new ExitHandler());
        handlers.put("FORK", new ForkHandler());
        handlers.put("EXEC", new ExecHandler());
        handlers.put("WAIT", new WaitHandler());
        handlers.put("GETPID", new GetPidHandler());
        handlers.put("GETPPID", new GetPpidHandler());
        handlers.put("THREAD_CREATE", new ThreadCreateHandler());
        handlers.put("THREAD_JOIN", new ThreadJoinHandler());
        handlers.put("SLEEP", new SleepHandler());

        // ================= 内存 =================
        handlers.put("MALLOC", new MallocHandler());
        handlers.put("FREE", new FreeHandler());

        // ================= 数组 =================
        handlers.put("ARR_LOAD", new ArrLoadHandler());
        handlers.put("ARR_STORE", new ArrStoreHandler());
        handlers.put("ARR_GET", new ArrGetHandler());
        handlers.put("ARR_SET", new ArrSetHandler());

        // ================= 系统信息 / 环境 / 随机数 =================
        handlers.put("GETENV", new GetEnvHandler());
        handlers.put("SETENV", new SetEnvHandler());
        handlers.put("NCPU", new NcpuHandler());
        handlers.put("RANDOM_BYTES", new RandomBytesHandler());
        handlers.put("ERRSTR", new ErrStrHandler());
        handlers.put("ERRNO", new ErrnoHandler());
        handlers.put("MEMINFO", new MemInfoHandler());

        // ================= 并发原语 =================
        handlers.put("MUTEX_NEW", new MutexNewHandler());
        handlers.put("MUTEX_LOCK", new MutexLockHandler());
        handlers.put("MUTEX_TRYLOCK", new MutexTrylockHandler());
        handlers.put("MUTEX_UNLOCK", new MutexUnlockHandler());
        handlers.put("COND_NEW", new CondNewHandler());
        handlers.put("COND_WAIT", new CondWaitHandler());
        handlers.put("COND_SIGNAL", new CondSignalHandler());
        handlers.put("COND_BROADCAST", new CondBroadcastHandler());
        handlers.put("SEM_NEW", new SemNewHandler());
        handlers.put("SEM_WAIT", new SemWaitHandler());
        handlers.put("SEM_POST", new SemPostHandler());
        handlers.put("RWLOCK_NEW", new RwlockNewHandler());
        handlers.put("RWLOCK_RLOCK", new RwlockRlockHandler());
        handlers.put("RWLOCK_WLOCK", new RwlockWlockHandler());
        handlers.put("RWLOCK_UNLOCK", new RwlockUnlockHandler());

        // ================= 时间与计时 =================
        handlers.put("CLOCK_GETTIME", new ClockGettimeHandler());
        handlers.put("NANOSLEEP", new NanosleepHandler());
        handlers.put("TIMEOFDAY", new TimeOfDayHandler());
        handlers.put("TICK_MS", new TickMsHandler());
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
