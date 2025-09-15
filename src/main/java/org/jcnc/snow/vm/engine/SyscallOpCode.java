package org.jcnc.snow.vm.engine;

/**
 * SyscallOpCode —— 系统调用操作码表
 */
public final class SyscallOpCode {

    // ================= 文件 & FD (0x1000 – 0x10FF) =================
    public static final int OPEN = 0x1000;
    public static final int READ = 0x1001;
    public static final int WRITE = 0x1002;
    public static final int SEEK = 0x1003;
    public static final int CLOSE = 0x1004;
    public static final int STAT = 0x1005;
    public static final int FSTAT = 0x1006;
    public static final int UNLINK = 0x1007;
    public static final int DUP = 0x1008;
    public static final int DUP2 = 0x1009;
    public static final int PIPE = 0x100A;
    public static final int TRUNCATE = 0x100B;
    public static final int FTRUNCATE = 0x100C;
    public static final int RENAME = 0x100D;
    public static final int LINK = 0x100E;
    public static final int SYMLINK = 0x100F;
    public static final int READLINK = 0x1010;
    public static final int SET_NONBLOCK = 0x1011;

    // ================= 目录 & FS (0x1100 – 0x11FF) =================
    public static final int MKDIR = 0x1100;
    public static final int RMDIR = 0x1101;
    public static final int CHDIR = 0x1102;
    public static final int GETCWD = 0x1103;
    public static final int READDIR = 0x1104;
    public static final int CHMOD = 0x1105;
    public static final int FCHMOD = 0x1106;
    public static final int UTIME = 0x1107;

    // ================= 标准 IO / 控制台 (0x1200 – 0x12FF) =================
    public static final int PRINT = 0x1200;
    public static final int PRINTLN = 0x1201;
    public static final int STDIN_READ = 0x1202;
    public static final int STDOUT_WRITE = 0x1203;
    public static final int STDERR_WRITE = 0x1204;

    // ================= 多路复用 (0x1300 – 0x13FF) =================
    public static final int SELECT = 0x1300;
    public static final int EPOLL_CREATE = 0x1301;
    public static final int EPOLL_CTL = 0x1302;
    public static final int EPOLL_WAIT = 0x1303;
    public static final int IO_WAIT = 0x1304;

    // ================= 套接字 & 网络 (0x1400 – 0x14FF) =================
    public static final int SOCKET = 0x1400;
    public static final int BIND = 0x1401;
    public static final int LISTEN = 0x1402;
    public static final int ACCEPT = 0x1403;
    public static final int CONNECT = 0x1404;
    public static final int SEND = 0x1405;
    public static final int RECV = 0x1406;
    public static final int SENDTO = 0x1407;
    public static final int RECVFROM = 0x1408;
    public static final int SHUTDOWN = 0x1409;
    public static final int SETSOCKOPT = 0x140A;
    public static final int GETSOCKOPT = 0x140B;
    public static final int GETPEERNAME = 0x140C;
    public static final int GETSOCKNAME = 0x140D;
    public static final int GETADDRINFO = 0x140E;

    // ================= 进程 & 线程 (0x1500 – 0x15FF) =================
    public static final int EXIT = 0x1500;
    public static final int FORK = 0x1501;
    public static final int EXEC = 0x1502;
    public static final int WAIT = 0x1503;
    public static final int GETPID = 0x1504;
    public static final int GETPPID = 0x1505;
    public static final int THREAD_CREATE = 0x1506;
    public static final int THREAD_JOIN = 0x1507;
    public static final int SLEEP = 0x1508;

    // ================= 并发原语 (0x1600 – 0x16FF) =================
    public static final int MUTEX_NEW = 0x1600;
    public static final int MUTEX_LOCK = 0x1601;
    public static final int MUTEX_TRYLOCK = 0x1602;
    public static final int MUTEX_UNLOCK = 0x1603;
    public static final int COND_NEW = 0x1604;
    public static final int COND_WAIT = 0x1605;
    public static final int COND_SIGNAL = 0x1606;
    public static final int COND_BROADCAST = 0x1607;
    public static final int SEM_NEW = 0x1608;
    public static final int SEM_WAIT = 0x1609;
    public static final int SEM_POST = 0x160A;
    public static final int RWLOCK_NEW = 0x160B;
    public static final int RWLOCK_RLOCK = 0x160C;
    public static final int RWLOCK_WLOCK = 0x160D;
    public static final int RWLOCK_UNLOCK = 0x160E;

    // ================= 时间 & 计时 (0x1700 – 0x17FF) =================
    public static final int CLOCK_GETTIME = 0x1700;
    public static final int NANOSLEEP = 0x1701;
    public static final int TIMEOFDAY = 0x1702;
    public static final int TICK_MS = 0x1703;

    // ================= 数组操作 (0x1800 – 0x18FF) =================
    public static final int ARR_LOAD = 0x1800;
    public static final int ARR_STORE = 0x1801;
    public static final int ARR_GET = 0x1802;
    public static final int ARR_SET = 0x1803;

    // ================= 系统信息 (0x1900 – 0x19FF) =================
    public static final int GETENV = 0x1900;
    public static final int SETENV = 0x1901;
    public static final int NCPU = 0x1902;
    public static final int RANDOM_BYTES = 0x1903;
    public static final int ERRSTR = 0x1904;
    public static final int ERRNO = 0x1905;
    public static final int MEMINFO = 0x1906;

    private SyscallOpCode() {
    }
}
