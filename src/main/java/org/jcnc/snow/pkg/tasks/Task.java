package org.jcnc.snow.pkg.tasks;

/**
 * Task 是所有构建任务（如编译、打包、清理、发布等）的统一接口。
 * <p>
 * 用于约定项目自动化流程中各阶段任务的生命周期与执行行为，便于任务链式调用与扩展。
 * </p>
 * <ul>
 *   <li>所有具体任务类（如 CompileTask、CleanTask 等）都应实现本接口</li>
 *   <li>支持 CLI、IDE 插件、自动化脚本等多种宿主环境统一调用</li>
 * </ul>
 */
public interface Task {

    /**
     * 执行具体任务的入口方法。
     *
     * @throws Exception 任务执行过程中发生的任意异常
     */
    void run() throws Exception;
}
