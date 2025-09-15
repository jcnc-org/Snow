package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;

/**
 * {@code SetNonblockHandler} 用于实现系统调用 SET_NONBLOCK。
 *
 * <p>
 * 功能：设置虚拟 fd 的阻塞/非阻塞模式。
 * 对支持 {@link SelectableChannel} 的通道生效，
 * 其他类型的通道（例如 {@code FileChannel}）会被忽略。
 * </p>
 *
 * <p>调用约定：</p>
 * <ul>
 *   <li>入参：{@code fd:int}, {@code on:int(0/1)}</li>
 *   <li>出参：{@code 0:int}（表示成功）</li>
 * </ul>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>{@code on=1} → 非阻塞模式；{@code on=0} → 阻塞模式。</li>
 *   <li>如果底层通道不支持非阻塞模式，则忽略操作，依旧返回成功。</li>
 * </ul>
 *
 * <p>异常：</p>
 * <ul>
 *   <li>如果参数不是整数，抛出 {@link IllegalArgumentException}</li>
 *   <li>如果 fd 无效，抛出 {@link IllegalArgumentException}</li>
 * </ul>
 */
public class SetNonblockHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 从操作数栈中依次弹出参数：on:int（栈顶），fd:int（栈底）
        Object onObj = stack.pop();
        Object fdObj = stack.pop();

        // 参数类型校验
        if (!(fdObj instanceof Number) || !(onObj instanceof Number)) {
            throw new IllegalArgumentException("SET_NONBLOCK: arguments must be int, got: "
                    + fdObj + ", " + onObj);
        }

        int fd = ((Number) fdObj).intValue();
        int on = ((Number) onObj).intValue();

        // 获取 Channel
        Channel ch = FDTable.get(fd);
        if (ch == null) {
            throw new IllegalArgumentException("SET_NONBLOCK: invalid fd " + fd);
        }

        // 如果支持 SelectableChannel，设置阻塞/非阻塞模式
        if (ch instanceof SelectableChannel selectable) {
            selectable.configureBlocking(on == 0); // on=1 → 非阻塞，所以 blocking=false
        }

        // 成功返回 0
        stack.push(0);
    }
}
