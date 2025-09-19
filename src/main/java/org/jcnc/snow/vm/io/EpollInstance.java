package org.jcnc.snow.vm.io;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code EpollInstance} 表示一个 epoll 实例，用于保存被监控的 fd 及其对应的事件掩码。
 *
 * <p><b>功能</b>：提供 fd 到事件掩码的线程安全映射，支持注册、更新和移除 fd 监控事件。
 *
 * <p><b>当前实现</b>：仅存储 fd → events 的映射，未实现真正的事件分发与通知，作为 epoll 机制的
 * 简化数据结构。</p>
 *
 * <p><b>字段说明</b>：
 * <ul>
 *   <li>{@link #flags}        epoll 创建时的 flags 参数（当前未用，仅保留）</li>
 *   <li>{@link #watchMap}     fd 到事件掩码的映射表（线程安全）</li>
 * </ul>
 * </p>
 */
public class EpollInstance {

    /**
     * epoll 创建时的 flags（当前实现未使用，仅保留）
     */
    private final int flags;

    /**
     * fd -> 事件掩码（线程安全）
     */
    private final Map<Integer, Integer> watchMap = new ConcurrentHashMap<>();

    /**
     * 创建 epoll 实例。
     *
     * @param flags 创建 epoll 时传入的 flags（暂未使用）
     */
    public EpollInstance(int flags) {
        this.flags = flags;
    }

    /**
     * 获取 epoll 创建时的 flags。
     *
     * @return flags
     */
    public int getFlags() {
        return flags;
    }

    /**
     * 注册或更新一个 fd 的事件掩码。
     *
     * @param fd     文件描述符
     * @param events 事件掩码
     */
    public void addOrUpdate(int fd, int events) {
        watchMap.put(fd, events);
    }

    /**
     * 移除一个 fd 的监控。
     *
     * @param fd 文件描述符
     */
    public void remove(int fd) {
        watchMap.remove(fd);
    }

    /**
     * 获取当前所有监控的 fd → 事件掩码的映射。
     *
     * @return 线程安全 map
     */
    public Map<Integer, Integer> getWatchMap() {
        return watchMap;
    }
}
