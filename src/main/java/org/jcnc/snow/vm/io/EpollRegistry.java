package org.jcnc.snow.vm.io;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code EpollRegistry} 管理虚拟机中所有 epoll 实例，
 * 负责分配唯一 epfd 并实现 epoll 实例的注册、查找与释放。
 *
 * <p><b>功能</b>：
 * <ul>
 *   <li>为每个新 epoll 分配唯一的 epfd（从 100 开始自增）</li>
 *   <li>支持创建、获取和关闭 epoll 实例</li>
 * </ul>
 * </p>
 *
 * <p><b>实现细节</b>：
 * <ul>
 *   <li>内部使用线程安全的 {@link ConcurrentHashMap} 存储所有 epoll 实例</li>
 *   <li>禁止实例化，仅提供静态方法</li>
 * </ul>
 * </p>
 */
public final class EpollRegistry {

    /**
     * epfd 递增计数器（从 100 起始）
     */
    private static final AtomicInteger COUNTER = new AtomicInteger(100);

    /**
     * epfd -> EpollInstance 的全局注册表
     */
    private static final Map<Integer, EpollInstance> REGISTRY = new ConcurrentHashMap<>();

    private EpollRegistry() {
        // 禁止实例化
    }

    /**
     * 创建一个新的 epoll 实例并返回其 epfd。
     *
     * @param flags 创建参数（当前未使用，预留）
     * @return 新的 epfd
     */
    public static int create(int flags) throws IOException {
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
