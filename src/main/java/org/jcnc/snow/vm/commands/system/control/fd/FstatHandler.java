package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

/**
 * {@code FstatHandler} 用于实现系统调用 FSTAT。
 *
 * <p>
 * 功能：根据虚拟文件描述符（fd）获取对应文件的基本属性。
 * </p>
 *
 * <p>调用约定：</p>
 * <ul>
 *   <li>入参：{@code fd:int}</li>
 *   <li>出参：{@code stat:Map<String,Object>}</li>
 * </ul>
 *
 * <p>返回字段（示例）：</p>
 * <ul>
 *   <li>{@code size:long} 文件大小（字节）</li>
 *   <li>{@code isDirectory:boolean} 是否目录</li>
 *   <li>{@code isRegularFile:boolean} 是否普通文件</li>
 *   <li>{@code lastModified:long} 最后修改时间（epoch millis）</li>
 *   <li>{@code created:long} 创建时间（epoch millis）</li>
 * </ul>
 *
 * <p>异常：</p>
 * <ul>
 *   <li>如果 fd 无效或不是 {@link SeekableByteChannel}，抛出 {@link IllegalArgumentException}</li>
 *   <li>I/O 操作失败时，抛出 {@link IOException}</li>
 * </ul>
 */
public class FstatHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 从操作数栈中弹出参数：fd:int
        Object fdObj = stack.pop();

        // 校验参数类型
        if (!(fdObj instanceof Number)) {
            throw new IllegalArgumentException("FSTAT: fd must be an int, got: " + fdObj);
        }

        int fd = ((Number) fdObj).intValue();

        // 从 FDTable 获取通道
        var ch = FDTable.get(fd);
        if (!(ch instanceof SeekableByteChannel sbc)) {
            throw new IllegalArgumentException("FSTAT: fd " + fd + " is not seekable or invalid");
        }

        // 由于 Channel 本身无法直接拿 Path，需要 hack：尝试通过 size/position 验证文件存在
        // 这里假设 VM 使用 Files.newByteChannel(path, ...) 打开的文件，所以可用反射或者 FDTable 改进来存 Path。
        // 当前简化：只返回 size，其余字段标记不可用。
        var result = new java.util.HashMap<String, Object>();
        result.put("size", sbc.size());
        result.put("position", sbc.position());

        // 其余属性目前无法直接获取 Path → BasicFileAttributes，除非 FDTable 保存 path。
        // 暂时填充默认值
        result.put("isDirectory", false);
        result.put("isRegularFile", true);
        result.put("lastModified", -1L);
        result.put("created", -1L);

        // 压回栈
        stack.push(result);
    }
}
