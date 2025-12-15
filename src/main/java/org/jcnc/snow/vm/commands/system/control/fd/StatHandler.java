package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;
import org.jcnc.snow.vm.runtime.SnowDictObject;
import org.jcnc.snow.vm.runtime.SnowRuntime;
import org.jcnc.snow.vm.runtime.SnowStringObject;
import org.jcnc.snow.vm.value.IntValue;
import org.jcnc.snow.vm.value.LongValue;
import org.jcnc.snow.vm.value.RefValue;
import org.jcnc.snow.vm.value.Value;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * {@code StatHandler} 用于实现系统调用 STAT。
 *
 * <p>
 * 功能：获取文件的基本属性，例如大小、创建时间、修改时间等。
 * </p>
 *
 * <p>调用约定：</p>
 * <ul>
 *   <li>入参：{@code path:string}</li>
 *   <li>出参：{@code FileStat}（自定义的属性对象，或用 Map/Record 表示）</li>
 * </ul>
 *
 * <p>返回字段（示例）：</p>
 * <ul>
 *   <li>{@code size:long} 文件大小（字节）</li>
 *   <li>{@code isDirectory:boolean} 是否目录</li>
 *   <li>{@code lastModified:long} 最后修改时间（epoch millis）</li>
 *   <li>{@code created:long} 创建时间（epoch millis）</li>
 * </ul>
 *
 * <p>异常：</p>
 * <ul>
 *   <li>如果 path 不是字符串，抛出 {@link IllegalArgumentException}</li>
 *   <li>如果文件不存在，抛出 {@link java.nio.file.NoSuchFileException}</li>
 *   <li>I/O 操作失败时，抛出 {@link java.io.IOException}</li>
 * </ul>
 */
public class StatHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 从操作数栈中弹出参数：path:string
        Value pathV = stack.popValue();
        if (!(pathV instanceof RefValue(int pid))) {
            throw new IllegalArgumentException("STAT: path must be a string");
        }
        var pobj = SnowRuntime.get().heap().get(pid);
        if (!(pobj instanceof SnowStringObject pstr)) {
            throw new IllegalArgumentException("STAT: path must be a string");
        }
        String pathStr = pstr.value();

        Path path = Path.of(pathStr);

        // 获取文件基本属性
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);

        // 构造结果（Snow dict）
        var result = new SnowDictObject();
        result.put("size", new LongValue(attrs.size()));
        result.put("isDirectory", new IntValue(attrs.isDirectory() ? 1 : 0));
        result.put("isRegularFile", new IntValue(attrs.isRegularFile() ? 1 : 0));
        result.put("lastModified", new LongValue(attrs.lastModifiedTime().toMillis()));
        result.put("created", new LongValue(attrs.creationTime().toMillis()));

        int rid = SnowRuntime.get().heap().alloc(result);
        stack.pushValue(new RefValue(rid));
    }
}