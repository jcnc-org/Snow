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
 * SyscallCommand —— 虚拟机系统调用（SYSCALL）指令实现。
 *
 * <p>
 * 本类负责将虚拟机指令集中的 SYSCALL 进行分派，模拟现实系统常见的文件、网络、管道、标准输出等操作，
 * 通过操作数栈完成参数、返回值传递，并借助文件描述符表（FDTable）进行底层资源管理。
 * 所有 I/O 相关功能均基于 Java NIO 实现，兼容多种 I/O 场景。
 * </p>
 *
 * <p>参数与栈约定:</p>
 * <ul>
 *   <li>所有调用参数，均按“右值先入、左值后入”顺序压入 {@link OperandStack}。</li>
 *   <li>SYSCALL 指令自动弹出参数并处理结果；返回值（如描述符、读取长度、是否成功等）压回栈顶。</li>
 * </ul>
 *
 * <p>异常与失败处理:</p>
 * <ul>
 *   <li>系统调用失败或遇到异常时，均向操作数栈压入 {@code -1}，以便调用者统一检测。</li>
 * </ul>
 *
 * <p>支持的子命令示例:</p>
 * <ul>
 *   <li>PRINT / PRINTLN —— 控制台输出</li>
 *   <li>OPEN / CLOSE / READ / WRITE / SEEK —— 文件相关操作</li>
 *   <li>PIPE / DUP —— 管道与文件描述符复制</li>
 *   <li>SOCKET / CONNECT / BIND / LISTEN / ACCEPT —— 网络通信</li>
 *   <li>SELECT —— 多通道 I/O 就绪检测</li>
 * </ul>
 */
public class SyscallCommand implements Command {

    /**
     * 分发并执行 SYSCALL 子命令，根据子命令类型从操作数栈取出参数、操作底层资源，并将结果压回栈顶。
     *
     * @param parts     指令及子命令参数分割数组，parts[1]为子命令名
     * @param pc        当前指令计数器
     * @param stack     操作数栈
     * @param locals    局部变量表
     * @param callStack 调用栈
     * @return 下一条指令的 pc 值（通常为 pc+1）
     * @throws IllegalArgumentException      缺少子命令参数时抛出
     * @throws UnsupportedOperationException 不支持的 SYSCALL 子命令时抛出
     */
    @Override
    public int execute(String[] parts, int pc,
                       OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) {

        if (parts.length < 2) {
            throw new IllegalArgumentException("SYSCALL missing subcommand");
        }

        String cmd = parts[1].toUpperCase(Locale.ROOT);

        try {
            switch (cmd) {
                // 文件相关操作
                case "OPEN" -> {
                    int mode = (Integer) stack.pop();
                    int flags = (Integer) stack.pop();
                    String path = String.valueOf(stack.pop());
                    FileChannel fc = FileChannel.open(Paths.get(path), flagsToOptions(flags));
                    stack.push(FDTable.register(fc));
                }
                case "CLOSE" -> {
                    int fd = (Integer) stack.pop();
                    FDTable.close(fd);
                    stack.push(0);
                }
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
                        default -> throw new IllegalArgumentException("Invalid offset type");
                    };
                    stack.push(newPos);
                }

                // 管道与描述符操作
                case "PIPE" -> {
                    Pipe p = Pipe.open();
                    stack.push(FDTable.register(p.sink()));
                    stack.push(FDTable.register(p.source()));
                }
                case "DUP" -> {
                    int oldfd = (Integer) stack.pop();
                    stack.push(FDTable.dup(oldfd));
                }

                // 网络相关
                case "SOCKET" -> {
                    int proto = (Integer) stack.pop();
                    int type = (Integer) stack.pop();
                    int domain = (Integer) stack.pop();
                    Channel ch = (type == 1)
                            ? SocketChannel.open()
                            : DatagramChannel.open();
                    stack.push(FDTable.register(ch));
                }
                case "CONNECT" -> {
                    int port = (Integer) stack.pop();
                    String host = String.valueOf(stack.pop());
                    int fd = (Integer) stack.pop();
                    Channel ch = FDTable.get(fd);
                    if (ch instanceof SocketChannel sc) {
                        sc.connect(new InetSocketAddress(host, port));
                        stack.push(0);
                    } else {
                        stack.push(-1);
                    }
                }
                case "BIND" -> {
                    int port = (Integer) stack.pop();
                    String host = String.valueOf(stack.pop());
                    int fd = (Integer) stack.pop();
                    Channel ch = FDTable.get(fd);
                    if (ch instanceof ServerSocketChannel ssc) {
                        ssc.bind(new InetSocketAddress(host, port));
                        stack.push(0);
                    } else {
                        stack.push(-1);
                    }
                }
                case "LISTEN" -> {
                    int backlog = (Integer) stack.pop();
                    int fd = (Integer) stack.pop();
                    Channel ch = FDTable.get(fd);
                    if (ch instanceof ServerSocketChannel) {
                        stack.push(0);
                    } else {
                        stack.push(-1);
                    }
                }
                case "ACCEPT" -> {
                    int fd = (Integer) stack.pop();
                    Channel ch = FDTable.get(fd);
                    if (ch instanceof ServerSocketChannel ssc) {
                        SocketChannel cli = ssc.accept();
                        stack.push(FDTable.register(cli));
                    } else {
                        stack.push(-1);
                    }
                }

                // 多路复用
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
                        for (SelectionKey k : sel.selectedKeys()) {
                            readyFds.add((Integer) k.attachment());
                        }
                    }
                    stack.push(readyFds);
                    sel.close();
                }

                // 控制台输出
                case "PRINT" -> {
                    Object dataObj = stack.pop();
                    output(dataObj, false);
                    stack.push(0);
                }
                case "PRINTLN" -> {
                    Object dataObj = stack.pop();
                    output(dataObj, true);
                    stack.push(0);
                }

                default -> throw new UnsupportedOperationException("Unsupported SYSCALL subcommand: " + cmd);
            }
        } catch (Exception e) {
            pushErr(stack, e);
        }

        return pc + 1;
    }

    /**
     * 根据传入的文件打开标志，构造 NIO {@link OpenOption} 集合。
     * <p>
     * 本方法负责将底层虚拟机传递的 flags 整数型位域，转换为 Java NIO 标准的文件打开选项集合，
     * 以支持文件读、写、创建、截断、追加等多种访问场景。
     * 常用于 SYSCALL 的 OPEN 子命令。
     * </p>
     *
     * @param flags 文件打开模式标志。各标志可组合使用，具体含义请参见虚拟机文档。
     * @return 转换后的 OpenOption 集合，可直接用于 FileChannel.open 等 NIO 方法
     */
    private static Set<OpenOption> flagsToOptions(int flags) {
        Set<OpenOption> opts = new HashSet<>();
        // 如果有写入标志，则添加WRITE，否则默认为READ。
        if ((flags & 0x1) != 0) opts.add(WRITE);
        else opts.add(READ);
        // 如果包含创建标志，允许创建文件。
        if ((flags & 0x40) != 0) opts.add(CREATE);
        // 包含截断标志，打开时清空内容。
        if ((flags & 0x200) != 0) opts.add(TRUNCATE_EXISTING);
        // 包含追加标志，文件写入时追加到末尾。
        if ((flags & 0x400) != 0) opts.add(APPEND);
        return opts;
    }

    /**
     * 捕获所有异常并统一处理，操作数栈压入 -1 代表本次系统调用失败。
     * <p>
     * 本方法是全局错误屏障，任何命令异常都会转换为虚拟机通用的失败信号，
     * 保证上层调用逻辑不会被异常打断。实际应用中可拓展错误码机制。
     * </p>
     *
     * @param stack 操作数栈，将失败信号写入此栈
     * @param e     抛出的异常对象，可在调试时输出日志
     */
    private static void pushErr(OperandStack stack, Exception e) {
        stack.push(-1);
        System.err.println("Syscall exception: " + e);
    }

    /**
     * 控制台输出通用方法，支持基本类型、字节数组、任意数组、对象等。
     * <p>
     * 该方法用于 SYSCALL PRINT/PRINTLN，将任意类型对象转为易读字符串输出到标准输出流。
     * 字节数组自动按 UTF-8 解码，其它原生数组按格式化字符串输出。
     * </p>
     *
     * @param obj     待输出的内容，可以为任何类型（如基本类型、byte[]、数组、对象等）
     * @param newline 是否自动换行。如果为 true，则在输出后换行；否则直接输出。
     */
    private static void output(Object obj, boolean newline) {
        String str;
        if (obj == null) {
            str = "null";
        } else if (obj instanceof byte[] bytes) {
            // 字节数组作为文本输出
            str = new String(bytes);
        } else if (obj.getClass().isArray()) {
            // 其它数组格式化输出
            str = arrayToString(obj);
        } else {
            str = obj.toString();
        }
        if (newline) System.out.println(str);
        else System.out.print(str);
    }

    /**
     * 将各种原生数组和对象数组转换为可读字符串，便于控制台输出和调试。
     * <p>
     * 本方法针对 int、long、double、float、short、char、byte、boolean 等所有原生数组类型
     * 以及对象数组都能正确格式化，统一输出格式风格，避免显示为类型 hashCode。
     * 若为不支持的类型，返回通用提示字符串。
     * </p>
     *
     * @param array 任意原生数组或对象数组
     * @return 该数组的可读字符串表示
     */
    private static String arrayToString(Object array) {
        if (array instanceof int[] a) return Arrays.toString(a);
        if (array instanceof long[] a) return Arrays.toString(a);
        if (array instanceof double[] a) return Arrays.toString(a);
        if (array instanceof float[] a) return Arrays.toString(a);
        if (array instanceof short[] a) return Arrays.toString(a);
        if (array instanceof char[] a) return Arrays.toString(a);
        if (array instanceof byte[] a) return Arrays.toString(a);
        if (array instanceof boolean[] a) return Arrays.toString(a);
        if (array instanceof Object[] a) return Arrays.deepToString(a);
        return "Unsupported array";
    }
}
