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
 * CLI 命令：根据 project.cloud 生成项目目录结构。
 *
 * <pre>
 * $ snow generate
 * </pre>
 *
 * - 若当前目录不存在 project.cloud，则提示先执行 snow init。
 * - 成功后会在控制台输出已创建的目录/文件列表。
 */
public final class GenerateCommand implements CLICommand {

    @Override
    public String name() {
        return "generate";
    }

    @Override
    public String description() {
        return "Generate project directory structure based on project.cloud.";
    }

    @Override
    public void printUsage() {
        System.out.println("Usage: snow generate");
    }

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
