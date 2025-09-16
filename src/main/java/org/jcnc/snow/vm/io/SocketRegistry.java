package org.jcnc.snow.vm.io;

import java.io.IOException;
import java.nio.channels.Channel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 管理 VM 中的 socket fd ↔ Channel 映射。
 */
public class SocketRegistry {

    // 从 3 开始分配 fd (0/1/2 保留给标准输入输出)
    private static final AtomicInteger NEXT_FD = new AtomicInteger(3);

    // fd → Channel 映射表
    private static final ConcurrentHashMap<Integer, Channel> registry = new ConcurrentHashMap<>();

    /**
     * 注册一个新 Channel，返回分配的 fd
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
     * 根据 fd 获取 Channel
     */
    public static Channel get(int fd) {
        return registry.get(fd);
    }

    /**
     * 移除并关闭 Channel
     */
    public static void close(int fd) throws IOException {
        Channel channel = registry.remove(fd);
        if (channel != null) {
            channel.close();
        }
    }

    /**
     * 判断 fd 是否存在
     */
    public static boolean exists(int fd) {
        return registry.containsKey(fd);
    }
}
