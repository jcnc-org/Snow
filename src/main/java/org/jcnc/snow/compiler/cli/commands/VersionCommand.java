package org.jcnc.snow.compiler.cli.commands;

import org.jcnc.snow.compiler.cli.CLICommand;
import org.jcnc.snow.compiler.cli.SnowCLI;

/**
 * <p>
 * 子命令实现：`snow version`
 * <br>
 * 用于打印当前 Snow 的版本号。
 * </p>
 */
public final class VersionCommand implements CLICommand {

    /**
     * 获取命令名。
     *
     * @return "version"
     */
    @Override
    public String name() {
        return "version";
    }

    /**
     * 获取命令描述。
     *
     * @return 命令简介
     */
    @Override
    public String description() {
        return "Print snow version.";
    }

    /**
     * 打印该命令的用法说明。
     */
    @Override
    public void printUsage() {
        System.out.println("Usage:");
        System.out.println("  snow version");
    }

    /**
     * 执行 version 命令，输出当前 Snow 的版本号。
     *
     * @param args 命令参数，此命令不接受额外参数
     * @return 0 表示成功执行
     */
    @Override
    public int execute(String[] args) {
        System.out.println("snow version " + SnowCLI.SNOW_VERSION);
        return 0;
    }
}
