package org.jcnc.snow.vm.commands.system.control;

import org.jcnc.snow.vm.module.OperandStack;

import java.util.Arrays;
import java.nio.charset.StandardCharsets;

/**
 * {@code SyscallUtils} 提供系统调用相关的全局工具函数，供 {@link SyscallCommand} 及其子模块使用。
 * <p>
 * 主要功能：
 * <ul>
 *   <li>统一管理虚拟机全局的 errno/errstr 错误状态</li>
 *   <li>提供错误信息压栈及清空工具</li>
 *   <li>控制台输出与数组打印工具</li>
 * </ul>
 */
public class SyscallUtils {

    private SyscallUtils() {
    }

    /**
     * 全局 errno（进程/虚拟机级），-1 表示最近一次系统调用失败
     */
    private static volatile int LAST_ERRNO = 0;
    /**
     * 全局错误字符串（进程/虚拟机级）
     */
    private static volatile String LAST_ERRSTR = null;

    /**
     * 获取最近一次系统调用的 errno。
     *
     * @return 错误码，0 表示无错，-1 表示失败
     */
    public static int getErrno() {
        return LAST_ERRNO;
    }

    /**
     * 获取最近一次系统调用的错误信息字符串。
     *
     * @return 错误描述（如无错可能为 null）
     */
    public static String getErrStr() {
        return LAST_ERRSTR;
    }

    /**
     * 清空全局错误状态（errno=0，errstr=null）。
     */
    public static void clearErr() {
        LAST_ERRNO = 0;
        LAST_ERRSTR = null;
    }

    /**
     * 记录错误并在操作数栈顶压入 -1（int）。
     * <ul>
     *   <li>errno 固定为 -1</li>
     *   <li>errstr 记录异常类型与消息，如 {@code NullPointerException: ...}</li>
     *   <li>允许 e 为 null，表示“未知系统调用错误”</li>
     * </ul>
     *
     * @param stack 当前虚拟机操作数栈
     * @param e     异常对象（可为 null）
     */
    public static void pushErr(OperandStack stack, Exception e) {
        LAST_ERRNO = -1;
        LAST_ERRSTR = (e == null)
                ? "Unknown syscall error"
                : (e.getClass().getSimpleName() + ": " + e.getMessage());
        stack.push(-1);
    }

    /**
     * 控制台输出工具。
     * <ul>
     *   <li>若 {@code newline} 为 true，自动追加换行</li>
     *   <li>null    → 打印字符串 "null"</li>
     *   <li>byte[]  → 以 UTF-8 解码后打印</li>
     *   <li>其它数组→ 调用 {@link #arrayToString(Object)}</li>
     *   <li>其它对象→ toString()</li>
     * </ul>
     *
     * @param obj     要输出的对象
     * @param newline 是否追加换行
     */
    public static void output(Object obj, boolean newline) {
        String str;
        if (obj == null) {
            str = "null";
        } else if (obj instanceof byte[] bytes) {
            str = new String(bytes, StandardCharsets.UTF_8);
        } else if (obj.getClass().isArray()) {
            str = arrayToString(obj);
        } else {
            str = obj.toString();
        }
        if (newline) System.out.println(str);
        else System.out.print(str);
    }

    /**
     * 各种基本类型数组转字符串，Object[] 调用深度 toString，其他类型输出“Unsupported array”。
     *
     * @param array 数组对象
     * @return 规范化字符串
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
