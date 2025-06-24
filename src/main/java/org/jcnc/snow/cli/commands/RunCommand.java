package org.jcnc.snow.cli.commands;

import org.jcnc.snow.pkg.tasks.RunTask;

/**
 * CLI 命令：运行已编译的 VM 字节码文件（.water）。
 * <p>
 * 仅解析参数并委托给 {@link RunTask}，
 * 将 VM 运行逻辑下沉至 pkg 层，保持 CLI 无状态、薄封装。
 * </p>
 *
 * <pre>
 * 用法示例：
 * $ snow run main.water
 * </pre>
 */
public final class RunCommand implements CLICommand {

    /**
     * 返回命令名称，用于 CLI 调用。
     *
     * @return 命令名称，如 "run"
     */
    @Override
    public String name() {
        return "run";
    }

    /**
     * 返回命令简介，用于 CLI 帮助或命令列表展示。
     *
     * @return 命令描述字符串
     */
    @Override
    public String description() {
        return "Run the compiled VM bytecode file (.water)";
    }

    /**
     * 执行运行任务。
     *
     * @param args CLI 传入的参数数组
     * @return 执行结果码（0 表示成功，非 0 表示失败）
     * @throws Exception 执行过程中出现错误时抛出
     */
    @Override
    public int execute(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return 1;
        }
        // 委托给 RunTask 执行字节码运行逻辑
        new RunTask(args).run();
        return 0;
    }

    /**
     * 打印命令用法信息。
     */
    @Override
    public void printUsage() {
        System.out.println("Usage:");
        System.out.println("  snow run <program.water>");
    }
}
