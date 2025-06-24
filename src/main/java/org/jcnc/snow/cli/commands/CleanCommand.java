package org.jcnc.snow.cli.commands;

import org.jcnc.snow.pkg.lifecycle.LifecycleManager;
import org.jcnc.snow.pkg.lifecycle.LifecyclePhase;
import org.jcnc.snow.pkg.tasks.CleanTask;

/**
 * CLI 命令：清理构建输出和本地缓存目录。
 * <p>
 * 用于清除项目生成的 build、dist 等中间产物，保持工作区整洁。
 * </p>
 *
 * <pre>
 * 用法示例：
 * $ snow clean
 * </pre>
 */
public final class CleanCommand implements CLICommand {

    /**
     * 返回命令名称，用于 CLI 调用。
     *
     * @return 命令名称，如 "clean"
     */
    @Override
    public String name() {
        return "clean";
    }

    /**
     * 返回命令简介，用于 CLI 帮助或命令列表展示。
     *
     * @return 命令描述字符串
     */
    @Override
    public String description() {
        return "Clean build outputs and local cache, remove intermediate artifacts, and free disk space.";
    }

    /**
     * 打印命令用法信息。
     */
    @Override
    public void printUsage() {
        System.out.println("Usage: snow clean ");
    }

    /**
     * 执行清理任务。
     *
     * @param args CLI 传入的参数数组
     * @return 执行结果码（0 表示成功）
     * @throws Exception 执行过程中出现错误时抛出
     */
    @Override
    public int execute(String[] args) throws Exception {
        LifecycleManager lm = new LifecycleManager();
        lm.register(LifecyclePhase.CLEAN, new CleanTask());
        lm.executeAll();
        return 0;
    }
}
