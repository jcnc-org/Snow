package org.jcnc.snow.vm.io;

import java.io.IOException;
import java.nio.channels.Channel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code SocketRegistry} 管理虚拟机中的 socket 文件描述符（fd）到 {@link Channel} 的映射，
 * 支持 socket fd 分配、查找、关闭等操作。
 *
 * <p><b>功能</b>：
 * <ul>
 *   <li>从 3 开始自动分配唯一 fd（0/1/2 保留给标准输入/输出/错误）</li>
 *   <li>支持注册 Channel、获取 Channel、关闭并移除 Channel</li>
 *   <li>可检查 fd 是否存在</li>
 * </ul>
 * </p>
 *
 * <p>内部使用线程安全的 {@link ConcurrentHashMap}。</p>
 */
public class SocketRegistry {

    /**
     * 从 3 开始分配 fd（0/1/2 保留给标准输入输出）
     */
    private static final AtomicInteger NEXT_FD = new AtomicInteger(3);

    /**
     * fd → Channel 的注册表
     */
    private static final ConcurrentHashMap<Integer, Channel> registry = new ConcurrentHashMap<>();

    /**
     * 注册一个新 Channel，返回分配的 fd。
     *
     * @param channel 待注册的 Channel
     * @return 分配的 fd
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
     * 根据 fd 获取 Channel。
     *
     * @param fd 文件描述符
     * @return Channel 对象（找不到返回 null）
     */
    public static Channel get(int fd) {
        return registry.get(fd);
    }

    /**
     * 移除并关闭 fd 对应的 Channel。
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
     * 判断 fd 是否存在于注册表中。
     *
     * @param fd 文件描述符
     * @return 是否存在
     */
    public static boolean exists(int fd) {
        return registry.containsKey(fd);
    }
}
