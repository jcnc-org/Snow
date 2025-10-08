package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.channels.SeekableByteChannel;

/**
 * {@code SeekHandler} 实现 SEEK (0x1003) 系统调用，
 * 用于移动文件指针并返回新位置。
 *
 * <p><b>Stack</b>：入参 {@code (fd:int, offset:long, whence:int)} → 出参 {@code (newPos:long)}</p>
 *
 * <p><b>whence</b>：
 * <ul>
 *   <li>0 = 从文件开头（SEEK_SET）</li>
 *   <li>1 = 相对当前位置（SEEK_CUR）</li>
 *   <li>2 = 相对文件末尾（SEEK_END）</li>
 * </ul>
 * </p>
 *
 * <p><b>语义</b>：仅适用于 {@code SeekableByteChannel}。结果位置不得为负。</p>
 *
 * <p><b>返回</b>：新的文件指针位置（long）。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>fd 非法/不可定位时抛出 {@link IllegalArgumentException}</li>
 *   <li>whence 非法时抛出 {@link IllegalArgumentException}</li>
 *   <li>计算得到的新位置为负时抛出 {@link IllegalArgumentException}</li>
 *   <li>I/O 失败时抛出 {@link java.io.IOException}</li>
 * </ul>
 * </p>
 */
public class SeekHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 从操作数栈中弹出参数：先 whence:int（栈顶），再 offset:long，中 fd:int（最底）
        Object whenceObj = stack.pop();
        Object offsetObj = stack.pop();
        Object fdObj = stack.pop();

        // 校验参数类型
        if (!(fdObj instanceof Number)) {
            throw new IllegalArgumentException("SEEK: fd must be an int, got: " + fdObj);
        }
        if (!(offsetObj instanceof Number)) {
            throw new IllegalArgumentException("SEEK: offset must be a long, got: " + offsetObj);
        }
        if (!(whenceObj instanceof Number)) {
            throw new IllegalArgumentException("SEEK: whence must be an int, got: " + whenceObj);
        }

        int fd = ((Number) fdObj).intValue();
        long offset = ((Number) offsetObj).longValue();
        int whence = ((Number) whenceObj).intValue();

        // 从 FDTable 获取通道
        var ch = FDTable.get(fd);
        if (!(ch instanceof SeekableByteChannel sbc)) {
            throw new IllegalArgumentException("SEEK: fd " + fd + " is not seekable");
        }

        // 计算新位置
        long newPos = switch (whence) {
            case 0 -> // SEEK_SET
                    offset;
            case 1 -> // SEEK_CUR
                    sbc.position() + offset;
            case 2 -> // SEEK_END
                    sbc.size() + offset;
            default -> throw new IllegalArgumentException("SEEK: invalid whence " + whence);
        };

        if (newPos < 0) {
            throw new IllegalArgumentException("SEEK: resulting position < 0");
        }

        // 设置文件指针位置
        sbc.position(newPos);

        // 返回新的位置
        stack.push(newPos);
    }
}
