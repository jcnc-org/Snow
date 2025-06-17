package org.jcnc.snow.compiler.cli.commands;

import org.jcnc.snow.compiler.cli.CLICommand;
import org.jcnc.snow.vm.VMLauncher;

/**
 * <p>
 * 命令实现：`snow run`
 * <br>
 * 用于运行已编译的 VM 字节码文件（.water）。
 * </p>
 * <ul>
 *     <li>支持传递额外 VM 参数。</li>
 *     <li>实际执行通过 {@link VMLauncher#main(String[])} 入口完成。</li>
 * </ul>
 * <p>
 * 用法：<br>
 * <code>snow run program.water</code>
 * </p>
 */
public final class RunCommand implements CLICommand {

    /**
     * 获取命令名。
     *
     * @return "run"
     */
    @Override
    public String name() {
        return "run";
    }

    /**
     * 获取命令描述。
     *
     * @return 命令简介
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
     * 执行 run 命令，运行 VM 指令文件。
     *
     * @param args 剩余参数（不含命令名），第一个应为 .vm 文件路径
     * @return 0 表示成功，1 表示参数错误
     * @throws Exception 运行 VM 时可能抛出的异常
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
