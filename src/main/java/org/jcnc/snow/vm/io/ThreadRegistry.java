package org.jcnc.snow.vm.io;

import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code ThreadRegistry} 管理 VM 内部线程对象与其 ID 的注册与返回值缓存。
 *
 * <p><b>功能：</b>
 * <ul>
 *   <li>注册/注销线程实例（以 threadId 作为唯一键）</li>
 *   <li>按 tid 查询 Thread 对象</li>
 *   <li>为线程存取/移除返回值（可为 null，null 时自动移除）</li>
 * </ul>
 * </p>
 *
 * <p>底层采用线程安全的 {@link ConcurrentHashMap} 实现。</p>
 */
public class ThreadRegistry {

    /**
     * 线程ID → Thread 对象映射表
     */
    private static final ConcurrentHashMap<Long, Thread> threads = new ConcurrentHashMap<>();
    /**
     * 线程ID → 线程返回值映射表
     */
    private static final ConcurrentHashMap<Long, Object> results = new ConcurrentHashMap<>();

    /**
     * 注册一个线程对象到注册表。
     *
     * @param t 需注册的线程对象
     */
    public static void register(Thread t) {
        threads.put(t.threadId(), t);
    }

    /**
     * 获取指定 tid 对应的线程对象。
     *
     * @param tid 线程 ID
     * @return Thread 对象，未注册时返回 null
     */
    public static Thread get(long tid) {
        return threads.get(tid);
    }

    /**
     * 注销指定 tid 的线程与其返回值缓存。
     *
     * @param tid 线程 ID
     */
    public static void unregister(long tid) {
        threads.remove(tid);
        results.remove(tid);
    }

    /**
     * 设置线程的返回值。<br>
     * 注意 ConcurrentHashMap 不允许存储 null；
     * 若 result 为 null，则自动移除 tid 的返回值（等价于“无返回值”）。
     *
     * @param tid    线程 ID
     * @param result 线程执行结果，可为任意对象/null
     */
    public static void setResult(long tid, Object result) {
        if (result == null) {
            results.remove(tid);
        } else {
            results.put(tid, result);
        }
    }

    /**
     * 获取线程的返回值（如未设置或已移除则返回 null）。
     *
     * @param tid 线程 ID
     * @return 线程返回值对象/null
     */
    public static Object getResult(long tid) {
        return results.get(tid);
    }
}
