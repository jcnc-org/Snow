package org.jcnc.snow.vm.commands.system.control.multiplex;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code SelectHandler} 实现 SELECT (0x1300) 系统调用，
 * 用于在虚拟机中执行多路复用等待。
 *
 * <p><b>Stack</b>：入参
 * {@code (readSet:array, writeSet:array, exceptSet:array, timeout_ms:int)} →
 * 出参 {@code (ready:map)}</p>
 *
 * <p><b>语义</b>：检查给定的 fd 集合中哪些处于就绪状态。
 * 当前简化实现：直接将输入的集合原样返回，表示所有 fd 都就绪。</p>
 *
 * <p><b>返回</b>：一个 map，包含键 {@code read}、{@code write}、{@code except}，
 * 值为对应就绪的 fd 列表。</p>
 *
 * <p><b>异常</b>：参数类型错误时抛出 {@link IllegalArgumentException}。</p>
 */
public class SelectHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 参数出栈顺序：timeout, exceptSet, writeSet, readSet
        int timeoutMs = (int) stack.pop();
        Object exceptSetObj = stack.pop();
        Object writeSetObj  = stack.pop();
        Object readSetObj   = stack.pop();

        if (!(readSetObj instanceof List<?> readSet) ||
                !(writeSetObj instanceof List<?> writeSet) ||
                !(exceptSetObj instanceof List<?> exceptSet)) {
            throw new IllegalArgumentException("SELECT: 参数必须是数组类型");
        }

        // 构造返回结果（假设所有 fd 都已就绪）
        Map<String, Object> ready = new HashMap<>();
        ready.put("read",  readSet);
        ready.put("write", writeSet);
        ready.put("except", exceptSet);

        // TODO: 可以在未来扩展，真正基于 NIO Selector 实现阻塞等待和超时逻辑
        // timeoutMs 当前仅为占位参数

        stack.push(ready);
    }
}
