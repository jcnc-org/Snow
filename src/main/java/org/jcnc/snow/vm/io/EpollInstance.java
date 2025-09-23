package org.jcnc.snow.vm.io;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code EpollInstance} 表示一个 epoll 实例，基于 Java NIO Selector 实现。
 *
 * <p><b>事件位掩码约定</b>：
 * <ul>
 *   <li>1 = READ（OP_READ / OP_ACCEPT）</li>
 *   <li>2 = WRITE（OP_WRITE）</li>
 *   <li>4 = CONNECT（OP_CONNECT）</li>
 * </ul>
 * </p>
 */
public class EpollInstance {

    private final int flags;
    private final Selector selector;

    // fd -> channel
    private final Map<Integer, SelectableChannel> fdToChannel = new ConcurrentHashMap<>();
    // channel -> fd
    private final Map<SelectableChannel, Integer> channelToFd = new ConcurrentHashMap<>();

    public EpollInstance(int flags) throws IOException {
        this.flags = flags;
        this.selector = Selector.open();
    }

    public int getFlags() {
        return flags;
    }

    public Selector getSelector() {
        return selector;
    }

    /**
     * 根据 channel 反查 fd；找不到则返回 null。
     */
    public Integer fdOf(SelectableChannel ch) {
        return channelToFd.get(ch);
    }

    /**
     * 判断某个 fd 是否已被注册到该 epoll 实例。
     */
    public boolean containsFd(int fd) {
        return fdToChannel.containsKey(fd);
    }

    /**
     * 注册或更新一个 fd 的事件掩码。
     *
     * @param fd     文件描述符
     * @param events 事件掩码（见上方约定）
     */
    public void addOrUpdate(int fd, int events) throws IOException {
        Channel ch = FDTable.get(fd);
        if (!(ch instanceof SelectableChannel sc)) {
            throw new IllegalArgumentException("fd " + fd + " 不是可选择通道（SelectableChannel）");
        }
        sc.configureBlocking(false);

        int ops = toSelectionOps(events);

        SelectionKey key = sc.keyFor(selector);
        if (key == null) {
            sc.register(selector, ops);
        } else {
            key.interestOps(ops);
        }

        fdToChannel.put(fd, sc);
        channelToFd.put(sc, fd);
    }

    /**
     * 移除一个 fd 的监控。
     *
     * @param fd 文件描述符
     */
    public void remove(int fd) {
        SelectableChannel sc = fdToChannel.remove(fd);
        if (sc != null) {
            channelToFd.remove(sc);
            SelectionKey key = sc.keyFor(selector);
            if (key != null) {
                key.cancel();
            }
        }
    }

    /**
     * 将自定义事件位映射为 NIO SelectionKey 的兴趣集。
     */
    private int toSelectionOps(int events) {
        int ops = 0;
        if ((events & 1) != 0) ops |= SelectionKey.OP_READ | SelectionKey.OP_ACCEPT;
        if ((events & 2) != 0) ops |= SelectionKey.OP_WRITE;
        if ((events & 4) != 0) ops |= SelectionKey.OP_CONNECT;
        return ops;
    }
}
