package org.jcnc.snow.vm.engine;

import java.util.Stack;

/**
 * SyscallOpCode —— 系统调用操作码表
 */
public final class SyscallOpCode {

    // ================= 文件 & FD (0x1000 – 0x10FF) =================

    /**
     * 打开文件并返回一个新的 fd。
     *
     * <p><b>Stack</b>：入参 {@code (path:String, flags:int)} → 出参 {@code (fd:int)}</p>
     * <p><b>语义</b>：依据 {@code flags}（由 {@code OpenFlags} 解析为 {@code OpenOption} 集）打开
     * {@code path} 对应的文件，底层通过 {@code Files.newByteChannel(...)} 创建通道并注册到 {@code FDTable}。</p>
     * <p><b>异常</b>：路径/flags 类型错误，flags 非法，文件不存在且未指定创建，权限不足，或底层 I/O 失败。</p>
     */
    public static final int OPEN = 0x1000;

    /**
     * 从 fd 对应的可读通道读取最多 {@code length} 字节。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int, length:int)} → 出参 {@code (data:byte[])}</p>
     * <p><b>语义</b>：若读到 EOF 或 {@code length <= 0}，返回长度为 0 的字节数组；否则返回实际读取的字节数组。</p>
     * <p><b>异常</b>：fd 非法/不可读，length 类型错误或为负，I/O 失败。</p>
     */
    public static final int READ = 0x1001;

    /**
     * 向 fd 对应的可写通道写入字节数组。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int, data:byte[])} → 出参 {@code (written:int)}</p>
     * <p><b>语义</b>：写入 {@code data}，返回实际写入的字节数。</p>
     * <p><b>异常</b>：fd 非法/不可写，data 非字节数组，I/O 失败。</p>
     */
    public static final int WRITE = 0x1002;

    /**
     * 移动文件指针并返回新位置。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int, offset:long, whence:int)} → 出参 {@code (newPos:long)}</p>
     * <p><b>whence</b>：0 = 从文件开头（SEEK_SET），1 = 相对当前位置（SEEK_CUR），2 = 相对文件末尾（SEEK_END）。</p>
     * <p><b>语义</b>：仅适用于 {@code SeekableByteChannel}。结果位置不得为负。</p>
     * <p><b>异常</b>：fd 非法/不可定位，whence 非法，计算得到的新位置为负，I/O 失败。</p>
     */
    public static final int SEEK = 0x1003;

    /**
     * 关闭 fd。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int)} → 出参：无</p>
     * <p><b>语义</b>：关闭并从 {@code FDTable} 移除，释放底层通道。</p>
     * <p><b>异常</b>：fd 非法。</p>
     */
    public static final int CLOSE = 0x1004;

    /**
     * 获取路径对应文件的基本属性。
     *
     * <p><b>Stack</b>：入参 {@code (path:String)} → 出参 {@code (attrs:Map)}</p>
     * <p><b>实现</b>：通过 {@code Files.readAttributes(..., BasicFileAttributes.class)} 填充
     * {@code size/isDirectory/isRegularFile/lastModified/created} 等键。</p>
     * <p><b>异常</b>：路径类型错误，文件不存在，权限不足，I/O 失败。</p>
     */
    public static final int STAT = 0x1005;

    /**
     * 根据 fd 获取文件的基本属性。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int)} → 出参 {@code (attrs:Map)}</p>
     * <p><b>实现</b>：针对 {@code SeekableByteChannel}，至少提供
     * {@code size} 与 {@code position}；如未跟踪 Path，则 {@code lastModified/created} 等可能返回占位值（-1）。</p>
     * <p><b>异常</b>：fd 非法/不可定位，I/O 失败。</p>
     */
    public static final int FSTAT = 0x1006;

    /**
     * 删除路径（类似 POSIX {@code unlink}）。
     *
     * <p><b>Stack</b>：入参 {@code (path:String)} → 出参：无</p>
     * <p><b>实现</b>：{@code Files.delete(path)}，若为非空目录会抛出相应异常。</p>
     * <p><b>异常</b>：路径类型错误，目标不存在/是目录/权限不足，I/O 失败。</p>
     */
    public static final int UNLINK = 0x1007;

    /**
     * 复制一个已打开的 fd，返回新的 fd（指向同一底层通道）。
     *
     * <p><b>Stack</b>：入参 {@code (oldfd:int)} → 出参 {@code (newfd:int)}</p>
     * <p><b>异常</b>：oldfd 非法或复制失败。</p>
     */
    public static final int DUP = 0x1008;

    /**
     * 将 {@code newfd} 变为 {@code oldfd} 的副本（若 {@code newfd} 已占用会先关闭），返回 {@code newfd}。
     *
     * <p><b>Stack</b>：入参 {@code (oldfd:int, newfd:int)} → 出参 {@code (resultFd:int)}</p>
     * <p><b>异常</b>：oldfd 非法，newfd 非法或不可用，复制失败。</p>
     */
    public static final int DUP2 = 0x1009;

    /**
     * 创建匿名管道，返回一对 fd：读端与写端。
     *
     * <p><b>Stack</b>：入参：无 → 出参 {@code (readfd:int, writefd:int)}</p>
     * <p><b>顺序</b>：先压入 {@code readfd}，再压入 {@code writefd}；因此调用后栈顶元素为 {@code writefd}。</p>
     * <p><b>异常</b>：资源不足或底层 I/O 失败。</p>
     */
    public static final int PIPE = 0x100A;

    /**
     * 将路径对应文件截断到指定长度。
     *
     * <p><b>Stack</b>：入参 {@code (path:String, length:long)} → 出参：无</p>
     * <p><b>实现</b>：以可写方式打开（必要时创建）并调用 {@code SeekableByteChannel.truncate(length)}。</p>
     * <p><b>异常</b>：路径/长度类型错误，权限不足，I/O 失败。</p>
     */
    public static final int TRUNCATE = 0x100B;

    /**
     * 将 fd 对应文件截断到指定长度。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int, length:long)} → 出参 {@code (rc:int)}</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：fd 非法/不可定位或不可写，长度非法，I/O 失败。</p>
     */
    public static final int FTRUNCATE = 0x100C;

    /**
     * 重命名（或移动）文件/目录；若目标存在则覆盖。
     *
     * <p><b>Stack</b>：入参 {@code (oldPath:String, newPath:String)} → 出参 {@code (rc:int)}</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：参数类型错误，源不存在/权限不足，I/O 失败。</p>
     */
    public static final int RENAME = 0x100D;

    /**
     * 创建硬链接：{@code newPath} 指向 {@code oldPath}。
     *
     * <p><b>Stack</b>：入参 {@code (oldPath:String, newPath:String)} → 出参 {@code (rc:int)}</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：参数类型错误，文件系统不支持硬链接，权限不足，I/O 失败。</p>
     */
    public static final int LINK = 0x100E;

    /**
     * 创建符号链接：在 {@code linkPath} 处创建指向 {@code target} 的 symlink。
     *
     * <p><b>Stack</b>：入参 {@code (target:String, linkPath:String)} → 出参 {@code (rc:int)}</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：参数类型错误，文件系统/平台限制，权限不足，I/O 失败。</p>
     */
    public static final int SYMLINK = 0x100F;

    /**
     * 读取符号链接的目标路径。
     *
     * <p><b>Stack</b>：入参 {@code (path:String)} → 出参 {@code (target:String)}</p>
     * <p><b>异常</b>：参数类型错误，目标不是符号链接，权限不足，I/O 失败。</p>
     */
    public static final int READLINK = 0x1010;

    /**
     * 设置 fd 的非阻塞/阻塞模式（仅对 {@code SelectableChannel} 生效）。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int, on:int)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：{@code on=1} → 设为非阻塞（{@code configureBlocking(false)}）；{@code on=0} → 阻塞。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：fd 非法或通道不支持选择器/模式配置。</p>
     */
    public static final int SET_NONBLOCK = 0x1011;
    // ================= 目录 & FS (0x1100 – 0x11FF) =================

    /**
     * 创建目录（可选权限位）。
     *
     * <p><b>Stack</b>：入参 {@code (path:String, mode:int?)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：在 {@code path} 处创建目录；{@code mode} 如未提供可由实现采用默认权限。
     *   在不支持 POSIX 权限的平台上，{@code mode} 可能被忽略或部分生效。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：路径非法/已存在/父目录不存在/权限不足，I/O 失败。</p>
     */
    public static final int MKDIR = 0x1100;

    /**
     * 删除空目录。
     *
     * <p><b>Stack</b>：入参 {@code (path:String)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：仅当目录为空时删除；非空目录应返回错误。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：路径不是目录/目录非空/不存在/权限不足，I/O 失败。</p>
     */
    public static final int RMDIR = 0x1101;

    /**
     * 改变 VM 的当前工作目录（CWD）。
     *
     * <p><b>Stack</b>：入参 {@code (path:String)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：将调用上下文的 CWD 切换到 {@code path}；实现需要在 VM 运行时保存 CWD 状态。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：路径不存在/非目录/不可访问，或实现未提供 CWD 语义。</p>
     */
    public static final int CHDIR = 0x1102;

    /**
     * 获取当前工作目录（CWD）。
     *
     * <p><b>Stack</b>：入参：无 → 出参 {@code (cwd:String)}</p>
     * <p><b>语义</b>：返回 VM 保存的当前工作目录绝对路径。</p>
     * <p><b>异常</b>：实现未提供 CWD 语义或内部状态无效。</p>
     */
    public static final int GETCWD = 0x1103;

    /**
     * 读取目录内容。
     *
     * <p><b>Stack</b>：入参 {@code (path:String)} → 出参 {@code (entries:Array)}</p>
     * <p><b>语义</b>：返回 {@code path} 下的直接子项列表（非递归）。实现可返回字符串数组（文件/目录名），
     *   或返回包含名称/类型等字段的条目对象数组。</p>
     * <p><b>异常</b>：路径不存在/非目录/不可读，I/O 失败。</p>
     */
    public static final int READDIR = 0x1104;

    /**
     * 修改路径对应文件/目录的权限（平台支持受限时可部分生效或为 no-op）。
     *
     * <p><b>Stack</b>：入参 {@code (path:String, mode:int)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：将 {@code path} 的权限设置为 {@code mode}；在不支持 POSIX 权限的平台上可能退化。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：路径不存在/权限不足/模式非法，I/O 失败。</p>
     */
    public static final int CHMOD = 0x1105;

    /**
     * 修改 fd 对应文件/目录的权限（平台支持受限时可部分生效或为 no-op）。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int, mode:int)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：将 fd 指向对象的权限设置为 {@code mode}。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：fd 非法/权限不足/模式非法，I/O 失败。</p>
     */
    public static final int FCHMOD = 0x1106;

    /**
     * 更新文件/目录的访问与修改时间。
     *
     * <p><b>Stack</b>：入参 {@code (path:String, mtime:long, atime:long)} → 出参 {@code (rc:int)}</p>
     * <p><b>单位</b>：毫秒时间戳（自 Epoch）。</p>
     * <p><b>语义</b>：将 {@code path} 的修改时间设为 {@code mtime}，访问时间设为 {@code atime}。
     *   在部分平台/运行时可能仅支持修改时间（atime 可能被忽略）。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：路径不存在/权限不足/时间值非法，I/O 失败。</p>
     */
    public static final int UTIME = 0x1107;


    // ================= 标准 IO / 控制台 (0x1200 – 0x12FF) =================

    /**
     * 将对象内容输出到标准输出（不自动换行）。
     *
     * <p><b>Stack</b>：入参 {@code (obj:Object)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：从操作数栈弹出一个对象，通过 SyscallUtil
     * 方法将其打印到标准输出流（不附加换行）。</p>
     * <p><b>返回</b>：成功时返回 {@code 0}。</p>
     * <p><b>异常</b>：若对象输出过程发生错误（例如 I/O 异常），抛出对应异常。</p>
     */
    public static final int PRINT = 0x1200;
    /**
     * 将对象内容输出到标准输出，并在末尾附加换行。
     *
     * <p><b>Stack</b>：入参 {@code (obj:Object)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：从操作数栈弹出一个对象，通过 SyscallUtil
     * 方法将其打印到标准输出流，并在输出后自动追加换行。</p>
     * <p><b>返回</b>：成功时返回 {@code 0}。</p>
     * <p><b>异常</b>：若对象输出过程发生错误（例如 I/O 异常），抛出对应异常。</p>
     */
    public static final int PRINTLN = 0x1201;
    /**
     * 从标准输入读取一行文本。
     *
     * <p><b>Stack</b>：入参：无 → 出参 {@code (line:String)}</p>
     * <p><b>语义</b>：从 {@code System.in} 读取一行字符串并返回；若到达 EOF 或读取失败，则返回空字符串。</p>
     * <p><b>异常</b>：I/O 错误。</p>
     */
    public static final int STDIN_READ = 0x1202;

    /**
     * 向标准输出（stdout, fd=1）写入字节数组或字符串。
     *
     * <p><b>Stack</b>：入参 {@code (data:byte[]|String|Object)} → 出参 {@code (written:int)}</p>
     * <p><b>语义</b>：将参数转换为字节数组（字符串按 UTF-8 编码，其它对象调用 {@code toString()}），
     * 写入标准输出通道并返回实际写入的字节数。</p>
     * <p><b>异常</b>：stdout 通道不可写，或写入过程中发生 I/O 错误。</p>
     */
    public static final int STDOUT_WRITE = 0x1203;

    /**
     * 向标准错误输出（stderr）写入字符串。
     *
     * <p><b>Stack</b>：入参 {@code (data:Object)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：将对象转换为字符串（null 输出为 "null"），写入 {@code System.err}；
     * 操作完成后返回 {@code 0}。</p>
     * <p><b>异常</b>：写入过程中发生 I/O 错误。</p>
     */
    public static final int STDERR_WRITE = 0x1204;

    // ================= 多路复用 (0x1300 – 0x13FF) =================
    /**
     * I/O 多路复用 select。
     *
     * <p><b>Stack</b>：入参 {@code (readSet:array, writeSet:array, exceptSet:array, timeout_ms:int)}
     * → 出参 {@code (ready:map)}</p>
     *
     * <p><b>语义</b>：检查给定 fd 集合的可读、可写及异常状态，返回各类就绪 fd。
     * 结果 map 至少包含键 {@code read}、{@code write}、{@code except}。</p>
     *
     * <p><b>返回</b>：成功返回 map，其中的值为就绪 fd 列表。</p>
     *
     * <p><b>异常</b>：参数类型错误，timeout 非法，或底层 I/O 失败。</p>
     */
    public static final int SELECT = 0x1300;

    /**
     * 创建 epoll 实例。
     *
     * <p><b>Stack</b>：入参 {@code (flags:int?)} → 出参 {@code (epfd:int)}</p>
     *
     * <p><b>语义</b>：分配一个新的 epoll 文件描述符（epfd），并在内部注册 epoll 实例。</p>
     *
     * <p><b>返回</b>：成功返回新的 epfd。</p>
     *
     * <p><b>异常</b>：flags 非法，资源不足，或底层 I/O 错误。</p>
     */
    public static final int EPOLL_CREATE = 0x1301;

    /**
     * 管理 epoll 监控项。
     *
     * <p><b>Stack</b>：入参 {@code (epfd:int, op:int, fd:int, events:int)} → 出参 {@code (rc:int)}</p>
     *
     * <p><b>语义</b>：
     * <ul>
     *   <li>op=1 (ADD)：将 fd 添加到 epoll 并关注指定事件</li>
     *   <li>op=2 (MOD)：修改已存在 fd 的事件掩码</li>
     *   <li>op=3 (DEL)：移除 fd 的监控</li>
     * </ul></p>
     *
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     *
     * <p><b>异常</b>：epfd 无效，op 非法，fd 未注册 (MOD/DEL)，或底层 I/O 错误。</p>
     */
    public static final int EPOLL_CTL = 0x1302;

    /**
     * 等待 epoll 事件。
     *
     * <p><b>Stack</b>：入参 {@code (epfd:int, max:int, timeout_ms:int)} → 出参 {@code (events:array)}</p>
     *
     * <p><b>语义</b>：从 epoll 实例中获取就绪事件，返回不超过 {@code max} 个。
     * 数组元素为 {@code {fd:int, events:int}} 的 map。</p>
     *
     * <p><b>返回</b>：就绪事件数组。</p>
     *
     * <p><b>异常</b>：epfd 无效，max 非法，或底层 I/O 错误。</p>
     */
    public static final int EPOLL_WAIT = 0x1303;

    /**
     * 跨平台 I/O 多路等待。
     *
     * <p><b>Stack</b>：入参 {@code (fds:array(fd,events), timeout_ms:int)} → 出参 {@code (events:array)}</p>
     *
     * <p><b>语义</b>：对指定的 fd 集合等待事件，可由底层封装到 select/epoll/poll。
     * 返回数组元素为 {@code {fd:int, events:int}} 的 map。</p>
     *
     * <p><b>返回</b>：就绪事件数组。</p>
     *
     * <p><b>异常</b>：fds 参数非法，timeout 非法，或底层 I/O 错误。</p>
     */
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
