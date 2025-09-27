package org.jcnc.snow.vm.commands.system.control.multiplex;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.io.IOException;
import java.nio.channels.*;
import java.util.*;

/**
 * {@code SelectHandler} 实现 SELECT (0x1300) 系统调用，
 * 基于 Java NIO {@link Selector} 实现 I/O 多路复用，兼容标准流 fd=0/1/2。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (readSet: List<int>, writeSet: List<int>, exceptSet: List<int>, timeout_ms:int)} →
 * 出参 {@code (ready: Map{ "read":List<int>, "write":List<int>, "except":List<int> })}
 * </p>
 *
 * <p><b>语义：</b>
 * 等待指定文件描述符集合的 I/O 就绪事件，返回三类结果：可读、可写、异常。
 * <ul>
 *   <li>支持 {@link SelectableChannel}（SocketChannel、ServerSocketChannel、DatagramChannel 等）</li>
 *   <li>兼容标准流：
 *     <ul>
 *       <li>fd=0 (stdin)：支持 READ，采用 {@link System#in} 可用性轮询</li>
 *       <li>fd=1/2 (stdout/stderr)：支持 WRITE，视为始终可写</li>
 *     </ul>
 *   </li>
 *   <li>{@code readSet} → {@link SelectionKey#OP_READ} / {@link SelectionKey#OP_ACCEPT}</li>
 *   <li>{@code writeSet} → {@link SelectionKey#OP_WRITE}</li>
 *   <li>{@code exceptSet} → {@link SelectionKey#OP_CONNECT}</li>
 * </ul>
 * </p>
 *
 * <p><b>返回：</b>
 * 成功时返回一个 {@code Map}，包含 {@code "read"}、{@code "write"}、{@code "except"} 三个键，
 * 其值为就绪 fd 列表。若无事件触发，则返回的列表为空。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>参数类型非法时抛出 {@link IllegalArgumentException}</li>
 *   <li>底层 I/O 操作失败时抛出 {@link IOException}</li>
 *   <li>其他运行时错误时抛出 {@link RuntimeException}</li>
 * </ul>
 * </p>
 */
public class SelectHandler implements SyscallHandler {

    private static boolean waitStdinReadable(int timeoutMs)
            throws InterruptedException, IOException {
        if (timeoutMs == 0) return System.in.available() > 0;
        long deadline = (timeoutMs < 0) ? Long.MAX_VALUE
                : System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (System.in.available() > 0) return true;
            Thread.sleep(10);          // 10 ms 轮询
        }
        return false;
    }

    private static void pushResult(OperandStack stack,
                                   List<Integer> r, List<Integer> w, List<Integer> e) {
        Map<String, Object> ready = new HashMap<>();
        ready.put("read", r);
        ready.put("write", w);
        ready.put("except", e);
        stack.push(ready);
    }

    /**
     * 将任意 List<?> 转换为 List<Integer>
     */
    private static List<Integer> toIntList(Object obj) {
        if (obj == null) return Collections.emptyList();
        if (obj instanceof List<?> list) {
            List<Integer> out = new ArrayList<>(list.size());
            for (Object o : list)
                switch (o) {
                    case null -> {
                    }
                    case Number n -> out.add(n.intValue());
                    case Boolean b -> out.add(b ? 1 : 0);
                    default -> throw new IllegalArgumentException(
                            "SELECT: fd list must contain integers, got " + o.getClass());
                }
            return out;
        }
        throw new IllegalArgumentException("SELECT: fd list must be a List, got " + obj.getClass());
    }

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 参数解析
        Object timeoutObj = stack.pop();
        Object exceptObj = stack.pop();
        Object writeObj = stack.pop();
        Object readObj = stack.pop();

        final int timeoutMs = (timeoutObj == null) ? -1 : ((Number) timeoutObj).intValue();
        final List<Integer> readSet = toIntList(readObj);
        final List<Integer> writeSet = toIntList(writeObj);
        final List<Integer> exceptSet = toIntList(exceptObj);

        // interestOps 聚合
        final Map<Integer, Integer> fdOps = new HashMap<>();
        for (Integer fd : readSet) if (fd != null) fdOps.merge(fd, SelectionKey.OP_READ, Integer::sum);
        for (Integer fd : writeSet) if (fd != null) fdOps.merge(fd, SelectionKey.OP_WRITE, Integer::sum);
        for (Integer fd : exceptSet) if (fd != null) fdOps.merge(fd, SelectionKey.OP_CONNECT, Integer::sum);

        final List<Integer> readyRead = new ArrayList<>();
        final List<Integer> readyWrite = new ArrayList<>();
        final List<Integer> readyExcept = new ArrayList<>();

        // 1. 处理不可选择通道 (fd0/1/2)
        for (Iterator<Map.Entry<Integer, Integer>> it = fdOps.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Integer> e = it.next();
            int fd = e.getKey();
            int ops = e.getValue();
            Channel ch = FDTable.get(fd);

            if (ch == null || ch instanceof SelectableChannel) continue;

            if (fd == 0 && (ops & SelectionKey.OP_READ) != 0) {
                if (waitStdinReadable(timeoutMs)) readyRead.add(fd);
            }
            if ((fd == 1 || fd == 2) && (ops & SelectionKey.OP_WRITE) != 0) {
                readyWrite.add(fd);
            }
            it.remove();
        }

        if (fdOps.isEmpty()) {
            pushResult(stack, readyRead, readyWrite, readyExcept);
            return;
        }

        // 2. 剩余可选择通道交给 Selector
        Selector selector = Selector.open();
        try {
            final Map<SelectableChannel, Integer> ch2fd = new HashMap<>();
            final int validOps = SelectionKey.OP_READ | SelectionKey.OP_WRITE |
                    SelectionKey.OP_CONNECT | SelectionKey.OP_ACCEPT;

            for (Map.Entry<Integer, Integer> e : fdOps.entrySet()) {
                int fd = e.getKey();
                int ops = e.getValue();
                Channel ch = FDTable.get(fd);

                if (!(ch instanceof SelectableChannel sc)) continue;
                int interest = (ops & validOps);
                if ((interest & SelectionKey.OP_READ) != 0 && sc instanceof ServerSocketChannel)
                    interest = (interest & ~SelectionKey.OP_READ) | SelectionKey.OP_ACCEPT;

                sc.configureBlocking(false);
                SelectionKey key = sc.keyFor(selector);
                if (key == null) sc.register(selector, interest);
                else key.interestOps(key.interestOps() | interest);
                ch2fd.put(sc, fd);
            }

            int selected = (timeoutMs < 0) ? selector.select()
                    : (timeoutMs == 0) ? selector.selectNow()
                    : selector.select(timeoutMs);

            if (selected > 0) {
                for (SelectionKey k : selector.selectedKeys()) {
                    if (!k.isValid()) continue;
                    int fd = ch2fd.get(k.channel());
                    if (k.isAcceptable() || k.isReadable()) readyRead.add(fd);
                    if (k.isWritable()) readyWrite.add(fd);
                    if (k.isConnectable()) readyExcept.add(fd);
                }
                selector.selectedKeys().clear();
            }
        } finally {
            selector.keys().forEach(SelectionKey::cancel);
            selector.close();
        }

        // 返回结果
        pushResult(stack, readyRead, readyWrite, readyExcept);
    }
}
