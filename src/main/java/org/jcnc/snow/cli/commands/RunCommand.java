package org.jcnc.snow.cli.commands;

import org.jcnc.snow.cli.CLICommand;
import org.jcnc.snow.vm.VMLauncher;

/**
 * CLI 命令：运行已编译的 VM 字节码文件（.water）。
 * <p>
 * 用于执行 VM 程序文件。支持传递额外 VM 参数，实际运行由 {@link VMLauncher#main(String[])} 完成。
 * </p>
 *
 * <pre>
 * 用法示例：
 * $ snow run program.water [additional VM options]
 * </pre>
 */
public final class RunCommand implements CLICommand {

    /**
     * 返回命令名，用于 CLI 调用。
     *
     * @return 命令名称字符串（"run"）
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
        return "Execute compiled VM instructions.";
    }

    /**
     * 打印该命令的用法说明。
     */
    @Override
    public void printUsage() {
        System.out.println("""
                Usage: snow run <program.water> [additional VM options]
                """);
    }

    /**
     * 执行 run 命令，运行指定的 VM 字节码文件。
     *
     * @param args 剩余参数（不含命令名），第一个应为 .water 文件路径，其后为可选 VM 参数
     * @return 0 表示执行成功，1 表示参数错误
     * @throws Exception VM 启动或执行过程中可能抛出的异常
     */
    @Override
    public int execute(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return 1;
        }
        VMLauncher.main(args);
        return 0;
    }
}
