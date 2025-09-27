package org.jcnc.snow.vm.io;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * {@code RwlockRegistry} 用于维护 VM 级别的读写锁（rwlock），
 * 提供读写锁 ID 与 {@link ReentrantReadWriteLock} 对象之间的映射。
 *
 * <p><b>功能：</b></p>
 * <ul>
 *   <li>创建新的读写锁并分配唯一的 ID</li>
 *   <li>根据 ID 查找已注册的读写锁</li>
 *   <li>移除并注销指定 ID 的读写锁</li>
 * </ul>
 *
 * <p><b>实现细节：</b></p>
 * <ul>
 *   <li>读写锁 ID 从 {@code 1} 开始，自增分配</li>
 *   <li>内部使用 {@link ConcurrentHashMap} 存储 ID 与 {@link ReentrantReadWriteLock} 的映射</li>
 *   <li>线程安全，适用于 VM 内部的并发控制</li>
 *   <li>典型使用场景：系统调用 RWLOCK_RDLOCK、RWLOCK_WRLOCK、RWLOCK_UNLOCK 等</li>
 * </ul>
 *
 * <p>该类为工具类，构造方法私有化，不可实例化。</p>
 */
public final class RwlockRegistry {
    /**
     * 存储读写锁 ID 到 {@link ReentrantReadWriteLock} 的映射表
     */
    private static final ConcurrentHashMap<Integer, ReentrantReadWriteLock> REG = new ConcurrentHashMap<>();

    /**
     * 自增生成读写锁 ID，初始值为 1
     */
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    /**
     * 私有构造方法，禁止外部实例化
     */
    private RwlockRegistry() {
    }

    /**
     * 创建并注册一个新的读写锁，并返回其唯一 ID。
     *
     * @return 新读写锁的 ID（int）
     *
     * <p><b>示例：</b></p>
     * <pre>{@code
     * int rid = RwlockRegistry.create();
     * }</pre>
     */
    public static int create() {
        int id = NEXT_ID.getAndIncrement();
        REG.put(id, new ReentrantReadWriteLock());
        return id;
    }

    /**
     * 根据 ID 获取已注册的读写锁。
     *
     * @param id 读写锁 ID（int）
     * @return 对应的 {@link ReentrantReadWriteLock} 实例
     * @throws IllegalArgumentException 如果指定 ID 不存在
     */
    public static ReentrantReadWriteLock get(int id) {
        ReentrantReadWriteLock rwl = REG.get(id);
        if (rwl == null) {
            throw new IllegalArgumentException("Invalid rwlock id: " + id);
        }
        return rwl;
    }

    /**
     * 移除并注销指定 ID 的读写锁。
     *
     * @param id 读写锁 ID（int）
     */
    public static void remove(int id) {
        REG.remove(id);
    }
}
