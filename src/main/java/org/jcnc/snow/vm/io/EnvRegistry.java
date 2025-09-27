package org.jcnc.snow.vm.io;

import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code EnvRegistry} —— 进程级环境变量覆盖层，
 * 提供 VM 内可写环境变量接口，线程安全，作用于同一 JVM 进程下的所有 VM 实例。
 *
 * <p>
 * 用于在 Snow VM 内部维护一份“可写”的环境变量视图：
 * <ul>
 *   <li>优先返回通过 {@link #set(String, String, int)} 写入的变量值</li>
 *   <li>若未被覆盖，则回退查找 {@link System#getenv(String)}</li>
 *   <li>若变量被标记为删除（val == null），读取时返回 null</li>
 *   <li>所有方法线程安全</li>
 *   <li>JVM 不支持真正修改全局环境变量，本组件仅在 VM 内部模拟一致语义</li>
 * </ul>
 * 主要用于：
 * <ul>
 *   <li>VM 内部 getenv/setenv 系统调用一致性</li>
 *   <li>fork/exec 等子进程创建时环境快照传递</li>
 * </ul>
 * </p>
 */
public final class EnvRegistry {
    private EnvRegistry() {
    }

    /**
     * 内部哨兵值，用于表达“被删除”，ConcurrentHashMap 不支持 null value。
     */
    private static final String DELETED = "\u0000__SNOW_ENV_DELETED__";

    /**
     * 覆盖表（线程安全），key 使用系统环境变量大小写（原样）。
     */
    private static final ConcurrentHashMap<String, String> overlay = new ConcurrentHashMap<>();

    /**
     * 获取指定环境变量的值。
     *
     * <p>
     * <ul>
     *   <li>优先查找覆盖层，如存在且未被标记删除，直接返回</li>
     *   <li>如被标记删除，返回 null</li>
     *   <li>如覆盖层无记录，则回退至 System.getenv()</li>
     *   <li>key 为 null 返回 null</li>
     * </ul>
     * </p>
     *
     * @param key 环境变量名（null 时返回 null）
     * @return 当前值或 null（未设置/已删除/不存在）
     */
    public static String get(String key) {
        if (key == null) return null;
        String ov = overlay.get(key);
        if (ov != null) {
            return DELETED.equals(ov) ? null : ov;
        }
        return System.getenv(key); // 可能为 null
    }

    /**
     * 设置或删除指定环境变量（可选覆盖）。
     *
     * <p>
     * <ul>
     *   <li>key 不可为 null/空</li>
     *   <li>val 为 null 表示“删除”变量（覆盖层用哨兵值）</li>
     *   <li>overwrite=1 时允许覆盖已有（包括系统环境或覆盖层）</li>
     *   <li>overwrite=0 且已存在则不变</li>
     * </ul>
     * </p>
     *
     * @param key       变量名（不可为 null/空）
     * @param val       新值；null 表示删除
     * @param overwrite 允许覆盖已有值（1 覆盖，0 不覆盖）
     * @return 总是返回 0
     * @throws IllegalArgumentException key 为 null 或空
     */
    public static int set(String key, String val, int overwrite) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("env key cannot be null/empty");
        }
        boolean allowOverwrite = overwrite != 0;

        // 是否已存在（覆盖层 or 系统环境）
        boolean exists = overlay.containsKey(key) || System.getenv(key) != null;

        if (!allowOverwrite && exists) {
            // 不允许覆盖：直接返回
            return 0;
        }

        overlay.put(key, Objects.requireNonNullElse(val, DELETED));
        return 0;
    }

    /**
     * 生成子进程友好的环境变量快照。
     *
     * <p>
     * 先深拷贝 System.getenv()，再应用当前 overlay 层（增、改、删皆生效）。
     * 适用于 {@code ProcessBuilder.environment()} 传递。
     * </p>
     *
     * @return 包含所有有效（未删除）变量的 Map 快照
     */
    public static Map<String, String> snapshot() {
        Map<String, String> m = new HashMap<>(System.getenv());
        for (Map.Entry<String, String> e : overlay.entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            if (DELETED.equals(v)) m.remove(k);
            else m.put(k, v);
        }
        return m;
    }
}
