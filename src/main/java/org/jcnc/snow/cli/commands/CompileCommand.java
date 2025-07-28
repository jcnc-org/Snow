package org.jcnc.snow.cli.commands;

import org.jcnc.snow.cli.api.CLICommand;
import org.jcnc.snow.pkg.dsl.CloudDSLParser;
import org.jcnc.snow.pkg.model.Project;
import org.jcnc.snow.pkg.tasks.CompileTask;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CLI 命令: 编译当前项目。
 *
 * <p>工作模式说明: </p>
 * <ul>
 *   <li><strong>Cloud 模式</strong>
 *       - 项目根目录存在 {@code project.cloud} 时触发；
 *       - 解析 build 区块，自动推导源码目录与输出文件名；
 *       - 用法: {@code snow compile [run]}</li>
 *   <li><strong>Local 模式</strong>
 *       - 未检测到 {@code project.cloud} 时回退；
 *       - 保持向后兼容: {@code snow compile [run] [-o <name>] [-d <srcDir>] [file.snow …]}</li>
 * </ul>
 *
 * <p>两种模式均将最终参数交由 {@link CompileTask} 处理。</p>
 */
public final class CompileCommand implements CLICommand {

    @Override
    public String name() {
        return "compile";
    }

    @Override
    public String description() {
        return "Compile .snow source files into VM byte-code (.water).";
    }

    @Override
    public void printUsage() {
        System.out.println("Usage:");
        System.out.println("  snow compile [run]                               (cloud mode, use project.cloud)");
        System.out.println("  snow compile [run] [-o <name>] [-d <srcDir>] [file1.snow …]  (GOPATH mode)");
    }

    @Override
    public int execute(String[] args) throws Exception {

        Path dslFile = Paths.get("project.cloud");
        Project project;
        String[] compileArgs;

        /* ---------- 1. Cloud 模式 ---------- */
        if (Files.exists(dslFile)) {
            project = CloudDSLParser.parse(dslFile);

            List<String> argList = new ArrayList<>();

            // 保留用户在 cloud 模式下传入的 “run” / “--debug” 标志
            for (String a : args) {
                if ("run".equals(a) || "--debug".equals(a)) {
                    argList.add(a);
                }
            }

            /* 源码目录: build.srcDir -> 默认 src */
            String srcDir = project.getBuild().get("srcDir", "src");
            argList.add("-d");
            argList.add(srcDir);

            /* 输出名称: build.output -> fallback to artifact */
            String output = project.getBuild().get("output", project.getArtifact());
            argList.add("-o");
            argList.add(output);

            compileArgs = argList.toArray(new String[0]);
        }
        /* ---------- 2. Local 模式 ---------- */
        else {
            project = Project.fromFlatMap(Collections.emptyMap()); // 占位项目，保持接口统一
            compileArgs = args;                                    // 透传原始 CLI 参数
        }

        // 委托给 CompileTask 完成实际编译/运行
        new CompileTask(project, compileArgs).run();
        return 0;
    }
}
