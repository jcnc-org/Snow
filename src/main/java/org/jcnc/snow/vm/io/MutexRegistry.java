package org.jcnc.snow.vm.io;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@code MutexRegistry} 用于维护 VM 级别的互斥量（mutex），
 * 提供互斥量 ID 与 {@link ReentrantLock} 对象之间的映射。
 *
 * <p><b>功能：</b></p>
 * <ul>
 *   <li>创建新的互斥量并分配唯一的 ID</li>
 *   <li>根据 ID 查找已注册的互斥量</li>
 *   <li>移除并注销指定 ID 的互斥量</li>
 * </ul>
 *
 * <p><b>实现细节：</b></p>
 * <ul>
 *   <li>互斥量 ID 从 {@code 1} 开始，自增分配</li>
 *   <li>内部使用 {@link ConcurrentHashMap} 存储 ID 与 {@link ReentrantLock} 的映射</li>
 *   <li>线程安全，适用于 VM 内部的并发控制</li>
 *   <li>典型使用场景：系统调用 COND_WAIT、MUTEX_LOCK、MUTEX_TRYLOCK、MUTEX_UNLOCK 等</li>
 * </ul>
 *
 * <p>该类为工具类，构造方法私有化，不可实例化。</p>
 */
public final class MutexRegistry {
    /** 存储互斥量 ID 到 {@link ReentrantLock} 的映射表 */
    private static final ConcurrentHashMap<Integer, ReentrantLock> REG = new ConcurrentHashMap<>();

    /** 自增生成互斥量 ID，初始值为 1 */
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    /** 私有构造方法，禁止外部实例化 */
    private MutexRegistry() { }

    /**
     * 创建并注册一个新的互斥量，并返回其唯一 ID。
     *
     * @return 新互斥量的 ID（int）
     */
    public static int create() {
        int id = NEXT_ID.getAndIncrement();
        REG.put(id, new ReentrantLock());
        return id;
    }

    /**
     * 根据 ID 获取已注册的互斥量。
     *
     * @param id 互斥量 ID（int）
     * @return 对应的 {@link ReentrantLock} 实例
     * @throws IllegalArgumentException 如果指定 ID 不存在
     */
    public static ReentrantLock get(int id) {
        ReentrantLock lock = REG.get(id);
        if (lock == null) {
            throw new IllegalArgumentException("Invalid mutex id: " + id);
        }
        return lock;
    }

    /**
     * 移除并注销指定 ID 的互斥量。
     *
     * @param id 互斥量 ID（int）
     *
     * <p><b>说明：</b></p>
     * <ul>
     *   <li>通常用于 VM 回收或测试场景</li>
     *   <li>若 ID 不存在，调用该方法不会抛出异常</li>
     * </ul>
     */
    public static void remove(int id) {
        REG.remove(id);
    }
}
