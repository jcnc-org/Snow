package org.jcnc.snow.vm.commands.system.control.multiplex;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code IoWaitHandler} 实现 IO_WAIT (0x1304) 系统调用，
 * 用于在虚拟机中等待一组 fd 的 I/O 事件。
 *
 * <p><b>Stack</b>：入参 {@code (fds:array, timeout_ms:int)} → 出参 {@code (events:array)}</p>
 *
 * <p><b>语义</b>：对一组文件描述符等待指定事件。当前简化实现：
 * 直接返回传入的 fds 数组，表示所有请求的事件均已就绪。</p>
 *
 * <p><b>返回</b>：数组元素为 {@code {fd:int, events:int}} 的 map。</p>
 *
 * <p><b>异常</b>：参数类型错误时抛出 {@link IllegalArgumentException}。</p>
 */
public class IoWaitHandler implements SyscallHandler {

    @Override
    @SuppressWarnings("unchecked")
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 入参：timeout, fds
        int timeoutMs = (int) stack.pop();
        Object fdsObj = stack.pop();

        if (!(fdsObj instanceof List<?> fdsList)) {
            throw new IllegalArgumentException("IO_WAIT: fds 必须是数组类型");
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Object obj : fdsList) {
            if (!(obj instanceof Map<?, ?> fdMap)) {
                throw new IllegalArgumentException("IO_WAIT: fds 元素必须是 {fd:int, events:int} map");
            }

            Object fdVal = fdMap.get("fd");
            Object evVal = fdMap.get("events");

            if (!(fdVal instanceof Integer) || !(evVal instanceof Integer)) {
                throw new IllegalArgumentException("IO_WAIT: fd 和 events 必须是 int");
            }

            // 简化：假设所有请求的事件都就绪，直接返回
            result.add(Map.of(
                    "fd", (Integer) fdVal,
                    "events", (Integer) evVal
            ));
        }

        // TODO: 如果未来要实现真正的多路等待，可以在这里接入 Select/Epoll，并考虑 timeoutMs
        stack.push(result);
    }
}
