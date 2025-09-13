package org.jcnc.snow.vm.commands.system.control;

import org.jcnc.snow.vm.module.OperandStack;

import java.nio.file.OpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.nio.file.StandardOpenOption.*;

/**
 * 公共工具函数，用于 SyscallCommand。
 */
public class SyscallUtils {

    /**
     * 将 flags 转换为 NIO OpenOption
     */
    public static Set<OpenOption> flagsToOptions(int flags) {
        Set<OpenOption> opts = new HashSet<>();
        // 写标志
        if ((flags & 0x1) != 0) opts.add(WRITE);
        else opts.add(READ);
        if ((flags & 0x40) != 0) opts.add(CREATE);
        if ((flags & 0x200) != 0) opts.add(TRUNCATE_EXISTING);
        if ((flags & 0x400) != 0) opts.add(APPEND);
        return opts;
    }

    /**
     * 统一错误处理
     */
    public static void pushErr(OperandStack stack, Exception e) {
        stack.push(-1);
        System.err.println("Syscall exception: " + e);
    }

    /**
     * 控制台输出
     */
    public static void output(Object obj, boolean newline) {
        String str;
        if (obj == null) {
            str = "null";
        } else if (obj instanceof byte[] bytes) {
            str = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        } else if (obj.getClass().isArray()) {
            str = arrayToString(obj);
        } else {
            str = obj.toString();
        }
        if (newline) System.out.println(str);
        else System.out.print(str);
    }

    /**
     * 数组转字符串
     */
    public static String arrayToString(Object array) {
        if (array instanceof int[] a) return Arrays.toString(a);
        if (array instanceof long[] a) return Arrays.toString(a);
        if (array instanceof double[] a) return Arrays.toString(a);
        if (array instanceof float[] a) return Arrays.toString(a);
        if (array instanceof short[] a) return Arrays.toString(a);
        if (array instanceof char[] a) return Arrays.toString(a);
        if (array instanceof byte[] a) return Arrays.toString(a);
        if (array instanceof boolean[] a) return Arrays.toString(a);
        if (array instanceof Object[] a) return Arrays.deepToString(a);
        return "Unsupported array";
    }
}
