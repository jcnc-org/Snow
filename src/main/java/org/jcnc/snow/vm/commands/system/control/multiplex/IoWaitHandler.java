package org.jcnc.snow.vm.commands.system.control.multiplex;

import org.jcnc.snow.vm.commands.system.control.multiplex.utils.SelectorUtils;
import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;

/**
 * {@code IoWaitHandler} 实现 IO_WAIT (0x1304) 系统调用，
 * 用于等待一组文件描述符 (fd) 的 I/O 事件（多路复用，基于 Java NIO Selector）。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (fds: List<Map{fd:int, events:int}}, timeout_ms:int)} →
 * 出参 {@code (events: List<Map{fd:int, events:int}})}
 * </p>
 *
 * <p><b>语义：</b>
 * 调用方提供若干 fd 及其关注的事件掩码，系统阻塞等待直至有事件就绪或超时。
 * 最多返回与入参中 fd 数量相同的事件列表，每项包含 {@code {"fd":int, "events":int}}。
 * </p>
 *
 * <p><b>事件掩码：</b>
 * <ul>
 *   <li>{@code 1} = READ（可读或可接受连接）</li>
 *   <li>{@code 2} = WRITE（可写）</li>
 *   <li>{@code 4} = CONNECT（连接就绪）</li>
 * </ul>
 * </p>
 *
 * <p><b>返回：</b>
 * 成功时返回一个事件列表；若超时或无事件触发，则返回空列表。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>{@code fds} 参数不是数组类型时抛出 {@link IllegalArgumentException}</li>
 *   <li>{@code fds} 元素不是 {@code {fd:int, events:int}} map 时抛出 {@link IllegalArgumentException}</li>
 *   <li>{@code fd} 或 {@code events} 不是 int 时抛出 {@link IllegalArgumentException}</li>
 *   <li>I/O 相关错误可能抛出 {@link java.io.IOException}</li>
 * </ul>
 * </p>
 */
public class IoWaitHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 入参出栈：timeout_ms, fds
        int timeoutMs = (int) stack.pop();
        Object fdsObj = stack.pop();

        // 校验 fds 类型
        if (!(fdsObj instanceof List<?> fdsList)) {
            throw new IllegalArgumentException("IO_WAIT: fds 必须是数组类型");
        }

        try (Selector selector = Selector.open()) {
            // keyToFd 映射 selection key -> fd
            Map<SelectionKey, Integer> keyToFd = new HashMap<>();

            // 遍历每个 fd+events 配置
            for (Object obj : fdsList) {
                if (!(obj instanceof Map<?, ?> fdMap)) {
                    throw new IllegalArgumentException("IO_WAIT: fds 元素必须是 {fd:int, events:int} map");
                }

                Object fdVal = fdMap.get("fd");
                Object evVal = fdMap.get("events");

                if (!(fdVal instanceof Integer fd) || !(evVal instanceof Integer events)) {
                    throw new IllegalArgumentException("IO_WAIT: fd 和 events 必须是 int");
                }

                Channel ch = FDTable.get(fd);
                if (!(ch instanceof SelectableChannel sc)) {
                    continue; // 跳过不可选择通道
                }
                sc.configureBlocking(false);

                // 计算 SelectionKey interestOps
                int ops = 0;
                if ((events & 1) != 0) ops |= SelectionKey.OP_READ;    // READ
                if ((events & 2) != 0) ops |= SelectionKey.OP_WRITE;   // WRITE
                if ((events & 4) != 0) ops |= SelectionKey.OP_CONNECT; // CONNECT

                SelectionKey key = sc.register(selector, ops);
                keyToFd.put(key, fd);
            }

            // 等待事件，返回活跃 key 数
            int n = SelectorUtils.selectWithTimeout(selector, timeoutMs);

            // 收集活跃事件
            List<Map<String, Object>> result = new ArrayList<>();
            if (n > 0) {
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                for (SelectionKey key : selectedKeys) {
                    if (!key.isValid()) continue;
                    Integer fd = keyToFd.get(key);
                    if (fd == null) continue;

                    int ev = 0;
                    if (key.isReadable() || key.isAcceptable()) ev |= 1;
                    if (key.isWritable()) ev |= 2;
                    if (key.isConnectable()) ev |= 4;

                    result.add(Map.of("fd", fd, "events", ev));
                }
                selectedKeys.clear();
            }

            // 压回事件数组
            stack.push(result);
        }
    }
}
