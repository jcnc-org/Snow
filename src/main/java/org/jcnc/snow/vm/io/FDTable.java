package org.jcnc.snow.vm.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code FDTable} 维护 Snow VM 全局文件描述符（fd）与通道（Channel）的映射。
 *
 * <p>
 * 本表为进程全局，支持标准输入输出（0/1/2）及所有动态分配 fd。
 * 所有非标准 fd 的分配、查找、关闭、复制等操作均为线程安全。
 * </p>
 *
 * <ul>
 *   <li>fd 0: 标准输入（{@code ReadableByteChannel}）</li>
 *   <li>fd 1: 标准输出（{@code WritableByteChannel}）</li>
 *   <li>fd 2: 标准错误（{@code WritableByteChannel}）</li>
 *   <li>fd ≥ 3: 动态分配，各种 I/O 资源</li>
 * </ul>
 */
public final class FDTable {

    /**
     * 全局 fd → Channel 映射表
     */
    private static final ConcurrentHashMap<Integer, Channel> MAP = new ConcurrentHashMap<>();
    /**
     * fd → 源路径元信息
     */
    private static final ConcurrentHashMap<Integer, Path> PATHS = new ConcurrentHashMap<>();
    /**
     * fd 分配器，从 3 开始递增
     */
    private static final AtomicInteger NEXT_FD = new AtomicInteger(3);

    static {
        MAP.put(0, Channels.newChannel(new BufferedInputStream(System.in)));
        MAP.put(1, Channels.newChannel(new BufferedOutputStream(System.out)));
        MAP.put(2, Channels.newChannel(new BufferedOutputStream(System.err)));
    }

    private FDTable() {
    }

    /**
     * 注册一个通道，返回分配的 fd（不记录路径）。
     *
     * @param ch 要注册的 Channel
     * @return 分配的新 fd
     * @throws NullPointerException ch 为 null
     */
    public static int register(Channel ch) {
        Objects.requireNonNull(ch, "channel");
        int fd = NEXT_FD.getAndIncrement();
        MAP.put(fd, ch);
        PATHS.remove(fd);
        return fd;
    }

    /**
     * 注册一个通道及其来源路径，返回分配的 fd。
     *
     * @param ch   通道
     * @param path 来源路径，可为 null
     * @return 新 fd
     */
    public static int register(Channel ch, Path path) {
        int fd = register(ch);
        if (path != null) {
            PATHS.put(fd, path.toAbsolutePath().normalize());
        }
        return fd;
    }

    /**
     * 通过 fd 获取通道。
     *
     * @param fd 文件描述符
     * @return Channel 对象，若不存在则为 null
     */
    public static Channel get(int fd) {
        return MAP.get(fd);
    }

    /**
     * 通过 fd 获取其来源路径。
     *
     * @param fd 文件描述符
     * @return Path，可能为 null
     */
    public static Path getPath(int fd) {
        return PATHS.get(fd);
    }

    /**
     * 关闭并移除 fd。对 0/1/2 只执行 flush，不关闭。
     *
     * @param fd 文件描述符
     * @throws IOException 关闭失败
     */
    public static void close(int fd) throws IOException {
        if (fd < 0) {
            throw new IllegalArgumentException("invalid fd: " + fd);
        }
        if (fd <= 2) {
            // 刷新标准输出/错误
            if (fd == 1) {
                System.out.flush();
            } else if (fd == 2) {
                System.err.flush();
            }
            return;
        }
        Channel ch = MAP.remove(fd);
        PATHS.remove(fd);
        if (ch != null) {
            ch.close();
        }
    }

    /**
     * 复制 fd，分配新 fd 指向同一通道。
     *
     * @param oldfd 原始 fd
     * @return 新 fd
     * @throws IllegalArgumentException oldfd 不存在
     */
    public static int dup(int oldfd) {
        Channel ch = MAP.get(oldfd);
        if (ch == null) {
            throw new IllegalArgumentException("dup: invalid fd " + oldfd);
        }
        int newfd = NEXT_FD.getAndIncrement();
        MAP.put(newfd, ch);
        Path p = PATHS.get(oldfd);
        if (p != null) {
            PATHS.put(newfd, p);
        }
        return newfd;
    }

    /**
     * 复制 fd 到指定 fd（如 newfd 已存在则先关闭）。
     *
     * @param oldfd 原始 fd
     * @param newfd 指定的新 fd
     * @return newfd
     * @throws IllegalArgumentException oldfd/newfd 非法
     * @throws IOException              关闭旧 fd 失败
     */
    public static int dup2(int oldfd, int newfd) throws IOException {
        if (newfd < 0) {
            throw new IllegalArgumentException("dup2: invalid newfd " + newfd);
        }
        Channel ch = MAP.get(oldfd);
        if (ch == null) {
            throw new IllegalArgumentException("dup2: invalid oldfd " + oldfd);
        }
        if (oldfd == newfd) {
            return newfd;
        }
        if (newfd > 2) {
            Channel removed = MAP.remove(newfd);
            PATHS.remove(newfd);
            if (removed != null) {
                removed.close();
            }
        }
        MAP.put(newfd, ch);
        Path p = PATHS.get(oldfd);
        if (p != null) {
            PATHS.put(newfd, p);
        } else {
            PATHS.remove(newfd);
        }
        NEXT_FD.updateAndGet(n -> Math.max(n, newfd + 1));
        return newfd;
    }
}
