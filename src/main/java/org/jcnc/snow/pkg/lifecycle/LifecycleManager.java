package org.jcnc.snow.pkg.lifecycle;

import org.jcnc.snow.pkg.tasks.Task;

import java.util.EnumMap;
import java.util.Map;

/**
 * 管理不同生命周期阶段与其对应任务的工具类。
 * <p>
 * 可为每个 {@link LifecyclePhase} 注册对应的 {@link Task}，并按阶段顺序执行所有任务。
 * </p>
 *
 * <pre>
 * 示例用法：
 * LifecycleManager manager = new LifecycleManager();
 * manager.register(LifecyclePhase.INIT, new InitTask());
 * manager.executeAll();
 * </pre>
 */
public final class LifecycleManager {

    /**
     * 存储生命周期阶段与对应任务的映射关系。
     */
    private final Map<LifecyclePhase, Task> tasks = new EnumMap<>(LifecyclePhase.class);

    /**
     * 为指定生命周期阶段注册任务。
     * <p>若该阶段已有任务，则会被新任务覆盖。</p>
     *
     * @param phase 生命周期阶段，不能为空
     * @param task  对应任务，不能为空
     * @throws NullPointerException 若 phase 或 task 为 null
     */
    public void register(LifecyclePhase phase, Task task) {
        if (phase == null) {
            throw new NullPointerException("Lifecycle phase must not be null");
        }
        if (task == null) {
            throw new NullPointerException("Task must not be null");
        }
        tasks.put(phase, task);
    }

    /**
     * 按 {@link LifecyclePhase} 声明顺序依次执行所有已注册任务。
     * <p>
     * 未注册任务的阶段将被跳过。任务执行前会打印阶段名。
     * </p>
     *
     * @throws Exception 若某个任务执行时抛出异常，将直接抛出并终止后续任务执行
     */
    public void executeAll() throws Exception {
        for (LifecyclePhase phase : LifecyclePhase.values()) {
            Task task = tasks.get(phase);
            if (task != null) {
                System.out.println(">>> Phase: " + phase);
                task.run();
            }
        }
    }
}
