package org.jcnc.snow.vm.commands.system.control.multiplex;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.channels.*;
import java.util.*;

/**
 * {@code SelectHandler} 实现 SELECT (0x1300) 系统调用，
 * 基于 Java NIO {@link Selector} 实现多路复用，等待一组文件描述符 (fd) 的 I/O 就绪事件。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (readSet: List<int>, writeSet: List<int>, exceptSet: List<int>, timeout_ms:int)} →
 * 出参 {@code (ready: Map{ "read":List<int>, "write":List<int>, "except":List<int> })}
 * </p>
 *
 * <p><b>语义：</b>
 * 监视三类 fd 集合（读 / 写 / 异常），阻塞或超时等待就绪事件，返回对应的就绪 fd 列表。
 * </p>
 *
 * <p><b>事件集合映射：</b>
 * <ul>
 *   <li>{@code readSet} → {@link SelectionKey#OP_READ} 或服务器端 {@link SelectionKey#OP_ACCEPT}</li>
 *   <li>{@code writeSet} → {@link SelectionKey#OP_WRITE}</li>
 *   <li>{@code exceptSet} → {@link SelectionKey#OP_CONNECT}</li>
 * </ul>
 * </p>
 *
 * <p><b>返回：</b>
 * 成功时返回一个 {@code Map}，包含三个 key：
 * <ul>
 *   <li>{@code "read"} = 可读/可接受连接的 fd 列表</li>
 *   <li>{@code "write"} = 可写的 fd 列表</li>
 *   <li>{@code "except"} = 连接就绪的 fd 列表</li>
 * </ul>
 * 若无事件触发，则所有列表为空。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>参数类型错误时抛出 {@link IllegalArgumentException}</li>
 *   <li>非 {@link SelectableChannel} 的 fd 将被忽略，不抛异常</li>
 *   <li>I/O 操作中可能抛出 {@link java.io.IOException}</li>
 * </ul>
 * </p>
 */
public class SelectHandler implements SyscallHandler {

    private static List<Integer> toIntList(Object obj) {
        if (obj == null) return Collections.emptyList();
        if (obj instanceof List<?> list) {
            List<Integer> out = new ArrayList<>(list.size());
            for (Object o : list) {
                if (o == null) continue;
                if (!(o instanceof Number n)) {
                    throw new IllegalArgumentException("SELECT: fd list must contain integers");
                }
                out.add(n.intValue());
            }
            return out;
        }
        throw new IllegalArgumentException("SELECT: expected an array/list");
    }

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 按 LIFO 次序弹出参数：timeout_ms, exceptSet, writeSet, readSet
        Object timeoutObj = stack.pop();
        Object exceptObj = stack.pop();
        Object writeObj = stack.pop();
        Object readObj = stack.pop();

        final int timeoutMs = (timeoutObj == null) ? -1 : ((Number) timeoutObj).intValue();
        final List<Integer> readSet = toIntList(readObj);
        final List<Integer> writeSet = toIntList(writeObj);
        final List<Integer> exceptSet = toIntList(exceptObj);

        // 为每个 fd 聚合 interestOps
        final Map<Integer, Integer> fdOps = new HashMap<>();
        for (Integer fd : readSet) {
            if (fd == null) continue;
            fdOps.merge(fd, SelectionKey.OP_READ, (a, b) -> a | b);
        }
        for (Integer fd : writeSet) {
            if (fd == null) continue;
            fdOps.merge(fd, SelectionKey.OP_WRITE, (a, b) -> a | b);
        }
        for (Integer fd : exceptSet) {
            if (fd == null) continue;
            fdOps.merge(fd, SelectionKey.OP_CONNECT, (a, b) -> a | b);
        }

        final List<Integer> readyRead = new ArrayList<>();
        final List<Integer> readyWrite = new ArrayList<>();
        final List<Integer> readyExcept = new ArrayList<>();

        if (fdOps.isEmpty()) {
            // 没有可监控对象，直接返回空结果
            Map<String, Object> ready = new HashMap<>();
            ready.put("read", readyRead);
            ready.put("write", readyWrite);
            ready.put("except", readyExcept);
            stack.push(ready);
            return;
        }

        Selector selector = Selector.open();
        try {
            final Map<SelectableChannel, Integer> chToFd = new HashMap<>();
            final int validOps = SelectionKey.OP_READ
                    | SelectionKey.OP_WRITE
                    | SelectionKey.OP_CONNECT
                    | SelectionKey.OP_ACCEPT;

            for (Map.Entry<Integer, Integer> e : fdOps.entrySet()) {
                final int fd = e.getKey();
                final int ops = e.getValue();

                Channel ch = FDTable.get(fd);
                if (!(ch instanceof SelectableChannel sc)) {
                    // 不可选择的通道跳过
                    continue;
                }

                // 服务器端监听套接字：读兴趣映射为 ACCEPT
                int interestOps = ops;
                if ((ops & SelectionKey.OP_READ) != 0 && sc instanceof ServerSocketChannel) {
                    interestOps = (interestOps & ~SelectionKey.OP_READ) | SelectionKey.OP_ACCEPT;
                }

                // 确保 interestOps 合法
                interestOps &= validOps;

                sc.configureBlocking(false);

                // 如果 channel 已经注册过，需要合并 interestOps
                SelectionKey key = sc.keyFor(selector);
                if (key == null) {
                    sc.register(selector, interestOps);
                } else {
                    key.interestOps(key.interestOps() | interestOps);
                }
                chToFd.put(sc, fd);
            }

            // 执行选择
            int selected;
            if (timeoutMs < 0) {
                selected = selector.select(); // 无限等待
            } else if (timeoutMs == 0) {
                selected = selector.selectNow(); // 立即返回
            } else {
                selected = selector.select(timeoutMs);
            }

            if (selected > 0) {
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                for (SelectionKey key : selectedKeys) {
                    if (!key.isValid()) continue;
                    final SelectableChannel sc = key.channel();
                    final Integer fd = chToFd.get(sc);
                    if (fd == null) continue;

                    if (key.isAcceptable() || key.isReadable()) {
                        readyRead.add(fd);
                    }
                    if (key.isWritable()) {
                        readyWrite.add(fd);
                    }
                    if (key.isConnectable()) {
                        readyExcept.add(fd);
                    }
                }
                selectedKeys.clear();
            }
        } finally {
            try {
                // 取消所有注册，关闭 selector
                for (SelectionKey k : selector.keys()) {
                    try {
                        k.cancel();
                    } catch (Throwable ignored) {
                    }
                }
            } catch (Throwable ignored) {
            }
            try {
                selector.close();
            } catch (Throwable ignored) {
            }
        }

        // 构造返回
        Map<String, Object> ready = new HashMap<>();
        ready.put("read", readyRead);
        ready.put("write", readyWrite);
        ready.put("except", readyExcept);
        stack.push(ready);
    }
}
