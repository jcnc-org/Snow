package org.jcnc.snow.cli.commands;

import org.jcnc.snow.cli.api.CLICommand;
import org.jcnc.snow.cli.utils.ProjectCloudExample;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * CLI 命令: 初始化项目配置文件。
 * <p>
 * 用于快速生成 DSL 配置文件（project.cloud）。
 * </p>
 *
 * <pre>
 * 用法示例:
 * $ snow init
 * </pre>
 */
public final class InitCommand implements CLICommand {

    /**
     * 返回命令名称，用于 CLI 调用。
     *
     * @return 命令名称，如 "init"
     */
    @Override
    public String name() {
        return "init";
    }

    /**
     * 返回命令简介，用于 CLI 帮助或命令列表展示。
     *
     * @return 命令描述字符串
     */
    @Override
    public String description() {
        return "Initialize a new project with project.cloud file.";
    }

    /**
     * 打印命令用法信息。
     */
    @Override
    public void printUsage() {
        System.out.println("Usage: snow init");
    }

    /**
     * 执行项目初始化流程，仅创建 DSL 配置文件。
     *
     * @param args CLI 传入的参数数组
     * @return 执行结果码（0 表示成功）
     * @throws Exception 文件创建过程中出现错误时抛出
     */
    @Override
    public int execute(String[] args) throws Exception {
        // 仅生成 `.cloud` 文件
        Path dir = Paths.get(".").toAbsolutePath();
        Path dsl = dir.resolve("project.cloud");
        if (Files.notExists(dsl)) {
            Files.writeString(dsl, ProjectCloudExample.getProjectCloud());
            System.out.println("[init] created " + dsl);
        } else {
            System.out.println("[init] project.cloud already exists");
        }
        return 0;
    }
}
