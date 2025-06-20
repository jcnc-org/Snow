package org.jcnc.snow.pkg.tasks;

/**
 * 构建任务的通用接口，所有具体任务都应实现该接口。
 * <p>
 * 用于统一生命周期内的任务行为，例如编译、打包、清理等。
 * </p>
 */
public interface Task {

    /**
     * 执行具体任务的入口方法。
     *
     * @throws Exception 任务执行过程中出现的异常
     */
    void run() throws Exception;
}
