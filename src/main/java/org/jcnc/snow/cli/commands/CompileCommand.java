package org.jcnc.snow.cli.commands;

import org.jcnc.snow.cli.api.CLICommand;
import org.jcnc.snow.pkg.dsl.CloudDSLParser;
import org.jcnc.snow.pkg.model.Project;
import org.jcnc.snow.pkg.tasks.CompileTask;

import java.nio.file.Paths;

/**
 * CLI 命令：编译当前项目。
 * <p>
 * 负责读取项目描述文件并委托给 {@link CompileTask}，
 * 将 .snow 文件编译为 .water 字节码。
 * </p>
 *
 * <pre>
 * 用法示例：
 * $ snow compile -o main -d src/
 * $ snow compile run entry.snow
 * </pre>
 */
public final class CompileCommand implements CLICommand {

    /**
     * 返回命令名称，用于 CLI 调用。
     *
     * @return 命令名称，如 "compile"
     */
    @Override
    public String name() {
        return "compile";
    }

    /**
     * 返回命令简介，用于 CLI 帮助或命令列表展示。
     *
     * @return 命令描述字符串
     */
    @Override
    public String description() {
        return "Compile .snow source files into VM byte-code (.water).";
    }

    /**
     * 打印命令用法信息。
     */
    @Override
    public void printUsage() {
        System.out.println("Usage:");
        System.out.println("  snow compile [run] [-o <name>] [-d <srcDir>] [file1.snow file2.snow …]");
        System.out.println("Options:");
        System.out.println("  run           compile then run");
        System.out.println("  -o <name>     specify output base name (without .water suffix)");
        System.out.println("  -d <srcDir>   recursively compile all .snow files in directory");
    }

    /**
     * 执行编译任务。
     *
     * @param args CLI 传入的参数数组
     * @return 执行结果码（0 表示成功）
     * @throws Exception 执行过程中出现错误时抛出
     */
    @Override
    public int execute(String[] args) throws Exception {
        // 解析云项目描述文件（默认为工作目录下的 project.cloud）
        Project project = CloudDSLParser.parse(Paths.get("project.cloud"));
        // 委托给 CompileTask 处理核心编译逻辑
        new CompileTask(project, args).run();
        return 0;
    }
}
