package org.jcnc.snow.vm.commands.system.control;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.file.StandardOpenOption.*;

/**
 * {@code SyscallCommand} —— I/O syscall 子命令集合指令。
 *
 * <p>
 * 封装类 UNIX 文件描述符（File Descriptor, FD）操作、文件/网络 I/O、管道、fd 复制、多路复用（select）等系统调用能力，
 * 基于 Java NIO 统一实现，供虚拟机指令集以 SYSCALL 形式扩展使用。
 * </p>
 *
 * <b>栈操作约定：</b>
 * <ul>
 *   <li>参数按“右值先入栈、左值后入栈”的顺序推入操作数栈，执行 {@code SYSCALL <SUBCMD>} 后出栈。</li>
 *   <li>即：调用 SYSCALL OPEN path flags mode，入栈顺序为 path（先入）→ flags → mode（后入），出栈时 mode→flags→path 弹出。</li>
 * </ul>
 *
 * <b>返回值说明：</b>
 * <ul>
 *   <li>成功：压入正值（如 FD、实际数据、字节数、0 表示成功）。</li>
 *   <li>失败：统一压入 -1，后续可扩展为 errno 机制。</li>
 * </ul>
 *
 * <b>支持的子命令（部分）：</b>
 * <ul>
 *   <li>PRINT / PRINTLN —— 控制台输出</li>
 *   <li>OPEN / CLOSE / READ / WRITE / SEEK —— 文件 I/O 操作</li>
 *   <li>PIPE / DUP —— 管道和 FD 复制</li>
 *   <li>SOCKET / CONNECT / BIND / LISTEN / ACCEPT —— 网络套接字</li>
 *   <li>SELECT —— I/O 多路复用</li>
 * </ul>
 */
public class SyscallCommand implements Command {

    /*------------------------------------ 工具方法 ------------------------------------*/

    /**
     * <b>POSIX open 标志到 Java NIO OpenOption 的映射</b>
     * <p>
     * 将 Linux/UNIX 的 open 调用 flags 参数，转换为 Java NIO 的 OpenOption 集合。
     * 目前仅支持 WRITE（0x1）、READ、CREATE（0x40）、TRUNCATE（0x200）、APPEND（0x400）等常用标志。
     * </p>
     *
     * @param flags POSIX 风格 open 标志（如 O_WRONLY=0x1, O_CREAT=0x40 等）
     * @return 映射后的 OpenOption 集合
     */
    private static Set<OpenOption> flagsToOptions(int flags) {
        Set<OpenOption> opts = new HashSet<>();
        // 0x1 = WRITE，否则默认 READ
        if ((flags & 0x1) != 0) opts.add(WRITE);
        else opts.add(READ);
        if ((flags & 0x40) != 0) opts.add(CREATE);
        if ((flags & 0x200) != 0) opts.add(TRUNCATE_EXISTING);
        if ((flags & 0x400) != 0) opts.add(APPEND);
        return opts;
    }

    /**
     * <b>统一异常处理</b>：
     * <p>
     * 捕获 syscall 内部所有异常，将 -1 压入操作数栈，表示系统调用失败（暂不区分错误类型）。
     * 常见异常如文件不存在、权限不足、通道类型不符、网络故障等。
     * </p>
     *
     * @param stack 当前操作数栈
     * @param e 捕获的异常对象
     */
    private static void pushErr(OperandStack stack, Exception e) {
        stack.push(-1);  // 目前统一用 -1，后续可按异常类型/errno 映射
    }

    /*--------------------------------------------------------------------------------*/

    /**
     * <b>执行 SYSCALL 子命令</b>：
     * <p>
     * 按照 parts[1] 指定的 SYSCALL 子命令，结合虚拟机栈与资源表完成对应的系统调用模拟。
     * 支持基础文件、网络、管道等 I/O 操作，并对异常统一处理。
     * </p>
     *
     * @param parts 指令字符串数组，parts[1] 为子命令
     * @param pc 当前指令计数器
     * @param stack 操作数栈
     * @param locals 局部变量表
     * @param callStack 调用栈
     * @return 下一条指令的 pc 值（默认 pc+1）
     */
    @Override
    public int execute(String[] parts, int pc,
                       OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) {

        if (parts.length < 2)
            throw new IllegalArgumentException("SYSCALL needs sub-command");

        String cmd = parts[1].toUpperCase(Locale.ROOT);

        try {
            switch (cmd) {

                /*==================== 文件 / 目录 ====================*/

                /*
                 * OPEN —— 打开文件，返回文件描述符（File Descriptor, FD）。
                 * 入栈（push）顺序: path（文件路径, String）, flags（int）, mode（int，文件权限，暂未用）
                 * 出栈（pop）顺序: mode → flags → path
                 * 成功返回 fd，失败返回 -1。
                 */
                case "OPEN" -> {
                    int mode = (Integer) stack.pop();      // 目前未用
                    int flags = (Integer) stack.pop();
                    String path = String.valueOf(stack.pop());
                    FileChannel fc = FileChannel.open(Paths.get(path), flagsToOptions(flags));
                    stack.push(FDTable.register(fc));
                }
                /*
                 * CLOSE —— 关闭文件描述符。
                 * 入栈顺序: fd（int）
                 * 出栈顺序: fd
                 * 返回 0 成功，-1 失败
                 */
                case "CLOSE" -> {
                    int fd = (Integer) stack.pop();
                    FDTable.close(fd);
                    stack.push(0);
                }
                /*
                 * READ —— 从 fd 读取指定字节数。
                 * 入栈顺序: fd（int）, count（int，字节数）
                 * 出栈顺序: count → fd
                 * 返回：byte[]（实际读取的数据，EOF 时长度为 0）
                 */
                case "READ" -> {
                    int count = (Integer) stack.pop();
                    int fd = (Integer) stack.pop();
                    Channel ch = FDTable.get(fd);
                    if (!(ch instanceof ReadableByteChannel rch)) {
                        stack.push(new byte[0]);
                        break;
                    }
                    ByteBuffer buf = ByteBuffer.allocate(count);
                    int n = rch.read(buf);
                    if (n < 0) n = 0;
                    buf.flip();
                    byte[] out = new byte[n];
                    buf.get(out);
                    stack.push(out);
                }
                /*
                 * WRITE —— 向 fd 写数据。
                 * 入栈顺序: fd（int）, data（byte[] 或 String）
                 * 出栈顺序: data → fd
                 * 返回写入字节数，失败 -1
                 */
                case "WRITE" -> {
                    Object dataObj = stack.pop();
                    int fd = (Integer) stack.pop();
                    byte[] data = (dataObj instanceof byte[] b)
                            ? b
                            : String.valueOf(dataObj).getBytes();
                    Channel ch = FDTable.get(fd);
                    if (!(ch instanceof WritableByteChannel wch)) {
                        stack.push(-1);
                        break;
                    }
                    int written = wch.write(ByteBuffer.wrap(data));
                    stack.push(written);
                }
                /*
                 * SEEK —— 文件定位，移动文件读写指针。
                 * 入栈顺序: fd（int）, offset（long/int）, whence（int, 0=SET,1=CUR,2=END）
                 * 出栈顺序: whence → offset → fd
                 * 返回新位置（long），失败 -1
                 */
                case "SEEK" -> {
                    int whence = (Integer) stack.pop();
                    long off = ((Number) stack.pop()).longValue();
                    int fd = (Integer) stack.pop();
                    Channel ch = FDTable.get(fd);
                    if (!(ch instanceof SeekableByteChannel sbc)) {
                        stack.push(-1);
                        break;
                    }
                    SeekableByteChannel newPos = switch (whence) {
                        case 0 -> sbc.position(off);
                        case 1 -> sbc.position(sbc.position() + off);
                        case 2 -> sbc.position(sbc.size() + off);
                        default -> throw new IllegalArgumentException("bad whence");
                    };
                    stack.push(newPos);
                }

                /*==================== 管道 / FD 相关 ====================*/

                /*
                 * PIPE —— 创建匿名管道。
                 * 入栈顺序: （无）
                 * 出栈顺序: （无）
                 * 返回顺序: write fd（先压栈），read fd（后压栈）
                 */
                case "PIPE" -> {
                    Pipe p = Pipe.open();
                    stack.push(FDTable.register(p.sink()));   // write fd
                    stack.push(FDTable.register(p.source())); // read fd
                }
                /*
                 * DUP —— 复制一个已存在的 fd（dup）。
                 * 入栈顺序: oldfd（int）
                 * 出栈顺序: oldfd
                 * 返回新的 fd，失败 -1
                 */
                case "DUP" -> {
                    int oldfd = (Integer) stack.pop();
                    stack.push(FDTable.dup(oldfd));
                }

                /*==================== 网络相关 ====================*/

                /*
                 * SOCKET —— 创建套接字，支持 stream/dgram。
                 * 入栈顺序: domain（int, AF_INET=2等）, type（int, 1=STREAM,2=DGRAM）, protocol（int, 预留）
                 * 出栈顺序: protocol → type → domain
                 * 返回 fd
                 */
                case "SOCKET" -> {
                    int proto = (Integer) stack.pop();      // 预留，暂不用
                    int type = (Integer) stack.pop();       // 1=STREAM,2=DGRAM
                    int domain = (Integer) stack.pop();     // AF_INET=2……
                    Channel ch = (type == 1)
                            ? SocketChannel.open()
                            : DatagramChannel.open();
                    stack.push(FDTable.register(ch));
                }
                /*
                 * CONNECT —— 发起 TCP 连接。
                 * 入栈顺序: fd（int）, host（String）, port（int）
                 * 出栈顺序: port → host → fd
                 * 返回 0 成功，-1 失败
                 */
                case "CONNECT" -> {
                    int port = (Integer) stack.pop();
                    String host = String.valueOf(stack.pop());
                    int fd = (Integer) stack.pop();
                    Channel ch = FDTable.get(fd);
                    if (ch instanceof SocketChannel sc) {
                        sc.connect(new InetSocketAddress(host, port));
                        stack.push(0);
                    } else stack.push(-1);
                }
                /*
                 * BIND —— 绑定端口。
                 * 入栈顺序: fd（int）, host（String）, port（int）
                 * 出栈顺序: port → host → fd
                 * 返回 0 成功，-1 失败
                 */
                case "BIND" -> {
                    int port = (Integer) stack.pop();
                    String host = String.valueOf(stack.pop());
                    int fd = (Integer) stack.pop();
                    Channel ch = FDTable.get(fd);
                    if (ch instanceof ServerSocketChannel ssc) {
                        ssc.bind(new InetSocketAddress(host, port));
                        stack.push(0);
                    } else stack.push(-1);
                }
                /*
                 * LISTEN —— 监听 socket，兼容 backlog。
                 * 入栈顺序: fd（int）, backlog（int）
                 * 出栈顺序: backlog → fd
                 * 返回 0 成功，-1 失败
                 * <b>注意：Java NIO 打开 ServerSocketChannel 已自动监听，无 backlog 处理，行为和 UNIX 有区别。</b>
                 */
                case "LISTEN" -> {
                    int backlog = (Integer) stack.pop();
                    int fd = (Integer) stack.pop();
                    Channel ch = FDTable.get(fd);
                    if (ch instanceof ServerSocketChannel) stack.push(0);
                    else stack.push(-1);
                }
                /*
                 * ACCEPT —— 接收连接。
                 * 入栈顺序: fd（int）
                 * 出栈顺序: fd
                 * 返回新连接 fd，失败 -1
                 */
                case "ACCEPT" -> {
                    int fd = (Integer) stack.pop();
                    Channel ch = FDTable.get(fd);
                    if (ch instanceof ServerSocketChannel ssc) {
                        SocketChannel cli = ssc.accept();
                        stack.push(FDTable.register(cli));
                    } else stack.push(-1);
                }

                /*==================== 多路复用 ====================*/

                /*
                 * SELECT —— I/O 多路复用，监视多个 fd 是否可读写。
                 * 入栈顺序: fds（List<Integer>）, timeout_ms（long）
                 * 出栈顺序: timeout_ms → fds
                 * 返回: 就绪 fd 列表（List<Integer>）
                 */
                case "SELECT" -> {
                    long timeout = ((Number) stack.pop()).longValue();
                    @SuppressWarnings("unchecked")
                    List<Integer> fds = (List<Integer>) stack.pop();

                    Selector sel = Selector.open();
                    for (int fd : fds) {
                        Channel c = FDTable.get(fd);
                        if (c instanceof SelectableChannel sc) {
                            sc.configureBlocking(false);
                            int ops = (c instanceof ReadableByteChannel ? SelectionKey.OP_READ : 0)
                                    | (c instanceof WritableByteChannel ? SelectionKey.OP_WRITE : 0);
                            sc.register(sel, ops, fd);
                        }
                    }
                    int ready = sel.select(timeout);
                    List<Integer> readyFds = new ArrayList<>();
                    if (ready > 0) {
                        for (SelectionKey k : sel.selectedKeys())
                            readyFds.add((Integer) k.attachment());
                    }
                    stack.push(readyFds);
                    sel.close();
                }


                /*==================== 控制台输出 ====================*/

                /*
                 * PRINT —— 控制台输出（无换行）。
                 * 入栈顺序: data（String 或 byte[]）
                 * 出栈顺序: data
                 * 返回 0
                 */
                case "PRINT" -> {
                    Object dataObj = stack.pop();
                    if (dataObj instanceof byte[] b) {
                        System.out.print(new String(b));
                    } else {
                        System.out.print(dataObj);
                    }
                    stack.push(0); // 表示成功
                }
                /*
                 * PRINTLN —— 控制台输出（自动换行）。
                 * 入栈顺序: data（String 或 byte[]）
                 * 出栈顺序: data
                 * 返回 0
                 */
                case "PRINTLN" -> {
                    Object dataObj = stack.pop();
                    if (dataObj instanceof byte[] b) {
                        System.out.println(new String(b));
                    } else {
                        System.out.println(dataObj);
                    }
                    stack.push(0); // 表示成功
                }

                /*==================== 其它未实现/扩展命令 ====================*/

                /*
                 * 其它自定义 syscall 子命令未实现
                 */
                default -> throw new UnsupportedOperationException("Unsupported SYSCALL: " + cmd);
            }
        } catch (Exception e) {
            // 统一异常处理，异常时压入 -1
            pushErr(stack, e);
        }

        // 默认：下一条指令
        return pc + 1;
    }
}
