package org.jcnc.snow.cli.commands;

import org.jcnc.snow.cli.api.CLICommand;
import org.jcnc.snow.pkg.dsl.CloudDSLParser;
import org.jcnc.snow.pkg.lifecycle.LifecycleManager;
import org.jcnc.snow.pkg.lifecycle.LifecyclePhase;
import org.jcnc.snow.pkg.model.Project;
import org.jcnc.snow.pkg.tasks.PublishTask;

import java.nio.file.Paths;

/**
 * CLI 命令: 将已构建的项目包发布到远程仓库。
 * <p>
 * 用于持续集成、交付或分发场景。
 * 支持自动读取 DSL 项目描述文件，并注册和执行发布生命周期阶段的任务。
 * </p>
 *
 * <pre>
 * 用法示例: 
 * $ snow publish
 * </pre>
 */
public final class PublishCommand implements CLICommand {

    /**
     * 返回该命令的名称（用于 CLI 调用）。
     *
     * @return 命令名称字符串，如 "publish"
     */
    @Override
    public String name() {
        return "publish";
    }

    /**
     * 返回命令简介，用于 CLI 帮助或命令列表展示。
     *
     * @return 命令描述字符串
     */
    @Override
    public String description() {
        return "Publish the built package to a remote repository, suitable for continuous integration, delivery, or project distribution.";
    }

    /**
     * 打印命令用法信息，供终端用户参考。
     */
    @Override
    public void printUsage() {
        System.out.println("Usage: snow publish ");
    }

    /**
     * 执行发布命令。
     * <ul>
     *   <li>解析项目描述文件（如 project.cloud）</li>
     *   <li>注册并执行 PUBLISH 阶段的任务</li>
     * </ul>
     *
     * @param args CLI 传入的参数数组
     * @return 执行结果码（0表示成功）
     * @throws Exception 执行过程中出现错误时抛出
     */
    @Override
    public int execute(String[] args) throws Exception {
        Project project = CloudDSLParser.parse(Paths.get("project.cloud"));
        LifecycleManager lm = new LifecycleManager();
        lm.register(LifecyclePhase.PUBLISH, new PublishTask(project));
        lm.executeAll();

        return 0;
    }
}
