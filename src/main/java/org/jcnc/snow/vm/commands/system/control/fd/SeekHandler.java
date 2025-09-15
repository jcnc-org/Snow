package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.channels.SeekableByteChannel;

/**
 * {@code SeekHandler} 用于实现系统调用 SEEK。
 *
 * <p>
 * 功能：移动文件指针到指定位置，并返回新的位置。
 * </p>
 *
 * <p>调用约定：</p>
 * <ul>
 *   <li>入参：{@code fd:int}, {@code offset:long}, {@code whence:int}</li>
 *   <li>出参：{@code newPos:long}</li>
 * </ul>
 *
 * <p>whence 参数：</p>
 * <ul>
 *   <li>0 = SEEK_SET：从文件开头计算偏移</li>
 *   <li>1 = SEEK_CUR：从当前位置计算偏移</li>
 *   <li>2 = SEEK_END：从文件末尾计算偏移</li>
 * </ul>
 *
 * <p>异常：</p>
 * <ul>
 *   <li>如果 fd 无效或不是可定位通道，抛出 {@link IllegalArgumentException}</li>
 *   <li>如果 whence 无效，抛出 {@link IllegalArgumentException}</li>
 *   <li>I/O 操作失败时，抛出 {@link java.io.IOException}</li>
 * </ul>
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
        if (!(ch instanceof SeekableByteChannel)) {
            throw new IllegalArgumentException("SEEK: fd " + fd + " is not seekable");
        }
        SeekableByteChannel sbc = (SeekableByteChannel) ch;

        // 计算新位置
        long newPos;
        switch (whence) {
            case 0: // SEEK_SET
                newPos = offset;
                break;
            case 1: // SEEK_CUR
                newPos = sbc.position() + offset;
                break;
            case 2: // SEEK_END
                newPos = sbc.size() + offset;
                break;
            default:
                throw new IllegalArgumentException("SEEK: invalid whence " + whence);
        }

        if (newPos < 0) {
            throw new IllegalArgumentException("SEEK: resulting position < 0");
        }

        // 设置文件指针位置
        sbc.position(newPos);

        // 返回新的位置
        stack.push(newPos);
    }
}
