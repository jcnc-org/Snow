package org.jcnc.snow.vm.io;

import java.io.IOException;
import java.nio.channels.Channel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code SocketRegistry} 管理 VM 中所有 socket fd（文件描述符）到 {@link Channel} 的映射。
 * <br>提供 fd 分配、查找、关闭、替换等核心功能。所有操作线程安全。
 *
 * <p><b>功能：</b>
 * <ul>
 *   <li>从 3 开始自动分配唯一 fd（0/1/2 为标准输入/输出/错误保留）</li>
 *   <li>注册、获取、关闭、替换 Channel</li>
 *   <li>判断 fd 是否存在</li>
 * </ul>
 * </p>
 *
 * <p><b>实现说明：</b>底层用 {@link ConcurrentHashMap}，线程安全。</p>
 */
public class SocketRegistry {

    /**
     * fd 计数器，从 3 开始（0/1/2 为标准 IO 保留）
     */
    private static final AtomicInteger NEXT_FD = new AtomicInteger(3);

    /**
     * fd → Channel 注册表
     */
    private static final ConcurrentHashMap<Integer, Channel> registry = new ConcurrentHashMap<>();

    /**
     * 注册一个新 Channel，分配唯一 fd。
     *
     * @param channel 需注册的 Channel（不可为 null）
     * @return 分配的 fd（int，始于 3）
     * @throws IllegalArgumentException channel 为空时抛出
     */
    public static int register(Channel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("Channel cannot be null");
        }
        int fd = NEXT_FD.getAndIncrement();
        registry.put(fd, channel);
        return fd;
    }

    /**
     * 获取指定 fd 对应的 Channel。
     *
     * @param fd 文件描述符
     * @return Channel（找不到时返回 null）
     */
    public static Channel get(int fd) {
        return registry.get(fd);
    }

    /**
     * 关闭并移除 fd 对应的 Channel。
     *
     * @param fd 文件描述符
     * @throws IOException 关闭底层 Channel 失败时抛出
     */
    public static void close(int fd) throws IOException {
        Channel channel = registry.remove(fd);
        if (channel != null) {
            channel.close();
        }
    }

    /**
     * 检查 fd 是否已注册。
     *
     * @param fd 文件描述符
     * @return 是否存在
     */
    public static boolean exists(int fd) {
        return registry.containsKey(fd);
    }

    /**
     * 用新 Channel 替换 fd 对应的 Channel，自动关闭旧 Channel（如有）。
     *
     * @param fd         文件描述符
     * @param newChannel 新 Channel
     * @throws IOException 关闭旧 Channel 失败时抛出
     */
    public static void replace(int fd, Channel newChannel) throws IOException {
        Channel old = registry.put(fd, newChannel);
        if (old != null && old.isOpen()) {
            try {
                old.close();
            } catch (IOException ignore) {
            }
        }
    }
}
