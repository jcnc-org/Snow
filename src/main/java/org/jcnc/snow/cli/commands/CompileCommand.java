package org.jcnc.snow.cli.commands;

import org.jcnc.snow.cli.CLICommand;
import org.jcnc.snow.pkg.dsl.CloudDSLParser;
import org.jcnc.snow.pkg.model.Project;
import org.jcnc.snow.pkg.tasks.CompileTask;

import java.nio.file.Paths;

/**
 * CLI 命令：编译当前项目。
 * <p>
 * 负责读取项目描述文件并委托给 {@link CompileTask}，
 * </p>
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
        System.out.println("  snow compile [run] [-o <name>] [-d <srcDir>] [file1.snow file2.snow …]");
        System.out.println("Options:");
        System.out.println("  run           compile then run");
        System.out.println("  -o <name>     specify output base name (without .water suffix)");
        System.out.println("  -d <srcDir>   recursively compile all .snow files in directory");    }

    @Override
    public int execute(String[] args) throws Exception {
        // 解析云项目描述文件（默认为工作目录下的 cloud.snow）
        Project project = CloudDSLParser.parse(Paths.get("project.cloud"));
        // 委托给 CompileTask 处理核心编译逻辑
        new CompileTask(project, args).run();
        return 0;
    }
}