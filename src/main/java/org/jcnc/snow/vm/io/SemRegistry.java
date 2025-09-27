package org.jcnc.snow.vm.io;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code SemRegistry} 用于维护 VM 级别的信号量（semaphore），
 * 提供信号量 ID 与 {@link Semaphore} 对象之间的映射。
 *
 * <p><b>功能：</b></p>
 * <ul>
 *   <li>创建新的信号量并分配唯一的 ID</li>
 *   <li>根据 ID 查找已注册的信号量</li>
 *   <li>移除并注销指定 ID 的信号量</li>
 * </ul>
 *
 * <p><b>实现细节：</b></p>
 * <ul>
 *   <li>信号量 ID 从 {@code 1} 开始，自增分配</li>
 *   <li>内部使用 {@link ConcurrentHashMap} 存储 ID 与 {@link Semaphore} 的映射</li>
 *   <li>线程安全，适用于 VM 内部的并发控制</li>
 *   <li>典型使用场景：系统调用 SEM_WAIT、SEM_POST、SEM_TRYWAIT 等</li>
 * </ul>
 *
 * <p>该类为工具类，构造方法私有化，不可实例化。</p>
 */
public final class SemRegistry {
    /**
     * 存储信号量 ID 到 {@link Semaphore} 的映射表
     */
    private static final ConcurrentHashMap<Integer, Semaphore> REG = new ConcurrentHashMap<>();

    /**
     * 自增生成信号量 ID，初始值为 1
     */
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    /**
     * 私有构造方法，禁止外部实例化
     */
    private SemRegistry() {
    }

    /**
     * 创建并注册一个新的信号量，并返回其唯一 ID。
     *
     * @param init 信号量的初始许可数（必须大于等于 0）
     * @return 新信号量的 ID（int）
     * @throws IllegalArgumentException 如果初始值小于 0
     */
    public static int create(int init) {
        if (init < 0) {
            throw new IllegalArgumentException("Semaphore init must be >= 0");
        }
        int id = NEXT_ID.getAndIncrement();
        REG.put(id, new Semaphore(init));
        return id;
    }

    /**
     * 根据 ID 获取已注册的信号量。
     *
     * @param id 信号量 ID（int）
     * @return 对应的 {@link Semaphore} 实例
     * @throws IllegalArgumentException 如果指定 ID 不存在
     */
    public static Semaphore get(int id) {
        Semaphore sem = REG.get(id);
        if (sem == null) {
            throw new IllegalArgumentException("Invalid semaphore id: " + id);
        }
        return sem;
    }

    /**
     * 移除并注销指定 ID 的信号量。
     *
     * @param id 信号量 ID（int）
     */
    public static void remove(int id) {
        REG.remove(id);
    }
}
