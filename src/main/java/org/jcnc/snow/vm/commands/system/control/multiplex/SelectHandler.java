package org.jcnc.snow.vm.commands.system.control.multiplex;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;
import org.jcnc.snow.vm.runtime.SnowArrayObject;
import org.jcnc.snow.vm.runtime.SnowDictObject;
import org.jcnc.snow.vm.runtime.SnowRuntime;
import org.jcnc.snow.vm.value.IntValue;
import org.jcnc.snow.vm.value.RefValue;
import org.jcnc.snow.vm.value.Value;

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

    private static RefValue newIntArray(List<Integer> ints) {
        SnowArrayObject arr = new SnowArrayObject();
        for (Integer i : ints) {
            if (i == null) continue;
            arr.push(new IntValue(i));
        }
        int id = SnowRuntime.get().heap().alloc(arr);
        return new RefValue(id);
    }

    private static List<Integer> toIntList(Value v) {
        if (v == Value.NULL) return Collections.emptyList();
        if (!(v instanceof RefValue(int id))) {
            throw new IllegalArgumentException("SELECT: fd list must be an array");
        }
        var obj = SnowRuntime.get().heap().get(id);
        if (!(obj instanceof SnowArrayObject arr)) {
            throw new IllegalArgumentException("SELECT: fd list must be an array");
        }
        List<Integer> out = new ArrayList<>(arr.length());
        for (var item : arr.snapshot()) {
            int fd = switch (item) {
                case IntValue(int i) -> i;
                case org.jcnc.snow.vm.value.ShortValue(short s) -> s;
                case org.jcnc.snow.vm.value.ByteValue(byte b) -> b;
                case org.jcnc.snow.vm.value.LongValue(long l) -> (int) l;
                default -> throw new IllegalArgumentException("SELECT: fd list must contain integers");
            };
            out.add(fd);
        }
        return out;
    }

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 参数解析
        Value timeoutV = stack.popValue();
        Value exceptV = stack.popValue();
        Value writeV = stack.popValue();
        Value readV = stack.popValue();

        final int timeoutMs = (timeoutV == Value.NULL) ? -1 : switch (timeoutV) {
            case IntValue(int i) -> i;
            case org.jcnc.snow.vm.value.LongValue(long l) -> (int) l;
            default -> throw new IllegalArgumentException("SELECT: timeout must be int");
        };
        final List<Integer> readSet = toIntList(readV);
        final List<Integer> writeSet = toIntList(writeV);
        final List<Integer> exceptSet = toIntList(exceptV);

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
            var out = new SnowDictObject();
            out.put("read", newIntArray(readyRead));
            out.put("write", newIntArray(readyWrite));
            out.put("except", newIntArray(readyExcept));
            int outId = SnowRuntime.get().heap().alloc(out);
            stack.pushValue(new RefValue(outId));
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
        var out = new SnowDictObject();
        out.put("read", newIntArray(readyRead));
        out.put("write", newIntArray(readyWrite));
        out.put("except", newIntArray(readyExcept));
        int outId = SnowRuntime.get().heap().alloc(out);
        stack.pushValue(new RefValue(outId));
    }
}