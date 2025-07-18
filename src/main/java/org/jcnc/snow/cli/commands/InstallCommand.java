package org.jcnc.snow.cli.commands;

import org.jcnc.snow.cli.api.CLICommand;
import org.jcnc.snow.pkg.dsl.CloudDSLParser;
import org.jcnc.snow.pkg.model.Project;
import org.jcnc.snow.pkg.resolver.DependencyResolver;

import java.nio.file.Paths;

/**
 * CLI 命令: 解析并下载项目依赖到本地缓存。
 * <p>
 * 适用于离线使用和依赖预热场景，会自动读取项目描述文件并处理依赖缓存。
 * </p>
 *
 * <pre>
 * 用法示例: 
 * $ snow install
 * </pre>
 */
public final class InstallCommand implements CLICommand {

    /**
     * 返回命令名称，用于 CLI 调用。
     *
     * @return 命令名称，如 "install"
     */
    @Override
    public String name() {
        return "install";
    }

    /**
     * 返回命令简介，用于 CLI 帮助或命令列表展示。
     *
     * @return 命令描述字符串
     */
    @Override
    public String description() {
        return "Resolve and download project dependencies to local cache for offline development or faster builds.";
    }

    /**
     * 打印命令用法信息。
     */
    @Override
    public void printUsage() {
        System.out.println("Usage: snow install ");
    }

    /**
     * 执行依赖解析和下载任务。
     *
     * @param args CLI 传入的参数数组
     * @return 执行结果码（0 表示成功）
     * @throws Exception 解析或下载依赖过程中出现错误时抛出
     */
    @Override
    public int execute(String[] args) throws Exception {
        Project project = CloudDSLParser.parse(Paths.get("project.cloud"));
        DependencyResolver resolver = new DependencyResolver(Paths.get(System.getProperty("user.home"), ".snow", "cache"));
        resolver.resolve(project);
        return 0;
    }
}
