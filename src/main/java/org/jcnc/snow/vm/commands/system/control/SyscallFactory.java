package org.jcnc.snow.vm.commands.system.control;

import org.jcnc.snow.vm.commands.system.control.array.ArrGetHandler;
import org.jcnc.snow.vm.commands.system.control.array.ArrLenHandler;
import org.jcnc.snow.vm.commands.system.control.array.ArrSetHandler;
import org.jcnc.snow.vm.commands.system.control.console.StderrWriteHandler;
import org.jcnc.snow.vm.commands.system.control.console.StdinReadHandler;
import org.jcnc.snow.vm.commands.system.control.console.StdoutWriteHandler;
import org.jcnc.snow.vm.commands.system.control.fd.*;
import org.jcnc.snow.vm.commands.system.control.fs.*;
import org.jcnc.snow.vm.commands.system.control.multiplex.*;
import org.jcnc.snow.vm.commands.system.control.process.*;
import org.jcnc.snow.vm.commands.system.control.socket.*;
import org.jcnc.snow.vm.commands.system.control.sync.*;
import org.jcnc.snow.vm.commands.system.control.sys.*;
import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.commands.system.control.time.ClockGettimeHandler;
import org.jcnc.snow.vm.commands.system.control.time.NanosleepHandler;
import org.jcnc.snow.vm.commands.system.control.time.TickMsHandler;
import org.jcnc.snow.vm.commands.system.control.time.TimeOfDayHandler;
import org.jcnc.snow.vm.engine.SyscallOpCode;

/**
 * SyscallFactory —— 将 SyscallOpCode 映射到具体的 SyscallHandler。
 */
public final class SyscallFactory {

    /**
     * 完整的 syscall 表。0x1000 – 0x19FF。
     */
    private static final SyscallHandler[] SYSCALLS = new SyscallHandler[0x1A00];

    static {
        // ================= 文件 & FD =================
        SYSCALLS[SyscallOpCode.OPEN] = new OpenHandler();
        SYSCALLS[SyscallOpCode.READ] = new ReadHandler();
        SYSCALLS[SyscallOpCode.WRITE] = new WriteHandler();
        SYSCALLS[SyscallOpCode.SEEK] = new SeekHandler();
        SYSCALLS[SyscallOpCode.CLOSE] = new CloseHandler();
        SYSCALLS[SyscallOpCode.STAT] = new StatHandler();
        SYSCALLS[SyscallOpCode.FSTAT] = new FstatHandler();
        SYSCALLS[SyscallOpCode.UNLINK] = new UnlinkHandler();
        SYSCALLS[SyscallOpCode.DUP] = new DupHandler();
        SYSCALLS[SyscallOpCode.DUP2] = new Dup2Handler();
        SYSCALLS[SyscallOpCode.PIPE] = new PipeHandler();
        SYSCALLS[SyscallOpCode.TRUNCATE] = new TruncateHandler();
        SYSCALLS[SyscallOpCode.FTRUNCATE] = new FtruncateHandler();
        SYSCALLS[SyscallOpCode.RENAME] = new RenameHandler();
        SYSCALLS[SyscallOpCode.LINK] = new LinkHandler();
        SYSCALLS[SyscallOpCode.SYMLINK] = new SymlinkHandler();
        SYSCALLS[SyscallOpCode.READLINK] = new ReadlinkHandler();
        SYSCALLS[SyscallOpCode.SET_NONBLOCK] = new SetNonblockHandler();

        // ================= 目录 & FS =================
        SYSCALLS[SyscallOpCode.MKDIR] = new MkdirHandler();
        SYSCALLS[SyscallOpCode.RMDIR] = new RmdirHandler();
        SYSCALLS[SyscallOpCode.CHDIR] = new ChdirHandler();
        SYSCALLS[SyscallOpCode.GETCWD] = new GetcwdHandler();
        SYSCALLS[SyscallOpCode.READDIR] = new ReaddirHandler();
        SYSCALLS[SyscallOpCode.CHMOD] = new ChmodHandler();
        SYSCALLS[SyscallOpCode.FCHMOD] = new FchmodHandler();
        SYSCALLS[SyscallOpCode.UTIME] = new UtimeHandler();

        // ================= 标准 IO =================
        SYSCALLS[SyscallOpCode.STDIN_READ] = new StdinReadHandler();
        SYSCALLS[SyscallOpCode.STDOUT_WRITE] = new StdoutWriteHandler();
        SYSCALLS[SyscallOpCode.STDERR_WRITE] = new StderrWriteHandler();

        // ================= 多路复用 =================
        SYSCALLS[SyscallOpCode.SELECT] = new SelectHandler();
        SYSCALLS[SyscallOpCode.EPOLL_CREATE] = new EpollCreateHandler();
        SYSCALLS[SyscallOpCode.EPOLL_CTL] = new EpollCtlHandler();
        SYSCALLS[SyscallOpCode.EPOLL_WAIT] = new EpollWaitHandler();
        SYSCALLS[SyscallOpCode.IO_WAIT] = new IoWaitHandler();


        // ================= 网络 =================
        SYSCALLS[SyscallOpCode.SOCKET] = new SocketHandler();
        SYSCALLS[SyscallOpCode.BIND] = new BindHandler();
        SYSCALLS[SyscallOpCode.LISTEN] = new ListenHandler();
        SYSCALLS[SyscallOpCode.ACCEPT] = new AcceptHandler();
        SYSCALLS[SyscallOpCode.CONNECT] = new ConnectHandler();
        SYSCALLS[SyscallOpCode.SEND] = new SendHandler();
        SYSCALLS[SyscallOpCode.RECV] = new RecvHandler();
        SYSCALLS[SyscallOpCode.SENDTO] = new SendToHandler();
        SYSCALLS[SyscallOpCode.RECVFROM] = new RecvFromHandler();
        SYSCALLS[SyscallOpCode.SHUTDOWN] = new ShutdownHandler();
        SYSCALLS[SyscallOpCode.SETSOCKOPT] = new SetSockOptHandler();
        SYSCALLS[SyscallOpCode.GETSOCKOPT] = new GetSockOptHandler();
        SYSCALLS[SyscallOpCode.GETPEERNAME] = new GetPeerNameHandler();
        SYSCALLS[SyscallOpCode.GETSOCKNAME] = new GetSockNameHandler();
        SYSCALLS[SyscallOpCode.GETADDRINFO] = new GetAddrInfoHandler();

        // ================= 进程 & 线程 =================
        SYSCALLS[SyscallOpCode.EXIT] = new ExitHandler();
        SYSCALLS[SyscallOpCode.FORK] = new ForkHandler();
        SYSCALLS[SyscallOpCode.EXEC] = new ExecHandler();
        SYSCALLS[SyscallOpCode.WAIT] = new WaitHandler();
        SYSCALLS[SyscallOpCode.GETPID] = new GetPidHandler();
        SYSCALLS[SyscallOpCode.GETPPID] = new GetPpidHandler();
        SYSCALLS[SyscallOpCode.THREAD_CREATE] = new ThreadCreateHandler();
        SYSCALLS[SyscallOpCode.THREAD_JOIN] = new ThreadJoinHandler();
        SYSCALLS[SyscallOpCode.SLEEP] = new SleepHandler();

        // ================= 数组 =================
        SYSCALLS[SyscallOpCode.ARR_LEN] = new ArrLenHandler();
        SYSCALLS[SyscallOpCode.ARR_GET] = new ArrGetHandler();
        SYSCALLS[SyscallOpCode.ARR_SET] = new ArrSetHandler();

        // ================= 系统信息 =================
        SYSCALLS[SyscallOpCode.GETENV] = new GetEnvHandler();
        SYSCALLS[SyscallOpCode.SETENV] = new SetEnvHandler();
        SYSCALLS[SyscallOpCode.NCPU] = new NcpuHandler();
        SYSCALLS[SyscallOpCode.RANDOM_BYTES] = new RandomBytesHandler();
        SYSCALLS[SyscallOpCode.ERRSTR] = new ErrStrHandler();
        SYSCALLS[SyscallOpCode.ERRNO] = new ErrnoHandler();
        SYSCALLS[SyscallOpCode.MEMINFO] = new MemInfoHandler();

        // ================= 并发原语 =================
        SYSCALLS[SyscallOpCode.MUTEX_NEW] = new MutexNewHandler();
        SYSCALLS[SyscallOpCode.MUTEX_LOCK] = new MutexLockHandler();
        SYSCALLS[SyscallOpCode.MUTEX_TRYLOCK] = new MutexTrylockHandler();
        SYSCALLS[SyscallOpCode.MUTEX_UNLOCK] = new MutexUnlockHandler();
        SYSCALLS[SyscallOpCode.COND_NEW] = new CondNewHandler();
        SYSCALLS[SyscallOpCode.COND_WAIT] = new CondWaitHandler();
        SYSCALLS[SyscallOpCode.COND_SIGNAL] = new CondSignalHandler();
        SYSCALLS[SyscallOpCode.COND_BROADCAST] = new CondBroadcastHandler();
        SYSCALLS[SyscallOpCode.SEM_NEW] = new SemNewHandler();
        SYSCALLS[SyscallOpCode.SEM_WAIT] = new SemWaitHandler();
        SYSCALLS[SyscallOpCode.SEM_POST] = new SemPostHandler();
        SYSCALLS[SyscallOpCode.RWLOCK_NEW] = new RwlockNewHandler();
        SYSCALLS[SyscallOpCode.RWLOCK_RLOCK] = new RwlockRlockHandler();
        SYSCALLS[SyscallOpCode.RWLOCK_WLOCK] = new RwlockWlockHandler();
        SYSCALLS[SyscallOpCode.RWLOCK_UNLOCK] = new RwlockUnlockHandler();

        // ================= 时间 & 计时 =================
        SYSCALLS[SyscallOpCode.CLOCK_GETTIME] = new ClockGettimeHandler();
        SYSCALLS[SyscallOpCode.NANOSLEEP] = new NanosleepHandler();
        SYSCALLS[SyscallOpCode.TIMEOFDAY] = new TimeOfDayHandler();
        SYSCALLS[SyscallOpCode.TICK_MS] = new TickMsHandler();
    }

    private SyscallFactory() {
    }

    public static SyscallHandler getHandler(int opcode) {
        if (opcode < 0 || opcode >= SYSCALLS.length) {
            throw new IllegalArgumentException("Invalid syscall opcode: " + opcode);
        }
        SyscallHandler handler = SYSCALLS[opcode];
        if (handler == null) {
            throw new UnsupportedOperationException("Unsupported syscall opcode: " + opcode);
        }
        return handler;
    }
}
