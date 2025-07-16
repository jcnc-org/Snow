package org.jcnc.snow.cli.commands;

import org.jcnc.snow.cli.api.CLICommand;
import org.jcnc.snow.pkg.dsl.CloudDSLParser;
import org.jcnc.snow.pkg.lifecycle.LifecycleManager;
import org.jcnc.snow.pkg.lifecycle.LifecyclePhase;
import org.jcnc.snow.pkg.model.Project;
import org.jcnc.snow.pkg.tasks.GenerateTask;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * CLI 命令: 根据 project.cloud 生成项目目录结构。
 * <p>
 * 负责解析云项目描述文件，并通过 {@link GenerateTask}
 * 在 INIT 生命周期阶段内生成基础目录结构。
 * </p>
 *
 * <pre>
 * 用法示例:
 * $ snow generate
 * </pre>
 *
 * <p>
 * 注意事项:
 * - 若当前目录不存在 project.cloud，则提示用户先执行 `snow init`。
 * - 执行成功后，会输出已创建的目录/文件。
 * </p>
 */
public final class GenerateCommand implements CLICommand {

    /**
     * 返回命令名称，用于 CLI 调用。
     *
     * @return 命令名称，如 "generate"
     */
    @Override
    public String name() {
        return "generate";
    }

    /**
     * 返回命令简介，用于 CLI 帮助或命令列表展示。
     *
     * @return 命令描述字符串
     */
    @Override
    public String description() {
        return "Generate project directory structure based on project.cloud.";
    }

    /**
     * 打印命令用法信息。
     */
    @Override
    public void printUsage() {
        System.out.println("Usage: snow generate");
    }

    /**
     * 执行生成任务。
     *
     * @param args CLI 传入的参数数组（此命令不接受参数）
     * @return 执行结果码（0 表示成功，1 表示 project.cloud 缺失）
     * @throws Exception 执行过程中出现错误时抛出
     */
    @Override
    public int execute(String[] args) throws Exception {
        Path dsl = Paths.get("project.cloud");
        if (Files.notExists(dsl)) {
            System.err.println("project.cloud not found. Please run `snow init` first.");
            return 1;
        }

        /* 1. 解析 DSL */
        Project project = CloudDSLParser.parse(dsl);

        /* 2. 执行生成任务 —— 复用 Lifecycle INIT 阶段 */
        LifecycleManager lm = new LifecycleManager();
        lm.register(LifecyclePhase.INIT, new GenerateTask(project));
        lm.executeAll();

        return 0;
    }
}
