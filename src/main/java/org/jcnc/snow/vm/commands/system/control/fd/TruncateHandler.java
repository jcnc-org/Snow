package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * {@code TruncateHandler} 用于实现系统调用 TRUNCATE。
 *
 * <p>
 * 功能：将路径对应文件截断到指定长度；若文件不存在则创建后再截断。
 * </p>
 *
 * <p><b>Stack</b>：
 * 入参 (path:String, length:long) ——> 出参 (rc:int，成功时为 0)
 * </p>
 *
 * <p><b>语义</b>：
 * 以可写方式（必要时创建）打开文件并调用 {@link SeekableByteChannel#truncate(long)}。
 * 成功时压入 0，以保持与 VM 对 syscall 语句的通用“弹栈存本地”代码生成的栈约定一致。
 * 失败时抛出异常，由上层统一转为 errno/errstr。
 * </p>
 */
public class TruncateHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 先弹出 length 再弹出 path
        Object lengthObj = stack.pop();
        Object pathObj = stack.pop();

        if (!(pathObj instanceof CharSequence)) {
            throw new IllegalArgumentException("TRUNCATE: path must be a string");
        }
        if (!(lengthObj instanceof Number)) {
            throw new IllegalArgumentException("TRUNCATE: length must be a number");
        }

        String pathStr = pathObj.toString();
        long length = ((Number) lengthObj).longValue();
        Path path = Paths.get(pathStr);

        // 打开文件通道（必须可写，若不存在则创建）
        try (SeekableByteChannel ch = java.nio.file.Files.newByteChannel(
                path,
                java.util.Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE))) {

            // 截断到指定长度
            ch.truncate(length);
        }

        // 成功：压入返回码 0（保持栈平衡，匹配编译器对 syscall 语句的 I_STORE 弹栈行为）
        stack.push(0);
    }
}
