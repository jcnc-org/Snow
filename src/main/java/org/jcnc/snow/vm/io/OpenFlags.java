package org.jcnc.snow.vm.io;

import java.nio.file.OpenOption;
import java.util.HashSet;
import java.util.Set;

import static java.nio.file.StandardOpenOption.*;

/**
 * {@code OpenFlags} 定义了 Snow VM 系统调用 {@code OPEN} 使用的全部标志位，
 * 并提供工具方法将这些标志转换为 Java NIO 的 {@link OpenOption} 集合。
 * <p>
 * 本类的常量和语义与 POSIX 标准（UNIX/Linux 的 open(2)）保持一致，
 * 用于描述虚拟机内部文件访问模式和创建行为。
 *
 * <p><b>标志定义：</b></p>
 * <ul>
 *   <li>{@code O_RDONLY = 0}：只读（默认）</li>
 *   <li>{@code O_WRONLY = 1}：只写</li>
 *   <li>{@code O_RDWR   = 2}：读写</li>
 *   <li>{@code O_CREAT  = 0x40}：文件不存在时创建</li>
 *   <li>{@code O_EXCL   = 0x80}：与 O_CREAT 配合，仅在文件不存在时创建，否则报错</li>
 *   <li>{@code O_TRUNC  = 0x200}：打开时将文件截断为长度为 0</li>
 *   <li>{@code O_APPEND = 0x400}：追加写，每次写入都追加到末尾</li>
 * </ul>
 *
 * <p><b>工具方法说明：</b></p>
 * <ul>
 *   <li>{@link #has(int, int)}：判断某一标志位是否被设置</li>
 *   <li>{@link #toOpenOptions(int)}：将 flags 转为 Java 的 {@link OpenOption} 集合，供 NIO 使用</li>
 *   <li>{@link #validate(int)}：检查 flags 组合的基本一致性，发现冲突时抛出异常</li>
 * </ul>
 *
 * <p><b>一致性校验举例：</b></p>
 * <ul>
 *   <li>只读模式下使用 {@code O_TRUNC} 或 {@code O_APPEND} 时抛错</li>
 *   <li>{@code O_TRUNC} 与 {@code O_APPEND} 同时设置时抛错</li>
 *   <li>{@code O_EXCL} 未与 {@code O_CREAT} 配合时不报错（但无效）</li>
 * </ul>
 */
public final class OpenFlags {

    private OpenFlags() {
    }

    /**
     * 只读访问
     */
    public static final int O_RDONLY = 0x0;
    /**
     * 只写访问
     */
    public static final int O_WRONLY = 0x1;
    /**
     * 读写访问
     */
    public static final int O_RDWR = 0x2;

    /**
     * 文件不存在时创建
     */
    public static final int O_CREAT = 0x40;
    /**
     * 仅当文件不存在时创建（必须与 O_CREAT 配合）
     */
    public static final int O_EXCL = 0x80;
    /**
     * 打开时截断为 0
     */
    public static final int O_TRUNC = 0x200;
    /**
     * 追加写
     */
    public static final int O_APPEND = 0x400;

    /**
     * 判断 flags 是否包含指定标志位。
     *
     * @param flags 全部标志的 bitmask
     * @param f     需判断的标志位
     * @return 如果包含该标志位则为 true，否则为 false
     */
    private static boolean has(int flags, int f) {
        return (flags & f) != 0;
    }

    /**
     * 将 Snow VM 的 open 标志转换为 Java NIO 的 {@link OpenOption} 集合。
     *
     * @param flags 标志位 bitmask
     * @return 对应的 {@link OpenOption} 集合
     */
    public static Set<OpenOption> toOpenOptions(int flags) {
        Set<OpenOption> opts = new HashSet<>();

        int acc = (flags & 0x3);
        switch (acc) {
            case O_WRONLY -> opts.add(WRITE);
            case O_RDWR -> {
                opts.add(READ);
                opts.add(WRITE);
            }
            default -> opts.add(READ);
        }

        if (has(flags, O_CREAT) && has(flags, O_EXCL)) {
            opts.add(CREATE_NEW);
        } else if (has(flags, O_CREAT)) {
            opts.add(CREATE);
        }

        if (has(flags, O_TRUNC)) {
            opts.add(TRUNCATE_EXISTING);
        }

        if (has(flags, O_APPEND)) {
            opts.add(APPEND);
        }

        return opts;
    }

    /**
     * 校验标志位组合的基本一致性，发现明显冲突时抛出异常。
     * <ul>
     *   <li>只读模式下禁止 {@code O_TRUNC}/{@code O_APPEND}</li>
     *   <li>{@code O_TRUNC} 与 {@code O_APPEND} 不能同时设置</li>
     *   <li>{@code O_EXCL} 未与 {@code O_CREAT} 配合不报错，但无实际意义</li>
     * </ul>
     *
     * @param flags open 标志位
     * @throws IllegalArgumentException 如果组合冲突
     */
    public static void validate(int flags) {
        int acc = (flags & 0x3);
        boolean write = (acc == O_WRONLY || acc == O_RDWR);

        if (!write && (has(flags, O_TRUNC) || has(flags, O_APPEND))) {
            throw new IllegalArgumentException("O_TRUNC/O_APPEND 需要写权限（O_WRONLY 或 O_RDWR）");
        }
        if (has(flags, O_TRUNC) && has(flags, O_APPEND)) {
            throw new IllegalArgumentException("O_TRUNC 与 O_APPEND 不能同时使用");
        }
        // O_EXCL 未与 O_CREAT 配合时无实际意义，这里不抛错
    }
}
