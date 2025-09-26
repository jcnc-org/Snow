package org.jcnc.snow.vm.io;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code CondRegistry} 维护 VM 条件变量 ID 到内部监视器对象的映射。
 *
 * <p><b>语义：</b>
 * 提供条件变量（condition）的分配、查找、移除等操作。条件变量本身是一个独立 monitor 对象，通常与外部互斥量（mutex）配合，
 * 通过 synchronized(monitor)+wait/notifyAll 等原语实现条件等待与唤醒。
 * </p>
 *
 * <p><b>实现说明：</b>
 * <ul>
 *   <li>每个条件变量分配唯一 int ID，内部用 {@link ConcurrentHashMap} 线程安全维护。</li>
 *   <li>兼容多种 mutex，不依赖于 {@link java.util.concurrent.locks.Condition}。</li>
 * </ul>
 * </p>
 */
public final class CondRegistry {
    private static final ConcurrentHashMap<Integer, Object> REG = new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private CondRegistry() {
    }

    /**
     * 创建并注册一个新的条件变量，返回其 id。
     *
     * <p><b>返回：</b> 新分配的条件变量 id（int）。</p>
     */
    public static int create() {
        int id = NEXT_ID.getAndIncrement();
        REG.put(id, new Object());
        return id;
    }

    /**
     * 获取指定 id 的条件变量监视器对象。
     *
     * <p><b>参数：</b> id 条件变量 id（int）</p>
     * <p><b>返回：</b> 监视器对象（Object）</p>
     * <p><b>异常：</b> id 无效时抛出 {@link IllegalArgumentException}</p>
     */
    public static Object get(int id) {
        Object mon = REG.get(id);
        if (mon == null) {
            throw new IllegalArgumentException("Invalid condition id: " + id);
        }
        return mon;
    }

    /**
     * 移除条件变量。
     *
     * <p><b>参数：</b> id 条件变量 id（int）</p>
     */
    public static void remove(int id) {
        REG.remove(id);
    }
}
