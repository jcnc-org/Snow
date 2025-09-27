package org.jcnc.snow.vm.io;

import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code ProcessRegistry} 用于管理虚拟机中所有子进程的注册、查找和注销。
 *
 * <p><b>功能</b>：
 * <ul>
 *   <li>支持注册新进程（以 pid 为键）</li>
 *   <li>根据 pid 查找进程</li>
 *   <li>根据 pid 注销（移除）进程</li>
 *   <li>遍历当前所有已注册进程</li>
 * </ul>
 * </p>
 *
 * <p>内部使用线程安全的 {@link ConcurrentHashMap}。</p>
 */
public class ProcessRegistry {

    /**
     * pid → Process 的注册表（线程安全）
     */
    private static final ConcurrentHashMap<Long, Process> processes = new ConcurrentHashMap<>();

    /**
     * 注册一个进程。
     *
     * @param p 子进程对象
     */
    public static void register(Process p) {
        processes.put(p.pid(), p);
    }

    /**
     * 获取指定 pid 的进程。
     *
     * @param pid 进程 id
     * @return 进程对象（未找到返回 null）
     */
    public static Process get(long pid) {
        return processes.get(pid);
    }

    /**
     * 注销（移除）指定 pid 的进程。
     *
     * @param pid 进程 id
     */
    public static void unregister(long pid) {
        processes.remove(pid);
    }

    /**
     * 返回所有已注册的进程对象集合。
     *
     * @return 所有进程的可迭代视图
     */
    public static Iterable<Process> all() {
        return processes.values();
    }
}
