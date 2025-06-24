package org.jcnc.snow.cli.commands;

import org.jcnc.snow.cli.api.CLICommand;
import org.jcnc.snow.pkg.dsl.CloudDSLParser;
import org.jcnc.snow.pkg.lifecycle.LifecycleManager;
import org.jcnc.snow.pkg.lifecycle.LifecyclePhase;
import org.jcnc.snow.pkg.model.Project;
import org.jcnc.snow.pkg.resolver.DependencyResolver;
import org.jcnc.snow.pkg.tasks.CompileTask;
import org.jcnc.snow.pkg.tasks.PackageTask;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * CLI 命令：构建当前项目（包含依赖解析、编译、打包）。
 * <p>
 * 该命令会依次执行依赖解析、源码编译和产物打包阶段。
 * </p>
 *
 * <pre>
 * 用法示例：
 * $ snow build
 * </pre>
 */
public final class BuildCommand implements CLICommand {

    /**
     * 返回命令名称，用于 CLI 调用。
     *
     * @return 命令名称，如 "build"
     */
    @Override
    public String name() {
        return "build";
    }

    /**
     * 返回命令简介，用于 CLI 帮助或命令列表展示。
     *
     * @return 命令描述字符串
     */
    @Override
    public String description() {
        return "Build the current project by resolving dependencies, compiling, and packaging in sequence.";
    }

    /**
     * 打印命令用法信息。
     */
    @Override
    public void printUsage() {
        System.out.println("Usage: snow build ");
    }

    /**
     * 执行项目构建流程。
     * <ul>
     *     <li>解析项目描述文件（project.cloud）</li>
     *     <li>依赖解析（RESOLVE_DEPENDENCIES）</li>
     *     <li>源码编译（COMPILE）</li>
     *     <li>产物打包（PACKAGE）</li>
     * </ul>
     *
     * @param args CLI 传入的参数数组
     * @return 执行结果码（0 表示成功）
     * @throws Exception 执行过程中出现错误时抛出
     */
    @Override
    public int execute(String[] args) throws Exception {

        Path dslFile = Paths.get("project.cloud");
        Project project = CloudDSLParser.parse(dslFile);
        DependencyResolver resolver = new DependencyResolver(Paths.get(System.getProperty("user.home"), ".snow", "cache"));
        LifecycleManager lm = new LifecycleManager();

        // 注册各阶段任务
        lm.register(LifecyclePhase.RESOLVE_DEPENDENCIES, () -> resolver.resolve(project));
        lm.register(LifecyclePhase.COMPILE, new CompileTask(project));
        lm.register(LifecyclePhase.PACKAGE, new PackageTask(project));

        lm.executeAll();

        return 0;
    }
}
