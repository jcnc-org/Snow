package org.jcnc.snow.vm.engine;

import org.jcnc.snow.vm.io.EnvRegistry;

import java.io.IOException;

/**
 * SyscallOpCode —— 系统调用操作码表
 */
public final class SyscallOpCode {

    // region File Descriptor(FD) (0x1000 – 0x10FF)
    /**
     * 打开文件并返回一个新的 fd。
     *
     * <p><b>Stack</b>：入参 {@code (path:String, flags:int)} → 出参 {@code (fd:int)}</p>
     * <p><b>语义</b>：依据 {@code flags}（由 {@code OpenFlags} 解析为 {@code OpenOption} 集）打开
     * {@code path} 对应的文件，底层通过 {@code Files.newByteChannel(...)} 创建通道并注册到 {@code FDTable}。</p>
     * <p><b>异常</b>：
     * <ul>
     *   <li>路径/flags 类型错误时抛出 {@link IllegalArgumentException}</li>
     *   <li>flags 非法时抛出 {@link IllegalArgumentException}</li>
     *   <li>文件不存在且未指定创建、权限不足或底层 I/O 失败时抛出 {@link java.io.IOException}</li>
     * </ul>
     * </p>
     */
    public static final int OPEN = 0x1000;

    /**
     * 从 fd 对应的可读通道读取最多 {@code length} 字节。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int, length:int)} → 出参 {@code (data:byte[])}</p>
     * <p><b>语义</b>：若读到 EOF 或 {@code length <= 0}，返回长度为 0 的字节数组；否则返回实际读取的字节数组。</p>
     * <p><b>返回</b>：实际读取的字节数组。</p>
     * <p><b>异常</b>：
     * <ul>
     *   <li>fd 非法/不可读时抛出 {@link IllegalArgumentException}</li>
     *   <li>length 类型错误或为负时抛出 {@link IllegalArgumentException}</li>
     *   <li>I/O 失败时抛出 {@link java.io.IOException}</li>
     * </ul>
     * </p>
     */
    public static final int READ = 0x1001;

    /**
     * 向 fd 对应的可写通道写入字节数组。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int, data:byte[])} → 出参 {@code (written:int)}</p>
     * <p><b>语义</b>：写入 {@code data}，返回实际写入的字节数。
     * <ul>
     *   <li>byte[]：按原样写入</li>
     *   <li>String：UTF-8 编码写入</li>
     *   <li>null：写入长度 0</li>
     *   <li>其他类型：toString() 后 UTF-8 编码写入</li>
     * </ul>
     * </p>
     * <p><b>返回</b>：实际写入的字节数（int）。</p>
     * <p><b>异常</b>：
     * <ul>
     *   <li>fd 不是 int 时抛出 {@link IllegalArgumentException}</li>
     *   <li>fd 不可写时抛出 {@link IllegalArgumentException}</li>
     *   <li>写入失败时抛出 {@link java.io.IOException}</li>
     * </ul>
     * </p>
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
     * <p><b>返回</b>：无。</p>
     * <p><b>异常</b>：
     * <ul>
     *   <li>fd 非法时抛出 {@link IllegalArgumentException}</li>
     *   <li>I/O 操作失败时抛出 {@link java.io.IOException}</li>
     * </ul>
     * </p>
     */
    public static final int CLOSE = 0x1004;

    /**
     * 获取路径对应文件的基本属性。
     *
     * <p><b>Stack</b>：入参 {@code (path:String)} → 出参 {@code (attrs:Map)}</p>
     * <p><b>实现</b>：通过 {@code Files.readAttributes(..., BasicFileAttributes.class)} 填充
     * {@code size/isDirectory/isRegularFile/lastModified/created} 等键。</p>
     * <p><b>返回</b>：包含文件属性的 Map 对象。</p>
     * <p><b>异常</b>：路径类型错误，文件不存在，权限不足，I/O 失败。</p>
     */
    public static final int STAT = 0x1005;

    /**
     * 根据 fd 获取文件的基本属性。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int)} → 出参 {@code (attrs:Map)}</p>
     * <p><b>实现</b>：针对 {@code SeekableByteChannel}，至少提供
     * {@code size} 与 {@code position}；如未跟踪 Path，则 {@code lastModified/created} 等可能返回占位值（-1）。</p>
     * <p><b>返回</b>：包含文件属性的 Map 对象。</p>
     * <p><b>异常</b>：fd 非法/不可定位，I/O 失败。</p>
     */
    public static final int FSTAT = 0x1006;

    /**
     * 删除路径。
     *
     * <p><b>Stack</b>：入参 {@code (path:String)} → 出参：int（0 表示成功）</p>
     * <p><b>实现</b>：{@code Files.delete(path)}，若为非空目录会抛出相应异常。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
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

    // endregion

    // region File System(FS) (0x1100 – 0x11FF)
    /**
     * 创建目录（可选权限位）。
     *
     * <p><b>Stack</b>：入参 {@code (path:String, mode:int?)} → 出参 {@code (0:int)}</p>
     * <p><b>语义</b>：在 {@code path} 处新建目录。若 {@code mode} 未提供，则由实现采用默认权限；</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：
     * 路径非法、目录已存在、父目录不存在、权限不足或发生 I/O 错误时抛出异常。</p>
     */
    public static final int MKDIR = 0x1100;


    /**
     * 删除空目录。
     *
     * <p><b>Stack</b>：入参 {@code (path:String)} → 出参 {@code (0:int)}</p>
     * <p><b>语义</b>：仅当目录为空时删除；非空目录应返回错误。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：路径不是目录/目录非空/不存在/权限不足，I/O 失败。</p>
     */
    public static final int RMDIR = 0x1101;

    /**
     * 改变 VM 的当前工作目录（CWD）。
     *
     * <p><b>Stack</b>：入参 {@code (path:String)} → 出参 {@code (0:int)}</p>
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
     * <p><b>Stack</b>：入参 {@code (path:String)} → 出参 {@code (entries:String[])}</p>
     * <p><b>语义</b>：返回 {@code path} 下的直接子项列表（非递归）。实现可返回字符串数组（文件/目录名），
     * 或返回包含名称/类型等字段的条目对象数组。</p>
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
     * 在部分平台/运行时可能仅支持修改时间（atime 可能被忽略）。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：路径不存在/权限不足/时间值非法，I/O 失败。</p>
     */
    public static final int UTIME = 0x1107;

    // endregion

    // region 标准 IO / 控制台 (0x1200 – 0x12FF)
    /**
     * 从标准输入读取一行文本。
     *
     * <p><b>Stack</b>：入参：无 → 出参 {@code (line:String)}</p>
     * <p><b>语义</b>：从 {@code System.in} 读取一行字符串并返回；若到达 EOF 或读取失败，则返回空字符串。</p>
     * <p><b>返回</b>：读取到的字符串。</p>
     * <p><b>异常</b>：
     * <ul>
     *   <li>I/O 错误时抛出 {@link Exception}</li>
     * </ul>
     * </p>
     */
    public static final int STDIN_READ = 0x1200;

    /**
     * 向标准输出（stdout, fd=1）写入字节数组或字符串。
     *
     * <p><b>Stack</b>：入参 {@code (data:byte[]|String|Object)} → 出参 {@code (written:int)}</p>
     * <p><b>语义</b>：
     * <ul>
     *   <li>参数为 byte[] 时，按原始字节输出</li>
     *   <li>参数为 null 时，输出字符串 "null"</li>
     *   <li>其它类型，调用 {@code toString()} 后以 UTF-8 编码输出</li>
     * </ul>
     * 输出内容直接写到 System.out，不带自动换行。
     * </p>
     * <p><b>返回</b>：实际写入的字节数（int）。</p>
     * <p><b>异常</b>：
     * <ul>
     *   <li>操作数栈为空时抛出 {@link IllegalStateException}</li>
     *   <li>I/O 错误时抛出 {@link java.io.IOException}</li>
     * </ul>
     * </p>
     */
    public static final int STDOUT_WRITE = 0x1201;

    /**
     * 向标准错误输出（stderr）写入字符串。
     *
     * <p><b>Stack</b>：入参 {@code (data:Object)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：将对象转换为字符串（null 输出为 "null"），写入 {@code System.err}；
     * 操作完成后返回 {@code 0}。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：
     * <ul>
     *   <li>写入过程中发生 I/O 错误时抛出 {@link Exception}</li>
     * </ul>
     * </p>
     */
    public static final int STDERR_WRITE = 0x1202;
    // endregion

    // region 多路复用 (0x1300 – 0x13FF)
    /**
     * {@code SELECT} (0x1300) — I/O 多路复用系统调用。
     *
     * <p><b>Stack：</b>
     * 入参 {@code (readSet: List<int>, writeSet: List<int>, exceptSet: List<int>, timeout_ms:int)} →
     * 出参 {@code (ready: Map{ "read":List<int>, "write":List<int>, "except":List<int> })}
     * </p>
     *
     * <p><b>语义：</b>
     * 等待指定文件描述符集合的 I/O 就绪事件，返回三类结果：可读、可写、异常。
     * <ul>
     *   <li>支持 {@link java.nio.channels.SelectableChannel}（SocketChannel、ServerSocketChannel、DatagramChannel 等）</li>
     *   <li>兼容标准流：
     *     <ul>
     *       <li>fd=0 (stdin)：支持 READ，采用 {@link System#in} 可用性轮询</li>
     *       <li>fd=1/2 (stdout/stderr)：支持 WRITE，视为始终可写</li>
     *     </ul>
     *   </li>
     *   <li>{@code readSet} → {@link java.nio.channels.SelectionKey#OP_READ} / {@link java.nio.channels.SelectionKey#OP_ACCEPT}</li>
     *   <li>{@code writeSet} → {@link java.nio.channels.SelectionKey#OP_WRITE}</li>
     *   <li>{@code exceptSet} → {@link java.nio.channels.SelectionKey#OP_CONNECT}</li>
     * </ul>
     * </p>
     *
     * <p><b>返回：</b>
     * 成功时返回一个 {@code Map}，包含 {@code "read"}、{@code "write"}、{@code "except"} 三个键，
     * 其值为就绪 fd 列表。若无事件触发，则返回的列表为空。
     * </p>
     *
     * <p><b>异常：</b>
     * <ul>
     *   <li>参数类型非法时抛出 {@link IllegalArgumentException}</li>
     *   <li>底层 I/O 操作失败时抛出 {@link java.io.IOException}</li>
     *   <li>其他运行时错误时抛出 {@link RuntimeException}</li>
     * </ul>
     * </p>
     */
    public static final int SELECT = 0x1300;

    /**
     * {@code EPOLL_CREATE} (0x1301) — 创建 epoll 实例。
     *
     * <p><b>Stack：</b>
     * 入参 {@code (flags:int?)} → 出参 {@code (epfd:int)}
     * </p>
     *
     * <p><b>语义：</b>
     * 分配并注册一个新的 epoll 实例，返回对应的文件描述符 {@code epfd}。
     * </p>
     *
     * <p><b>返回：</b>
     * 成功时返回新的 epfd。
     * </p>
     *
     * <p><b>异常：</b>
     * flags 非法、资源不足或底层 I/O 错误时抛出异常。
     * </p>
     */
    public static final int EPOLL_CREATE = 0x1301;

    /**
     * {@code EPOLL_CTL} (0x1302) — 管理 epoll 监控项。
     *
     * <p><b>Stack：</b>
     * 入参 {@code (epfd:int, op:int, fd:int, events:int)} → 出参 {@code (rc:int)}
     * </p>
     *
     * <p><b>语义：</b>
     * 管理 epoll 实例中监控的文件描述符：
     * <ul>
     *   <li>{@code op=1 (ADD)} → 将 fd 添加并关注指定事件</li>
     *   <li>{@code op=2 (MOD)} → 修改已注册 fd 的事件掩码</li>
     *   <li>{@code op=3 (DEL)} → 移除 fd 的监控</li>
     * </ul>
     * </p>
     *
     * <p><b>返回：</b>
     * 成功时返回 {@code 0}。
     * </p>
     *
     * <p><b>异常：</b>
     * epfd 无效、op 非法、fd 未注册 (MOD/DEL) 或底层 I/O 错误时抛出异常。
     * </p>
     */
    public static final int EPOLL_CTL = 0x1302;

    /**
     * {@code EPOLL_WAIT} (0x1303) — 等待 epoll 事件。
     *
     * <p><b>Stack：</b>
     * 入参 {@code (epfd:int, max:int, timeout_ms:int)} → 出参 {@code (events: List<Map{fd:int, events:int}})}
     * </p>
     *
     * <p><b>语义：</b>
     * 阻塞或超时等待 epoll 实例中就绪的 I/O 事件，最多返回 {@code max} 个。
     * 每个事件为 {@code {"fd":int, "events":int}}。
     * </p>
     *
     * <p><b>返回：</b>
     * 成功时返回就绪事件数组。若无事件触发，则返回空数组。
     * </p>
     *
     * <p><b>异常：</b>
     * epfd 无效、max 非法或底层 I/O 错误时抛出异常。
     * </p>
     */
    public static final int EPOLL_WAIT = 0x1303;

    /**
     * {@code IO_WAIT} (0x1304) — 跨平台 I/O 多路等待。
     *
     * <p><b>Stack：</b>
     * 入参 {@code (fds: List<Map{fd:int, events:int}}, timeout_ms:int)} →
     * 出参 {@code (events: List<Map{fd:int, events:int}})}
     * </p>
     *
     * <p><b>语义：</b>
     * 对指定的 fd 集合等待就绪事件。底层可能封装为 select/epoll/poll 实现。
     * 返回的数组元素为 {@code {"fd":int, "events":int}}。
     * </p>
     *
     * <p><b>返回：</b>
     * 成功时返回就绪事件数组；若超时或无事件触发，则返回空数组。
     * </p>
     *
     * <p><b>异常：</b>
     * fds 参数非法、timeout 非法或底层 I/O 错误时抛出异常。
     * </p>
     */
    public static final int IO_WAIT = 0x1304;
    // endregion


    // region 套接字 & 网络 (0x1400 – 0x14FF)
    /**
     * 创建一个新的套接字。
     *
     * <p><b>Stack</b>：入参 {@code (family:int, type:int, proto:int)} → 出参 {@code (fd:int)}</p>
     * <p><b>语义</b>：根据协议族 family、类型 type 创建新的 socket，并返回 fd。</p>
     * <p><b>返回</b>：新分配的 socket fd（int）。</p>
     * <p><b>异常</b>：
     * <ul>
     *   <li>family 或 type 不支持时抛出 {@link UnsupportedOperationException}</li>
     *   <li>创建 socket 失败时抛出 {@link java.io.IOException}</li>
     * </ul>
     * </p>
     */
    public static final int SOCKET = 0x1400;

    /**
     * 绑定套接字到本地地址和端口。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int, addr:String, port:int)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：将套接字与指定地址/端口绑定。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：地址被占用、权限不足、套接字无效。</p>
     */
    public static final int BIND = 0x1401;

    /**
     * 监听套接字上的连接请求。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int, backlog:int)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：将绑定的套接字置为监听状态，backlog 指定队列长度。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：套接字未绑定、类型错误。</p>
     */
    public static final int LISTEN = 0x1402;

    /**
     * 接受客户端连接。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int)} → 出参 {@code (cfd:int, addr:String, port:int)}</p>
     * <p><b>语义</b>：在监听套接字上接受一个新的连接，返回新套接字和对端地址。</p>
     * <p><b>返回</b>：新连接的文件描述符。</p>
     * <p><b>异常</b>：无连接可接受、I/O 失败。</p>
     */
    public static final int ACCEPT = 0x1403;

    /**
     * 主动连接远程套接字。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int, addr:String, port:int)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：将套接字连接到指定的远端主机和端口。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：地址不可达、连接被拒绝、超时。</p>
     */
    public static final int CONNECT = 0x1404;

    /**
     * 在流式套接字上发送数据。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int, data:byte[]/String)} → 出参 {@code (n:int)}</p>
     * <p><b>语义</b>：向已连接的套接字写入数据。</p>
     * <p><b>返回</b>：实际写入的字节数。</p>
     * <p><b>异常</b>：套接字未连接、对端关闭、I/O 失败。</p>
     */
    public static final int SEND = 0x1405;

    /**
     * 从流式套接字接收数据。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int, n:int)} → 出参 {@code (bytes:byte[])}</p>
     * <p><b>语义</b>：从套接字读取至多 n 字节数据。</p>
     * <p><b>返回</b>：接收的数据字节数组。</p>
     * <p><b>异常</b>：套接字未连接、对端关闭、I/O 失败。</p>
     */
    public static final int RECV = 0x1406;

    /**
     * 在无连接套接字上发送数据。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int, data:any, addr:String, port:int)} → 出参 {@code (n:int)}</p>
     * <p><b>语义</b>：向指定地址/端口发送一个 UDP 报文。</p>
     * <p><b>返回</b>：实际发送的字节数。</p>
     * <p><b>异常</b>：套接字未绑定、I/O 失败。</p>
     */
    public static final int SENDTO = 0x1407;

    /**
     * 从无连接套接字接收数据。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int, n:int)} → 出参 {@code (bytes:byte[], addr:String, port:int)}</p>
     * <p><b>语义</b>：接收一个 UDP 报文，返回数据和来源地址。</p>
     * <p><b>返回</b>：接收到的数据及对端信息。</p>
     * <p><b>异常</b>：I/O 失败。</p>
     */
    public static final int RECVFROM = 0x1408;

    /**
     * 关闭套接字的读/写方向。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int, how:int)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：根据 how 值关闭输入、输出或双向。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：套接字无效、I/O 失败。</p>
     */
    public static final int SHUTDOWN = 0x1409;

    /**
     * 设置套接字选项。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int, level:int, opt:int, value:any)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：修改套接字的控制选项，如缓冲区大小、TCP_NODELAY 等。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：选项不支持、值非法。</p>
     */
    public static final int SETSOCKOPT = 0x140A;

    /**
     * 获取套接字选项。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int, level:int, opt:int)} → 出参 {@code (value:any)}</p>
     * <p><b>语义</b>：读取套接字的控制选项值。</p>
     * <p><b>返回</b>：选项的当前值。</p>
     * <p><b>异常</b>：选项不支持、套接字无效。</p>
     */
    public static final int GETSOCKOPT = 0x140B;

    /**
     * 获取套接字对端地址。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int)} → 出参 {@code (addr:String, port:int)}</p>
     * <p><b>语义</b>：返回连接的远程主机地址与端口。</p>
     * <p><b>返回</b>：对端地址和端口。</p>
     * <p><b>异常</b>：套接字未连接、I/O 失败。</p>
     */
    public static final int GETPEERNAME = 0x140C;

    /**
     * 获取套接字本地地址。
     *
     * <p><b>Stack</b>：入参 {@code (fd:int)} → 出参 {@code (addr:String, port:int)}</p>
     * <p><b>语义</b>：返回套接字绑定的本地地址与端口。</p>
     * <p><b>返回</b>：本端地址和端口。</p>
     * <p><b>异常</b>：套接字未绑定、I/O 失败。</p>
     */
    public static final int GETSOCKNAME = 0x140D;

    /**
     * 解析主机名和服务名为地址信息。
     *
     * <p><b>Stack</b>：入参 {@code (host:String, service:String, hints:Map?)} → 出参 {@code (results:List)}</p>
     * <p><b>语义</b>：执行 DNS 解析，返回可用的地址列表。</p>
     * <p><b>返回</b>：地址数组，每项包含 addr、port、family 等。</p>
     * <p><b>异常</b>：主机未知、服务非法、解析失败。</p>
     */
    public static final int GETADDRINFO = 0x140E;
    // endregion


    //  region 进程 & 线程 (0x1500 – 0x15FF)
    /**
     * 结束当前进程/线程。
     *
     * <p><b>Stack</b>：入参 {@code (code:int)} → 出参 —</p>
     * <p><b>语义</b>：终止当前进程，退出码为 {@code code}。</p>
     * <p><b>返回</b>：无（不会返回）。</p>
     * <p><b>异常</b>：调用环境可能抛出 VM 终止异常。</p>
     */
    public static final int EXIT = 0x1500;

    /**
     * 启动外部进程（同步版）。
     *
     * <p><b>Stack：</b>入参 {@code (cmd:List<String>)} → 出参 {@code (pid:int)}</p>
     *
     * <p><b>语义：</b>
     * <ul>
     *   <li>以命令参数 {@code cmd} 启动一个新的系统进程，{@code cmd[0]} 通常为可执行程序路径。</li>
     *   <li>子进程继承当前虚拟机的环境变量（{@link EnvRegistry#snapshot()}）。</li>
     *   <li>标准输入继承父进程，标准输出和错误输出实时转发到当前控制台。</li>
     *   <li>当前线程阻塞，直到子进程执行结束后返回。</li>
     * </ul>
     * </p>
     *
     * <p><b>返回：</b>
     * <ul>
     *   <li>成功时返回子进程的 PID（int），如无法获取则返回 {@code 0}。</li>
     *   <li>启动失败时返回 {@code -1} 并抛出异常。</li>
     * </ul>
     * </p>
     *
     * <p><b>异常：</b>
     * <ul>
     *   <li>参数类型不符时抛出 {@link IllegalArgumentException}。</li>
     *   <li>命令数组元素不是字符串时抛出 {@link IllegalArgumentException}。</li>
     *   <li>I/O 错误（进程启动失败）时抛出 {@link java.io.IOException}。</li>
     * </ul>
     * </p>
     *
     * <p><b>注意事项：</b>
     * <ul>
     *   <li>此调用为同步版本，会阻塞直到子进程退出。</li>
     *   <li>若需异步执行，可使用独立的 {@code spawn} 或异步 {@code fork} 版本。</li>
     * </ul>
     * </p>
     */
    public static final int FORK = 0x1501;


    /**
     * 启动新进程并用其输出“接管”当前控制台，随后终结当前 VM 进程。
     *
     * <p><b>Stack：</b>
     * 入参 {@code (env:Map<String,String>, argv:List<String>, path:String)} → 无返回
     * </p>
     *
     * <p><b>语义：</b>
     * <ul>
     *   <li>以给定可执行文件 {@code path} 及参数 {@code argv} 启动一个新进程</li>
     *   <li>新进程环境变量 = 虚拟机当前的 {@link EnvRegistry#snapshot()}，再叠加 {@code env}</li>
     *   <li>新进程的标准输出/错误输出会被实时转发到当前控制台</li>
     *   <li>当前线程会阻塞等待该进程结束，然后当前 VM 会直接退出（不返回 Snow 代码）</li>
     * </ul>
     * </p>
     *
     * <p><b>返回：</b>
     * <ul>
     *   <li>无返回值：本系统调用不会返回到调用方。目标进程结束后，当前 VM 直接终止</li>
     * </ul>
     * </p>
     *
     * <p><b>异常：</b>
     * <ul>
     *   <li>path 不是 String 时抛出 {@link IllegalArgumentException}</li>
     *   <li>进程启动失败时抛出 {@link IOException}</li>
     *   <li>栈参数不足时抛出 {@link IllegalStateException}</li>
     * </ul>
     * </p>
     *
     * <p><b>注意事项：</b>
     * <ul>
     *   <li>这是“接管式”执行：Snow 进程最终会终止，不会回到调用点继续运行</li>
     *   <li>与传统 Unix {@code execve()} 类似，但在实际实现层面我们会等待子进程退出后再终止自身，
     *       以确保在 GraalVM native-image 下也能正确刷新/显示子进程输出</li>
     * </ul>
     * </p>
     */
    public static final int EXEC = 0x1502;

    /**
     * 等待子进程结束。
     *
     * <p><b>Stack</b>：入参 {@code (pid:int?)} → 出参 {@code (status:int)}</p>
     * <p><b>语义</b>：阻塞等待子进程退出。</p>
     * <p><b>返回</b>：子进程的退出状态码。</p>
     * <p><b>异常</b>：进程不存在、无子进程、等待失败。</p>
     */
    public static final int WAIT = 0x1503;

    /**
     * 获取当前进程号。
     *
     * <p><b>Stack</b>：无入参 → 出参 {@code (pid:int)}</p>
     * <p><b>语义</b>：返回当前 JVM 进程的 pid。</p>
     * <p><b>返回</b>：成功时返回当前进程 pid（int）。</p>
     * <p><b>异常</b>：
     * <ul>
     *   <li>如果当前 JVM 版本不支持 {@link ProcessHandle#current()}，可能抛出 {@link UnsupportedOperationException}</li>
     * </ul>
     * </p>
     */
    public static final int GETPID = 0x1504;

    /**
     * 获取父进程号。
     *
     * <p><b>Stack</b>：入参 — → 出参 {@code (ppid:int)}</p>
     * <p><b>语义</b>：返回当前进程的父进程标识符。</p>
     * <p><b>返回</b>：父进程 pid。</p>
     * <p><b>异常</b>：无。</p>
     */
    public static final int GETPPID = 0x1505;

    /**
     * 创建线程。
     *
     * <p><b>Stack</b>：入参 {@code (entry:fn/ptr, arg:any)} → 出参 {@code (tid:int)}</p>
     * <p><b>语义</b>：创建一个新线程，并在线程中执行 {@code entry}。</p>
     * <p><b>返回</b>：新线程的线程 ID。</p>
     * <p><b>异常</b>：线程创建失败、资源不足。</p>
     */
    public static final int THREAD_CREATE = 0x1506;

    /**
     * 等待线程结束。
     *
     * <p><b>Stack</b>：入参 {@code (tid:int)} → 出参 {@code (retval:any?)}</p>
     * <p><b>语义</b>：阻塞等待指定线程退出，并获取其返回值。</p>
     * <p><b>返回</b>：线程的返回值，可能为 {@code null}。</p>
     * <p><b>异常</b>：线程不存在、已结束或 join 失败。</p>
     */
    public static final int THREAD_JOIN = 0x1507;

    /**
     * 休眠当前线程。
     *
     * <p><b>Stack</b>：入参 {@code (ms:int)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：让当前线程挂起指定毫秒数。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：被中断时可能抛出异常。</p>
     */
    public static final int SLEEP = 0x1508;
    // endregion


    // region 并发原语 (0x1600 – 0x16FF)

    /**
     * 创建一个新的互斥量。
     *
     * <p><b>Stack</b>：入参 {@code ()} → 出参 {@code (mid:int)}</p>
     * <p><b>语义</b>：分配并返回一个互斥量 ID。</p>
     * <p><b>返回</b>：成功返回新互斥量的 ID。</p>
     * <p><b>异常</b>：资源不足或内部错误时抛出异常。</p>
     */
    public static final int MUTEX_NEW = 0x1600;

    /**
     * 加锁互斥量。
     *
     * <p><b>Stack</b>：入参 {@code (mid:int)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：阻塞直到获得互斥量锁。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：无效 ID 或线程被中断时抛出异常。</p>
     */
    public static final int MUTEX_LOCK = 0x1601;

    /**
     * 尝试加锁互斥量（非阻塞）。
     *
     * <p><b>Stack</b>：入参 {@code (mid:int)} → 出参 {@code (ok:int)}</p>
     * <p><b>语义</b>：立即尝试获取互斥量。</p>
     * <p><b>返回</b>：成功返回 {@code 1}，失败返回 {@code 0}。</p>
     * <p><b>异常</b>：无效 ID 时抛出异常。</p>
     */
    public static final int MUTEX_TRYLOCK = 0x1602;

    /**
     * 解锁互斥量。
     *
     * <p><b>Stack</b>：入参 {@code (mid:int)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：释放当前线程持有的互斥量。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：若线程未持有锁则抛出异常。</p>
     */
    public static final int MUTEX_UNLOCK = 0x1603;

    /**
     * 创建一个新的条件变量。
     *
     * <p><b>Stack</b>：入参 {@code ()} → 出参 {@code (cid:int)}</p>
     * <p><b>语义</b>：分配并返回一个条件变量 ID。</p>
     * <p><b>返回</b>：成功返回新条件变量的 ID。</p>
     */
    public static final int COND_NEW = 0x1604;

    /**
     * 等待条件变量。
     *
     * <p><b>Stack</b>：入参 {@code (cid:int, mid:int, timeout_ms:int?)} → 出参 {@code (reason:int)}</p>
     * <p><b>语义</b>：在释放互斥量后等待条件满足，返回前会重新加锁。</p>
     * <p><b>返回</b>：{@code 0}=信号唤醒，{@code 1}=超时，{@code -1}=被中断。</p>
     * <p><b>异常</b>：无效 ID 时抛出异常。</p>
     */
    public static final int COND_WAIT = 0x1605;

    /**
     * 唤醒一个等待在条件变量上的线程。
     *
     * <p><b>Stack</b>：入参 {@code (cid:int)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：对条件变量执行一次 {@code signal}。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：无效 ID 时抛出异常。</p>
     */
    public static final int COND_SIGNAL = 0x1606;

    /**
     * 唤醒所有等待在条件变量上的线程。
     *
     * <p><b>Stack</b>：入参 {@code (cid:int)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：对条件变量执行一次 {@code broadcast}。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：无效 ID 时抛出异常。</p>
     */
    public static final int COND_BROADCAST = 0x1607;

    /**
     * 创建一个新的信号量。
     *
     * <p><b>Stack</b>：入参 {@code (init:int)} → 出参 {@code (sid:int)}</p>
     * <p><b>语义</b>：分配并返回一个初始值为 {@code init} 的信号量。</p>
     * <p><b>返回</b>：成功返回新信号量的 ID。</p>
     */
    public static final int SEM_NEW = 0x1608;

    /**
     * 等待信号量。
     *
     * <p><b>Stack</b>：入参 {@code (sid:int, timeout_ms:int?)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：尝试获取信号量。</p>
     * <p><b>返回</b>：成功 {@code 1}，超时 {@code 0}，中断 {@code -1}。</p>
     * <p><b>异常</b>：无效 ID 时抛出异常。</p>
     */
    public static final int SEM_WAIT = 0x1609;

    /**
     * 释放信号量。
     *
     * <p><b>Stack</b>：入参 {@code (sid:int)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：对信号量执行一次 {@code post}。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     */
    public static final int SEM_POST = 0x160A;

    /**
     * 创建一个新的读写锁。
     *
     * <p><b>Stack</b>：入参 {@code ()} → 出参 {@code (rwl:int)}</p>
     * <p><b>语义</b>：分配并返回一个读写锁 ID。</p>
     * <p><b>返回</b>：成功返回新读写锁的 ID。</p>
     */
    public static final int RWLOCK_NEW = 0x160B;

    /**
     * 获取读写锁的读锁。
     *
     * <p><b>Stack</b>：入参 {@code (rwl:int)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：阻塞直到获取读锁。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     */
    public static final int RWLOCK_RLOCK = 0x160C;

    /**
     * 获取读写锁的写锁。
     *
     * <p><b>Stack</b>：入参 {@code (rwl:int)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：阻塞直到获取写锁。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     */
    public static final int RWLOCK_WLOCK = 0x160D;

    /**
     * 解锁读写锁。
     *
     * <p><b>Stack</b>：入参 {@code (rwl:int)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：释放当前线程持有的读锁或写锁。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：若线程未持有任何锁则抛出异常。</p>
     */
    public static final int RWLOCK_UNLOCK = 0x160E;

    // endregion


    // region 时间 & 计时 (0x1700 – 0x17FF)
    /**
     * 获取时钟的纳秒时间戳。
     *
     * <p><b>Stack</b>：入参 {@code (clockId:int)} → 出参 {@code (nanos:long)}</p>
     * <p><b>语义</b>：
     * 根据 {@code clockId} 返回纳秒时间戳:
     * <ul>
     *   <li>{@code 0 (REALTIME)}: 自 Unix 纪元的墙钟时间，单位纳秒</li>
     *   <li>{@code 1 (MONO)}: 单调时钟，适用于测量时间间隔</li>
     * </ul>
     * </p>
     * <p><b>返回</b>：成功时返回纳秒时间 {@code (long)}。</p>
     * <p><b>异常</b>：
     * <ul>
     *   <li>若 {@code clockId} 非法或不被支持，抛出 {@link IllegalArgumentException}</li>
     * </ul>
     * </p>
     */
    public static final int CLOCK_GETTIME = 0x1700;

    /**
     * 按纳秒级别挂起当前线程。
     *
     * <p><b>Stack</b>：入参 {@code (ns:long)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：让当前线程挂起指定的纳秒数（尽量靠近请求的时长，但不保证精确到每个纳秒）。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。</p>
     * <p><b>异常</b>：被中断时可能抛出 {@link InterruptedException}（或以实现定义的方式报告中断）；调用者应处理或恢复中断状态。</p>
     */
    public static final int NANOSLEEP = 0x1701;

    /**
     * 兼容式获取当前时间（秒 + 微秒 部分）。
     *
     * <p><b>Stack</b>：入参 {@code ()} → 出参 {@code (sec:long, usec:int)}</p>
     * <p><b>语义</b>：返回自 Unix 纪元（1970-01-01T00:00:00Z）以来的秒数（{@code sec}）和当前秒内的微秒部分（{@code usec}, 0..999_999）。</p>
     * <p><b>返回</b>：成功返回两个值：{@code sec:long} 和 {@code usec:int}（注意压栈顺序由实现约定）。</p>
     * <p><b>异常</b>：通常不抛出异常，但实现可能在极特殊环境下抛出运行时异常。</p>
     */
    public static final int TIMEOFDAY = 0x1702;

    /**
     * 获取单调时钟的毫秒数（用于计时/测量）。
     *
     * <p><b>Stack</b>：入参 {@code ()} → 出参 {@code (ms:long)}</p>
     * <p><b>语义</b>：返回一个单调时间的毫秒表示（例如基于 {@code System.nanoTime()} 的转换），适合计算时间间隔；该值不是绝对墙钟时间，可能没有固定的参考起点。</p>
     * <p><b>返回</b>：成功返回单调毫秒数 {@code (long)}。</p>
     * <p><b>异常</b>：通常不抛出异常。</p>
     */
    public static final int TICK_MS = 0x1703;
    // endregion


    // region 数组操作 (0x1800 – 0x18FF)
    /**
     * ARR_LEN (0x1801)
     *
     * <p><b>Stack</b>：入参 {@code (arr:any)} → 出参 {@code (len:int)}</p>
     *
     * <p><b>语义</b>：返回数组/列表/字符串的长度。
     * <ul>
     *   <li>支持 {@link java.util.List}、原生 Java 数组、{@link CharSequence}</li>
     *   <li>若为 {@code null} 则视为长度 0</li>
     * </ul>
     * </p>
     *
     * <p><b>返回</b>：长度 {@code (int)}。若 arr 为 null，返回 0。</p>
     *
     * <p><b>异常</b>：若类型不支持，则抛出 {@link IllegalArgumentException}。</p>
     */
    public static final int ARR_LEN = 0x1801;
    /**
     * ARR_GET (0x1802)
     *
     * <p><b>Stack</b>：入参 {@code (arr:any, index:int)} → 出参 {@code (elem:any)}</p>
     *
     * <p><b>语义</b>：获取数组/列表在指定索引位置的元素。</p>
     *
     * <p><b>支持</b>：{@link java.util.List}、原生 Java 数组、{@link CharSequence}。</p>
     *
     * <p><b>返回</b>：对应索引位置的元素；若是 {@link CharSequence}，返回 {@code char} 或其包装形式。</p>
     *
     * <p><b>异常</b>：若 {@code arr} 为 {@code null} 或索引越界，应抛出 {@link IndexOutOfBoundsException} 或 {@link IllegalArgumentException}。</p>
     */
    public static final int ARR_GET = 0x1802;
    /**
     * ARR_SET (0x1803)
     *
     * <p><b>Stack</b>：入参 {@code (arr:any, index:int, value:any)} → 出参 {@code ()}</p>
     *
     * <p><b>语义</b>：设置数组/列表在指定索引位置的元素为给定值。</p>
     *
     * <p><b>支持</b>：{@link java.util.List}、原生 Java 数组。</p>
     *
     * <p><b>限制</b>：不可变类型（如 {@link CharSequence}）不支持写操作。</p>
     *
     * <p><b>异常</b>：若 {@code arr} 为 {@code null}、索引越界或类型不支持写入，应抛出 {@link IndexOutOfBoundsException} 或 {@link IllegalArgumentException}。</p>
     */
    public static final int ARR_SET = 0x1803;
    /**
     * ARR_PUSH (0x1810)
     *
     * <p><b>Stack</b>：入参 {@code (arr:any, value:any)} → 出参 {@code (len:int)}</p>
     *
     * <p><b>语义</b>：
     * 向列表尾部追加一个元素，并返回追加后的长度。
     * </p>
     *
     * <p><b>支持</b>：仅支持 {@link java.util.List}（可变序列）。</p>
     *
     * <p><b>返回</b>：追加后的长度。</p>
     *
     * <p><b>异常</b>：若 arr 不是列表，抛出 {@link IllegalArgumentException}。</p>
     */
    public static final int ARR_PUSH = 0x1810;
    /**
     * ARR_POP (0x1811)
     *
     * <p><b>Stack</b>：入参 {@code (arr:any)} → 出参 {@code (elem:any)}</p>
     *
     * <p><b>语义</b>：
     * 移除并返回列表末尾的元素。
     * </p>
     *
     * <p><b>支持</b>：仅 {@link java.util.List}。</p>
     *
     * <p><b>返回</b>：被移除的元素。</p>
     *
     * <p><b>异常</b>：
     * <ul>
     *   <li>若 arr 不是列表，抛出 {@link IllegalArgumentException}</li>
     *   <li>若列表为空，抛出 {@link IndexOutOfBoundsException}</li>
     * </ul>
     * </p>
     */
    public static final int ARR_POP = 0x1811;
    /**
     * ARR_INSERT (0x1812)
     *
     * <p><b>Stack</b>：入参 {@code (arr:any, index:int, value:any)} → 出参 {@code (len:int)}</p>
     *
     * <p><b>语义</b>：
     * 在指定索引插入元素，原位置及之后的元素向后移动。
     * </p>
     *
     * <p><b>支持</b>：仅 {@link java.util.List}。</p>
     *
     * <p><b>返回</b>：插入后的长度。</p>
     *
     * <p><b>异常</b>：
     * <ul>
     *   <li>若 arr 不是列表，抛出 {@link IllegalArgumentException}</li>
     *   <li>若索引不在 [0, size] 范围内，抛出 {@link IndexOutOfBoundsException}</li>
     * </ul>
     * </p>
     */
    public static final int ARR_INSERT = 0x1812;
    /**
     * ARR_REMOVE (0x1813)
     *
     * <p><b>Stack</b>：入参 {@code (arr:any, index:int)} → 出参 {@code (elem:any)}</p>
     *
     * <p><b>语义</b>：
     * 删除指定索引的元素，并返回被删除的值。
     * </p>
     *
     * <p><b>支持</b>：仅 {@link java.util.List}。</p>
     *
     * <p><b>返回</b>：被移除的元素。</p>
     *
     * <p><b>异常</b>：
     * <ul>
     *   <li>若 arr 不是列表，抛出 {@link IllegalArgumentException}</li>
     *   <li>若索引越界，抛出 {@link IndexOutOfBoundsException}</li>
     * </ul>
     * </p>
     */
    public static final int ARR_REMOVE = 0x1813;
    /**
     * ARR_RESIZE (0x1814)
     *
     * <p><b>Stack</b>：入参 {@code (arr:any, newLen:int)} → 出参 {@code (len:int)}</p>
     *
     * <p><b>语义</b>：
     * 调整列表长度：
     * <ul>
     *   <li>若 newLen 小于当前长度，则从末尾删除多余元素</li>
     *   <li>若 newLen 大于当前长度，则在末尾补 {@code null}</li>
     * </ul>
     * </p>
     *
     * <p><b>支持</b>：仅 {@link java.util.List}。</p>
     *
     * <p><b>返回</b>：调整后的长度（即 newLen）。</p>
     *
     * <p><b>异常</b>：
     * <ul>
     *   <li>若 newLen 为负数，抛出 {@link IllegalArgumentException}</li>
     *   <li>若 arr 不是列表，抛出 {@link IllegalArgumentException}</li>
     * </ul>
     * </p>
     */
    public static final int ARR_RESIZE = 0x1814;
    /**
     * ARR_CLEAR (0x1815)
     *
     * <p><b>Stack</b>：入参 {@code (arr:any)} → 出参 {@code (len:int)}</p>
     *
     * <p><b>语义</b>：
     * 清空列表，使其长度变为 0。
     * </p>
     *
     * <p><b>支持</b>：仅 {@link java.util.List}。</p>
     *
     * <p><b>返回</b>：0（清空后的长度）。</p>
     *
     * <p><b>异常</b>：
     * 若 arr 不是 {@link java.util.List}，抛出 {@link IllegalArgumentException}。
     * </p>
     */
    public static final int ARR_CLEAR = 0x1815;
    // endregion


    // region 系统信息 (0x1900 – 0x19FF)
    /**
     * 获取环境变量的值。
     *
     * <p><b>Stack</b>：入参 {@code (key:string)} → 出参 {@code (val:string?)}。</p>
     * <p><b>语义</b>：
     * <ul>
     *   <li>优先从 VM 环境变量覆盖层（{@link org.jcnc.snow.vm.io.EnvRegistry}）查找指定 key</li>
     *   <li>若无，则回退查找 System.getenv（系统环境变量）</li>
     *   <li>如变量不存在，返回 {@code null}，不抛出异常</li>
     * </ul>
     * </p>
     * <p><b>返回</b>：
     * <ul>
     *   <li>指定环境变量的值（{@code String}），不存在则为 {@code null}</li>
     * </ul>
     * </p>
     * <p><b>异常</b>：
     * <ul>
     *   <li>参数不足时抛出 {@link IllegalStateException}</li>
     *   <li>其它参数类型错误时不抛出异常，自动转字符串</li>
     * </ul>
     * </p>
     */
    public static final int GETENV = 0x1900;

    /**
     * 设置或删除环境变量（仅影响子进程环境）。
     *
     * <p><b>Stack</b>：入参 {@code (key:string, val:string?, overwrite:int)} → 出参 {@code (rc:int)}</p>
     * <p><b>语义</b>：
     * 将环境变量 {@code key} 设置为 {@code val}（当 {@code val} 为 {@code null} 时删除该变量）。
     * 参数 {@code overwrite} 为 {@code 1} 表示强制覆盖已存在的变量；为 {@code 0} 表示若变量已存在则不修改。</p>
     * <p><b>返回</b>：成功返回 {@code 0}。当 {@code overwrite==0} 且变量已存在时也返回 {@code 0}（未修改）。</p>
     * <p><b>异常</b>：参数缺失、类型错误或 {@code overwrite} 非 {@code 0}/{@code 1} 时可能抛出 {@link IllegalStateException}/{@link IllegalArgumentException}。</p>
     * <p><b>注意</b>：实现使用 {@link ProcessBuilder#environment()}，因此只影响由 JVM 启动的子进程，不保证修改全局或当前 JVM 的环境变量。</p>
     */
    public static final int SETENV = 0x1901;

    /**
     * 返回逻辑处理器（CPU）数量。
     *
     * <p><b>Stack</b>：入参 {@code ()} → 出参 {@code (n:int)}</p>
     * <p><b>语义</b>：返回运行时可用的逻辑处理器数（等同于 {@code Runtime.getRuntime().availableProcessors()}）。</p>
     * <p><b>返回</b>：成功返回一个非负整数 {@code n}。</p>
     * <p><b>异常</b>：正常情况下不抛出异常；底层平台极端故障时可能抛出运行时异常。</p>
     */
    public static final int NCPU = 0x1902;

    /**
     * 生成指定长度的随机字节数组。
     *
     * <p><b>Stack</b>：入参 {@code (n:int)} → 出参 {@code (bytes:byte[])}</p>
     * <p><b>语义</b>：生成长度为 {@code n} 的随机字节数组并返回，使用安全随机数生成器（{@link java.security.SecureRandom}）。</p>
     * <p><b>返回</b>：成功返回长度为 {@code n} 的 {@code byte[]}；当 {@code n==0} 返回空数组。</p>
     * <p><b>异常</b>：若参数缺失、为 {@code null}、无法解析为整数、为负数、或超过允许的最大值（实现中默认上限为 10_000_000）时，会抛出 {@link IllegalStateException} 或 {@link IllegalArgumentException}。</p>
     * <p><b>注意</b>：为避免 OOM，调用方应避免请求过大的 {@code n}；实现可能对最大可请求字节数设定限制。</p>
     */
    public static final int RANDOM_BYTES = 0x1903;

    /**
     * 获取最近一次系统调用的错误字符串（errstr）。
     *
     * <p><b>Stack</b>：入参 {@code ()} → 出参 {@code (errstr:string)}</p>
     * <p><b>语义</b>：将最近一次系统调用产生的错误信息字符串压入栈顶。</p>
     * <p><b>返回</b>：若最近一次系统调用没有错误，则返回空字符串 {@code ""}；否则返回错误信息文本。</p>
     * <p><b>异常</b>：正常情况下不抛出异常；若底层错误信息读取失败可能抛出运行时异常。</p>
     */
    public static final int ERRSTR = 0x1904;

    /**
     * 获取最近一次系统调用的 errno（错误码）。
     *
     * <p><b>Stack</b>：入参 {@code ()} → 出参 {@code (errno:int)}</p>
     * <p><b>语义</b>：将最近一次系统调用对应的整数错误码压入栈顶。</p>
     * <p><b>返回</b>：无错误时返回 {@code 0}；非零值表示具体错误码（语义由上层/平台定义）。</p>
     * <p><b>异常</b>：正常情况下不抛出异常；若读取 errno 失败可能抛出运行时异常。</p>
     */
    public static final int ERRNO = 0x1905;

    /**
     * 获取内存与系统资源信息。
     *
     * <p><b>Stack</b>：入参 {@code ()} → 出参 {@code (map:Map<String,Object>)}</p>
     * <p><b>语义</b>：收集 JVM 堆内存信息以及在可用时的操作系统物理内存 / CPU 负载等指标，返回为键值映射。</p>
     * <p><b>返回（典型键）</b>：
     * <ul>
     *   <li>{@code "heapTotal"}: JVM 堆已分配总字节数（long）</li>
     *   <li>{@code "heapFree"}: JVM 堆空闲字节数（long）</li>
     *   <li>{@code "heapUsed"}: JVM 堆已使用字节数（long）</li>
     *   <li>{@code "heapMax"}: JVM 堆最大可用字节数（long）</li>
     *   <li>{@code "physicalTotal"}: 物理内存总量（long，平台可用时提供）</li>
     *   <li>{@code "physicalFree"}: 物理内存空闲量（long，平台可用时提供）</li>
     *   <li>{@code "physicalUsed"}: 物理内存已用量（long，平台可用时提供）</li>
     * </ul>
     * </p>
     * <p><b>异常</b>：收集平台级指标时若遇到权限或平台差异，处理器会忽略这些额外项并仍返回 JVM heap 信息；因此通常不会向上抛出异常。</p>
     */
    public static final int MEMINFO = 0x1906;
    // endregion
}
