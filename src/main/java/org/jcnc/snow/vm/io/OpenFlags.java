package org.jcnc.snow.vm.io;

import java.nio.file.OpenOption;
import java.util.HashSet;
import java.util.Set;

import static java.nio.file.StandardOpenOption.*;

/**
 * {@code OpenFlags} 定义了系统调用 {@code OPEN} 所使用的标志位，
 * 并提供辅助方法将这些标志转换为 Java NIO 的 {@link OpenOption} 集合。
 *
 * <p>
 * 这些标志与 POSIX 风格保持一致（取值范围及含义参考 UNIX 的 open 系统调用），
 * 在虚拟机内部用于描述文件的访问模式和创建语义。
 * </p>
 *
 * <p>常量定义：</p>
 * <ul>
 *   <li>{@code O_RDONLY = 0}：只读（默认）</li>
 *   <li>{@code O_WRONLY = 1}：只写</li>
 *   <li>{@code O_RDWR   = 2}：读写</li>
 *   <li>{@code O_CREAT  = 0x40}：文件不存在时创建</li>
 *   <li>{@code O_EXCL   = 0x80}：与 O_CREAT 同用，若文件已存在则失败</li>
 *   <li>{@code O_TRUNC  = 0x200}：打开时将文件长度截断为 0</li>
 *   <li>{@code O_APPEND = 0x400}：追加写，每次写入都追加到末尾</li>
 * </ul>
 *
 * <p>工具方法：</p>
 * <ul>
 *   <li>{@link #has(int, int)}：判断某一标志位是否被设置</li>
 *   <li>{@link #toOpenOptions(int)}：
 *       将 flags 翻译为 Java 的 {@link OpenOption} 集合，
 *       供 {@link java.nio.file.Files#newByteChannel} 使用</li>
 *   <li>{@link #validate(int)}：
 *       对 flags 组合进行基本一致性校验，
 *       发现明显冲突时抛出 {@link IllegalArgumentException}</li>
 * </ul>
 *
 * <p>一致性校验示例：</p>
 * <ul>
 *   <li>如果是只读模式，却指定了 {@code O_TRUNC} 或 {@code O_APPEND} → 抛错</li>
 *   <li>如果同时指定了 {@code O_TRUNC} 和 {@code O_APPEND} → 抛错</li>
 *   <li>{@code O_EXCL} 未与 {@code O_CREAT} 同用时无实际意义（但默认不抛错）</li>
 * </ul>
 */
public final class OpenFlags {
    // 访问模式（通常占用低两位）
    public static final int O_RDONLY = 0;       // 只读（默认）
    public static final int O_WRONLY = 1;       // 只写
    public static final int O_RDWR   = 2;       // 读写

    // 语义控制位
    public static final int O_CREAT  = 0x40;    // 不存在则创建
    public static final int O_EXCL   = 0x80;    // 与 O_CREAT 配合，存在则失败
    public static final int O_TRUNC  = 0x200;   // 打开时将文件截断为 0
    public static final int O_APPEND = 0x400;   // 追加写

    private OpenFlags() {} // 工具类，禁止实例化

    /** 某一位是否被设置 */
    public static boolean has(int flags, int bit) {
        return (flags & bit) != 0;
    }

    /** 将 flags 翻译为 Java 的 OpenOption 集合（供 Files.newByteChannel 使用） */
    public static Set<OpenOption> toOpenOptions(int flags) {
        // 使用 HashSet<OpenOption>，避免 EnumSet<StandardOpenOption> 的泛型不匹配
        Set<OpenOption> opts = new HashSet<>();

        // 访问模式
        int acc = (flags & 0x3);
        if (acc == O_WRONLY) {
            opts.add(WRITE);
        } else if (acc == O_RDWR) {
            opts.add(READ);
            opts.add(WRITE);
        } else {
            // 默认或未识别 -> READ
            opts.add(READ);
        }

        // 创建 / 独占 / 截断 / 追加
        if (has(flags, O_EXCL)) {
            opts.add(CREATE_NEW);        // 已存在则失败
        } else if (has(flags, O_CREAT)) {
            opts.add(CREATE);            // 不存在则创建
        }
        if (has(flags, O_TRUNC)) {
            opts.add(TRUNCATE_EXISTING); // 若文件存在则清空
        }
        if (has(flags, O_APPEND)) {
            opts.add(APPEND);            // 末尾追加
        }
        return opts;
    }

    /** 基本一致性校验（发现明显冲突时抛错） */
    public static void validate(int flags) {
        int acc = (flags & 0x3);
        boolean write = (acc == O_WRONLY || acc == O_RDWR);

        if (!write && (has(flags, O_TRUNC) || has(flags, O_APPEND))) {
            throw new IllegalArgumentException("O_TRUNC/O_APPEND 需要写权限（O_WRONLY 或 O_RDWR）");
        }
        if (has(flags, O_TRUNC) && has(flags, O_APPEND)) {
            throw new IllegalArgumentException("O_TRUNC 与 O_APPEND 不能同时使用");
        }
        // O_EXCL 未与 O_CREAT 同用时通常无意义，如需更严格可在此抛错
    }
}
