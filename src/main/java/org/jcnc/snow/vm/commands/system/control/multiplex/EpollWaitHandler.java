package org.jcnc.snow.vm.commands.system.control.multiplex;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.EpollRegistry;
import org.jcnc.snow.vm.io.EpollInstance;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code EpollWaitHandler} 实现 EPOLL_WAIT (0x1303) 系统调用，
 * 用于等待 epoll 中的事件并返回就绪的 fd 列表。
 *
 * <p><b>Stack</b>：入参 {@code (epfd:int, max:int, timeout_ms:int)} → 出参 {@code (events:array)}</p>
 *
 * <p><b>语义</b>：从指定 epoll 实例中获取就绪事件，返回不超过 {@code max} 个。
 * 当前实现为简化版本：直接返回已注册的监控项集合，不做真正的 I/O 事件检测。</p>
 *
 * <p><b>返回</b>：数组元素为 {@code {fd:int, events:int}} 的 map。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>epfd 无效 → {@link IllegalArgumentException}</li>
 *   <li>参数类型错误或 max 非法 → {@link IllegalArgumentException}</li>
 * </ul>
 * </p>
 */
public class EpollWaitHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 参数出栈顺序：timeout_ms, max, epfd
        int timeoutMs = (int) stack.pop();
        int max       = (int) stack.pop();
        int epfd      = (int) stack.pop();

        if (max <= 0) {
            throw new IllegalArgumentException("EPOLL_WAIT: max must be > 0");
        }

        EpollInstance instance = EpollRegistry.get(epfd);

        // 收集事件
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : instance.getWatchMap().entrySet()) {
            if (result.size() >= max) break;

            result.add(Map.of(
                    "fd", e.getKey(),
                    "events", e.getValue()
            ));
        }

        // TODO: 如果要支持阻塞等待，这里可以实现 timeoutMs 的逻辑
        // 当前版本：直接返回快照

        stack.push(result);
    }
}
