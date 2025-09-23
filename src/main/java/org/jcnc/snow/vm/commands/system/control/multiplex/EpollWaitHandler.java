package org.jcnc.snow.vm.commands.system.control.multiplex;

import org.jcnc.snow.vm.commands.system.control.multiplex.utils.SelectorUtils;
import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.EpollInstance;
import org.jcnc.snow.vm.io.EpollRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;

/**
 * {@code EpollWaitHandler} 实现 EPOLL_WAIT (0x1303) 系统调用，
 * 用于等待 epoll 实例中已注册的 I/O 事件。
 *
 * <p><b>Stack：</b> 入参 {@code (epfd:int, max:int, timeout_ms:int)} →
 * 出参 {@code (events: List<Map{fd:int, events:int}})}</p>
 *
 * <p><b>语义：</b>
 * 阻塞（或超时）等待 epoll 实例中的 I/O 事件，返回不超过 {@code max} 个就绪事件。
 * 每个事件由 {@code {"fd":int, "events":int}} 组成，其中 {@code events} 为事件掩码。
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
 * 成功时返回一个事件列表，若无事件则返回空列表。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>{@code max ≤ 0} 时抛出 {@link IllegalArgumentException}</li>
 *   <li>{@code epfd} 非法或未注册时抛出 {@link IllegalArgumentException}</li>
 *   <li>I/O 操作过程中可能抛出 {@link java.io.IOException}</li>
 * </ul>
 * </p>
 */
public class EpollWaitHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 依次出栈 timeout_ms, max, epfd
        int timeoutMs = (int) stack.pop();
        int max = (int) stack.pop();
        int epfd = (int) stack.pop();

        if (max <= 0) {
            throw new IllegalArgumentException("EPOLL_WAIT: max must be > 0");
        }

        // 获取 epoll 实例和 Selector
        EpollInstance instance = EpollRegistry.get(epfd);
        if (instance == null) {
            throw new IllegalArgumentException("EPOLL_WAIT: invalid epfd -> " + epfd);
        }
        Selector selector = instance.getSelector();

        // 阻塞等待，返回活跃事件数
        int n = SelectorUtils.selectWithTimeout(selector, timeoutMs);

        List<Map<String, Object>> result = new ArrayList<>();
        if (n > 0) {
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> it = keys.iterator();
            while (it.hasNext() && result.size() < max) {
                SelectionKey key = it.next();
                it.remove();

                if (!key.isValid()) continue;

                // 组装事件掩码
                int events = 0;
                if (key.isReadable() || key.isAcceptable()) events |= 1;
                if (key.isWritable()) events |= 2;
                if (key.isConnectable()) events |= 4;

                // 查找 fd 并加入结果集
                Integer fd = instance.fdOf(key.channel());
                if (fd != null) {
                    result.add(Map.of("fd", fd, "events", events));
                }
            }
        }

        // 压回事件结果数组
        stack.push(result);
    }
}
