package org.jcnc.snow.vm.io;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code EpollInstance} 表示一个 epoll 实例，
 * 用于保存被监控的 fd 及其对应的事件掩码。
 *
 * <p>目前这是一个简化实现，仅存储 fd -> events 的映射。</p>
 */
public class EpollInstance {

    /** epoll 创建时的 flags（当前实现未使用，仅保留） */
    private final int flags;

    /** fd -> 事件掩码 */
    private final Map<Integer, Integer> watchMap = new ConcurrentHashMap<>();

    public EpollInstance(int flags) {
        this.flags = flags;
    }

    public int getFlags() {
        return flags;
    }

    /**
     * 注册或更新一个 fd 的事件。
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
     * 获取当前监控的 fd -> events 映射。
     *
     * @return map
     */
    public Map<Integer, Integer> getWatchMap() {
        return watchMap;
    }
}
