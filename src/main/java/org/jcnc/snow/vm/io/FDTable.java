package org.jcnc.snow.vm.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code FDTable} 维护 Snow VM 全局文件描述符（fd）与通道（Channel）的映射关系。
 * <p>
 * 此表为进程级（process-wide），支持所有标准 I/O 及动态分配的 fd。
 * <ul>
 *   <li>fd 0: 标准输入（stdin），类型为 {@code ReadableByteChannel}</li>
 *   <li>fd 1: 标准输出（stdout），类型为 {@code WritableByteChannel}</li>
 *   <li>fd 2: 标准错误（stderr），类型为 {@code WritableByteChannel}</li>
 *   <li>fd ≥ 3: 动态分配，映射各种 I/O 资源</li>
 * </ul>
 * <p>
 * 所有非标准 fd 的分配、查找、关闭、复制均通过本类静态方法实现，线程安全。
 */
public final class FDTable {

    /**
     * 全局 fd → Channel 映射表
     */
    private static final ConcurrentHashMap<Integer, Channel> MAP = new ConcurrentHashMap<>();
    /**
     * fd 分配自增计数器，初始为 3（避开标准流）
     */
    private static final AtomicInteger NEXT_FD = new AtomicInteger(3);

    private FDTable() {
        // 禁止实例化
    }

    static {
        // 绑定标准流
        MAP.put(0, Channels.newChannel(new BufferedInputStream(System.in)));
        MAP.put(1, Channels.newChannel(new BufferedOutputStream(System.out)));
        MAP.put(2, Channels.newChannel(new BufferedOutputStream(System.err)));
    }

    /**
     * 注册一个通道，分配并返回新的 fd（≥3）。
     *
     * @param ch 要注册的通道
     * @return 分配的 fd
     */
    public static int register(Channel ch) {
        int fd = NEXT_FD.getAndIncrement();
        MAP.put(fd, ch);
        return fd;
    }

    /**
     * 通过 fd 获取对应的通道。
     *
     * @param fd 文件描述符
     * @return 映射的通道对象，若不存在则为 {@code null}
     */
    public static Channel get(int fd) {
        return MAP.get(fd);
    }

    /**
     * 关闭并移除指定 fd（0, 1, 2 不会被关闭）。
     * <p>
     * 若 fd 存在，执行 {@code Channel.close()} 并移除。
     *
     * @param fd 需关闭的文件描述符
     * @throws IOException 关闭通道时发生的 I/O 异常
     */
    public static void close(int fd) throws IOException {
        if (fd <= 2) return; // 标准流不允许关闭
        Channel ch = MAP.remove(fd);
        if (ch != null) {
            ch.close();
        }
    }

    /**
     * 复制一个 fd，分配新的 fd 并映射到同一个通道。
     * <p>
     * 注意：不做引用计数，关闭任意 fd 会立即关闭底层通道。
     *
     * @param oldfd 被复制的 fd
     * @return 新分配的 fd
     * @throws IllegalArgumentException 若 oldfd 不存在
     */
    public static int dup(int oldfd) {
        Channel ch = get(oldfd);
        if (ch == null) {
            throw new IllegalArgumentException("dup: invalid fd " + oldfd);
        }
        return register(ch);
    }

    /**
     * 复制 fd 至指定 newfd。如果 newfd 已存在，则先关闭原通道（0–2 不会被关闭）。
     * <p>
     * 调用后，newfd 映射到 oldfd 的通道；保证分配器不会分配低于 newfd+1 的 fd。
     *
     * @param oldfd 源 fd
     * @param newfd 目标 fd
     * @return 复制结果 fd（等于 newfd）
     * @throws IOException              关闭原 newfd 时抛出的 I/O 异常
     * @throws IllegalArgumentException 参数非法或 oldfd 不存在
     */
    public static int dup2(int oldfd, int newfd) throws IOException {
        if (newfd < 0) throw new IllegalArgumentException("dup2: newfd must be non-negative");
        Channel ch = get(oldfd);
        if (ch == null) {
            throw new IllegalArgumentException("dup2: invalid oldfd " + oldfd);
        }
        if (oldfd == newfd) {
            return newfd;
        }
        // 若 newfd 存在且不为标准流，则先关闭
        if (newfd > 2) {
            Channel removed = MAP.remove(newfd);
            if (removed != null) {
                removed.close();
            }
        }
        // 绑定 oldfd 的通道到 newfd
        MAP.put(newfd, ch);
        // 更新分配器，避免新分配低于 newfd+1 的 fd
        NEXT_FD.updateAndGet(n -> Math.max(n, newfd + 1));
        return newfd;
    }
}
