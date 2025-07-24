package org.jcnc.snow.vm.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.channels.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 维护 “虚拟 fd → Java NIO Channel” 的全局映射表。
 *
 * <pre>
 *   0  → stdin   (ReadableByteChannel)
 *   1  → stdout  (WritableByteChannel)
 *   2  → stderr  (WritableByteChannel)
 *   3+ → 运行期动态分配
 * </pre>
 */
public final class FDTable {

    private FDTable() {}

    /** 下一次可用 fd（0‒2 保留给标准流） */
    private static final AtomicInteger NEXT_FD = new AtomicInteger(3);
    /** 主映射表：fd → Channel */
    private static final ConcurrentHashMap<Integer, Channel> MAP = new ConcurrentHashMap<>();

    static {
        // JVM 标准流包装成 NIO Channel 后放入表中
        MAP.put(0, Channels.newChannel(new BufferedInputStream(System.in)));
        MAP.put(1, Channels.newChannel(new BufferedOutputStream(System.out)));
        MAP.put(2, Channels.newChannel(new BufferedOutputStream(System.err)));
    }

    /** 注册新 Channel，返回分配到的虚拟 fd */
    public static int register(Channel ch) {
        int fd = NEXT_FD.getAndIncrement();
        MAP.put(fd, ch);
        return fd;
    }

    /** 取得 Channel；如果 fd 不存在则返回 null */
    public static Channel get(int fd) {
        return MAP.get(fd);
    }

    /** 关闭并移除 fd（0‒2 忽略） */
    public static void close(int fd) throws IOException {
        if (fd <= 2) return;                                   // 标准流交由宿主 JVM 维护
        Channel ch = MAP.remove(fd);
        if (ch != null && ch.isOpen()) ch.close();
    }

    /** 类似 dup(oldfd) —— 返回指向同一 Channel 的新 fd */
    public static int dup(int oldfd) {
        Channel ch = MAP.get(oldfd);
        if (ch == null)
            throw new IllegalArgumentException("Bad fd: " + oldfd);
        return register(ch);                                   // 多个 fd 引用同一 Channel
    }
}
