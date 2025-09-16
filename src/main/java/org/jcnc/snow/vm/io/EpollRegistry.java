package org.jcnc.snow.vm.io;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code EpollRegistry} 管理所有虚拟机中的 epoll 实例，
 * 为每个 epoll 分配唯一的 epfd。
 */
public final class EpollRegistry {

    private static final AtomicInteger COUNTER = new AtomicInteger(100); // epfd 起始值
    private static final Map<Integer, EpollInstance> REGISTRY = new ConcurrentHashMap<>();

    private EpollRegistry() {
    }

    /**
     * 创建一个新的 epoll 实例并返回其 epfd。
     *
     * @param flags 创建参数（当前未使用，预留）
     * @return 新的 epfd
     */
    public static int create(int flags) {
        int epfd = COUNTER.incrementAndGet();
        REGISTRY.put(epfd, new EpollInstance(flags));
        return epfd;
    }

    /**
     * 根据 epfd 获取对应的 epoll 实例。
     *
     * @param epfd epoll 文件描述符
     * @return 对应的 epoll 实例
     * @throws IllegalArgumentException 如果 epfd 无效
     */
    public static EpollInstance get(int epfd) {
        EpollInstance inst = REGISTRY.get(epfd);
        if (inst == null) {
            throw new IllegalArgumentException("Invalid epfd: " + epfd);
        }
        return inst;
    }

    /**
     * 关闭并移除 epoll 实例。
     *
     * @param epfd epoll 文件描述符
     */
    public static void close(int epfd) {
        REGISTRY.remove(epfd);
    }
}
