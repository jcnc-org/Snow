
package org.jcnc.snow.cli.commands;

import org.jcnc.snow.cli.CLICommand;
import org.jcnc.snow.pkg.tasks.RunTask;

/**
 * CLI 命令：运行已编译的 VM 字节码文件（.water）。
 * <p>
 * 仅解析参数并委托给 {@link RunTask}，
 * 将 VM 运行逻辑下沉至 pkg 层，保持 CLI 无状态、薄封装。
 * </p>
 */
public final class RunCommand implements CLICommand {

    @Override
    public String name() {
        return "run";
    }

    @Override
    public String description() {
        return "Run the compiled VM bytecode file (.water)";
    }

    @Override
    public int execute(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return 1;
        }
        new RunTask(args).run();
        return 0;
    }

    @Override
    public void printUsage() {
        System.out.println("Usage:");
        System.out.println("  snow run <program.water> [vm-options]");
    }
}
