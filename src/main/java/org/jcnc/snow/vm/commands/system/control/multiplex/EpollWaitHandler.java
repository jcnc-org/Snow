package org.jcnc.snow.vm.commands.system.control.multiplex;

import org.jcnc.snow.vm.commands.system.control.multiplex.utils.SelectorUtils;
import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.EpollInstance;
import org.jcnc.snow.vm.io.EpollRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;

/**
 * {@code EpollWaitHandler} 实现 EPOLL_WAIT (0x1303) 系统调用，
 * 用于等待 epoll 实例中已注册的 I/O 事件，并兼容标准流 fd=0/1/2。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (epfd:int, max:int, timeout_ms:int)} →
 * 出参 {@code (events: List<Map{fd:int, events:int}})}
 * </p>
 *
 * <p><b>语义：</b>
 * 阻塞或超时等待 epoll 实例中已注册 fd 的 I/O 事件，最多返回 {@code max} 个活跃事件。
 * 支持伪 fd（fd=0/1/2）检测：
 * <ul>
 *   <li>fd=0（stdin）：可读事件由 {@code System.in.available()} 判断</li>
 *   <li>fd=1/2（stdout/stderr）：写事件始终就绪</li>
 * </ul>
 * 其它 fd 采用 Java NIO {@link Selector} 检查就绪状态。
 * 事件掩码：1=READ, 2=WRITE, 4=CONNECT。
 * </p>
 *
 * <p><b>返回：</b>
 * 成功时返回不超过 {@code max} 个就绪事件，数组元素为 {@code {"fd":int, "events":int}}。
 * 若无事件触发，则返回空数组。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>epfd 非法、max 非法时抛出 {@link IllegalArgumentException}</li>
 *   <li>底层 I/O 错误时抛出 {@link IOException}</li>
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

        EpollInstance instance = EpollRegistry.get(epfd);
        Selector selector = instance.getSelector();

        // 1. 检查 fd（标准流）就绪情况
        Map<Integer, Integer> pseudoFds = instance.getPseudoFds();
        List<Map<String, Object>> ready = new ArrayList<>();
        long deadline = (timeoutMs < 0) ? Long.MAX_VALUE : System.currentTimeMillis() + timeoutMs;
        boolean found = false;

        do {
            for (var entry : pseudoFds.entrySet()) {
                int fd = entry.getKey();
                int events = entry.getValue();
                // 1 = READ, 2 = WRITE
                try {
                    if (fd == 0 && (events & 1) != 0) { // stdin 可读
                        if (System.in.available() > 0) {
                            ready.add(Map.of("fd", 0, "events", 1));
                        }
                    }
                    if ((fd == 1 || fd == 2) && (events & 2) != 0) { // stdout/stderr 可写
                        ready.add(Map.of("fd", fd, "events", 2));
                    }
                } catch (IOException ignored) {
                }
                if (ready.size() >= max) break;
            }
            if (!ready.isEmpty() || timeoutMs == 0) {
                break;
            }
            if (System.currentTimeMillis() >= deadline) break;
            Thread.sleep(10);
        } while (!found);

        // 如果已就绪（或者 max 满了），直接返回
        if (!ready.isEmpty()) {
            stack.push(ready.size() > max ? ready.subList(0, max) : ready);
            return;
        }

        // 2. Selector 检查其它 fd
        int n = SelectorUtils.selectWithTimeout(selector, timeoutMs);

        if (n > 0) {
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> it = keys.iterator();
            while (it.hasNext() && ready.size() < max) {
                SelectionKey key = it.next();
                it.remove();

                if (!key.isValid()) continue;

                int ev = 0;
                if (key.isReadable() || key.isAcceptable()) ev |= 1;
                if (key.isWritable()) ev |= 2;
                if (key.isConnectable()) ev |= 4;

                Integer fd = instance.fdOf(key.channel());
                if (fd != null) {
                    ready.add(Map.of("fd", fd, "events", ev));
                }
            }
        }
        stack.push(ready);
    }
}
