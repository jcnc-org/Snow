package org.jcnc.snow.vm.commands.system.control.multiplex;

import org.jcnc.snow.vm.commands.system.control.multiplex.utils.SelectorUtils;
import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;

/**
 * {@code IoWaitHandler} 实现 IO_WAIT (0x1304) 系统调用。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (fds: List<Map{fd:int, events:int}}, timeout_ms:int)} →
 * 出参 {@code (events: List<Map{fd:int, events:int}})}
 * </p>
 *
 * <p><b>语义：</b>
 * 对指定的 fd 集合等待 I/O 事件直到超时，返回就绪的 (fd, events) 列表。
 * 事件位定义：READ=1、WRITE=2、CONNECT=4。
 * <ul>
 *   <li>对 {@link SelectableChannel}，通过 Java NIO {@link Selector} 监听对应事件。</li>
 *   <li>标准输入/输出/错误（fd=0/1/2）兼容处理：
 *     <ul>
 *       <li>fd=0 (stdin) 监听 READ，通过 {@link System#in#available()} 检查可读</li>
 *       <li>fd=1/2 (stdout/stderr) 监听 WRITE，视为始终可写</li>
 *     </ul>
 *   </li>
 * </ul>
 * </p>
 *
 * <p><b>返回：</b>
 * 返回所有就绪的事件数组，元素为 {@code {"fd":int, "events":int}}。
 * 若无事件就绪，返回空数组。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>fds 参数类型非法时抛出 {@link IllegalArgumentException}</li>
 *   <li>fd 或事件参数非法、底层 I/O 错误时抛出异常</li>
 * </ul>
 * </p>
 */
public class IoWaitHandler implements SyscallHandler {

    private static boolean waitStdinReadable(int timeoutMs) throws IOException, InterruptedException {
        if (timeoutMs == 0) return System.in.available() > 0;
        long deadline = (timeoutMs < 0) ? Long.MAX_VALUE : System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (System.in.available() > 0) return true;
            Thread.sleep(10); // 10ms 轮询
        }
        return false;
    }

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 入参出栈：timeout_ms, fds
        int timeoutMs = (int) stack.pop();
        Object fdsObj = stack.pop();

        if (!(fdsObj instanceof List<?> fdsList)) {
            throw new IllegalArgumentException("IO_WAIT: fds 必须是数组类型");
        }

        // 1. 处理不可选择通道（fd 0/1/2），收集需要注册到 Selector 的条目
        List<Map<String, Object>> result = new ArrayList<>();
        List<Map.Entry<Integer, Integer>> toRegister = new ArrayList<>(); // (fd, interestOps)

        for (Object obj : fdsList) {
            int fd;
            int events;

            if (obj instanceof Integer intFd) {
                fd = intFd;
                events = 1; // 默认监听 READ
            } else if (obj instanceof Map<?, ?> fdMap) {
                Object fdVal = fdMap.get("fd");
                Object evVal = fdMap.get("events");
                if (!(fdVal instanceof Integer) || !(evVal instanceof Integer)) {
                    throw new IllegalArgumentException("IO_WAIT: fd 和 events 必须是 int");
                }
                fd = (int) fdVal;
                events = (int) evVal;
            } else {
                throw new IllegalArgumentException("IO_WAIT: fds 元素必须是 int 或 {fd:int, events:int} map");
            }

            Channel ch = FDTable.get(fd);
            if (!(ch instanceof SelectableChannel)) {
                int readyEv = 0;
                if (fd == 0 && (events & 1) != 0) {
                    if (waitStdinReadable(timeoutMs)) readyEv |= 1;
                }
                if ((fd == 1 || fd == 2) && (events & 2) != 0) {
                    readyEv |= 2;
                }
                if (readyEv != 0) {
                    result.add(Map.of("fd", fd, "events", readyEv));
                }
                continue;
            }

            // 可选择通道：转换 Snow 事件位到 Java interestOps
            int ops = 0;
            if ((events & 1) != 0) ops |= SelectionKey.OP_READ | SelectionKey.OP_ACCEPT;
            if ((events & 2) != 0) ops |= SelectionKey.OP_WRITE;
            if ((events & 4) != 0) ops |= SelectionKey.OP_CONNECT;
            toRegister.add(new AbstractMap.SimpleEntry<>(fd, ops));
        }

        // 没有需要注册的通道，直接返回
        if (toRegister.isEmpty()) {
            stack.push(result);
            return;
        }

        // 2. 使用 Selector 监听剩余通道
        try (Selector selector = Selector.open()) {
            Map<SelectionKey, Integer> keyToFd = new HashMap<>();

            for (Map.Entry<Integer, Integer> e : toRegister) {
                int fd = e.getKey();
                int ops = e.getValue();
                Channel ch = FDTable.get(fd);

                SelectableChannel sc = (SelectableChannel) ch;
                sc.configureBlocking(false);
                SelectionKey key = sc.register(selector, ops);
                keyToFd.put(key, fd);
            }

            int n = SelectorUtils.selectWithTimeout(selector, timeoutMs);

            if (n > 0) {
                for (SelectionKey key : selector.selectedKeys()) {
                    Integer fd = keyToFd.get(key);
                    if (fd == null) continue;

                    int ev = 0;
                    if (key.isReadable() || key.isAcceptable()) ev |= 1;
                    if (key.isWritable()) ev |= 2;
                    if (key.isConnectable()) ev |= 4;

                    if (ev != 0) {
                        result.add(Map.of("fd", fd, "events", ev));
                    }
                }
                selector.selectedKeys().clear();
            }
        }

        stack.push(result);
    }
}
