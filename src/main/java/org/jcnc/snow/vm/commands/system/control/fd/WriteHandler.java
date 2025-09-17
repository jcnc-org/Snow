package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;

/**
 * {@code WriteHandler} 实现 WRITE (0x1002) 系统调用，
 * 将数据写入指定虚拟文件描述符（fd）对应的通道。
 *
 * <p><b>Stack</b>：入参 {@code (fd:int, data:any)} → 出参 {@code (written:int)}</p>
 *
 * <p><b>语义</b>：向 fd 指定的通道写入数据，支持 byte[]、String、null、其他类型会 toString()。</p>
 *
 * <p><b>返回</b>：实际写入的字节数（int）。</p>
 *
 * <p><b>支持的数据类型</b>：
 * <ul>
 *   <li>byte[]：按原样写入</li>
 *   <li>String：UTF-8 编码写入</li>
 *   <li>null：写入长度 0</li>
 *   <li>其他类型：toString() 后 UTF-8 编码写入</li>
 * </ul>
 * </p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>fd 不是 int 时抛出 {@link IllegalArgumentException}</li>
 *   <li>fd 不可写时抛出 {@link IllegalArgumentException}</li>
 *   <li>写入失败时抛出 {@link java.io.IOException}</li>
 * </ul>
 * </p>
 */
public class WriteHandler implements SyscallHandler {

    /**
     * 处理 WRITE 调用。
     *
     * @param stack     操作数栈，依次提供 fd、data
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception 参数错误或写入失败时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 1. 取出参数（先弹 data，再弹 fd）
        Object dataObj = stack.pop();   // data:any
        Object fdObj = stack.pop();   // fd:int

        if (!(fdObj instanceof Integer)) {
            throw new IllegalArgumentException("WRITE: fd must be an int");
        }
        int fd = (Integer) fdObj;

        // 2. 统一将数据转换为 byte[]
        byte[] data = switch (dataObj) {
            case byte[] b -> b;
            case String s -> s.getBytes(StandardCharsets.UTF_8);
            case null -> new byte[0];
            default -> dataObj.toString().getBytes(StandardCharsets.UTF_8);
        };

        // 3. 获取并检查可写通道
        var ch = FDTable.get(fd);
        if (!(ch instanceof WritableByteChannel wch)) {
            throw new IllegalArgumentException("WRITE: fd " + fd + " is not writable");
        }

        // 4. 执行写入
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int written = wch.write(buffer);

        // 5. 将写入字节数压回栈
        stack.push(written);
    }
}
