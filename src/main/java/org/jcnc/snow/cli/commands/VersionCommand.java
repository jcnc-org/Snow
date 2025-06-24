package org.jcnc.snow.cli.commands;

import org.jcnc.snow.cli.SnowCLI;

/**
 * CLI 子命令：输出当前 Snow 工具的版本号。
 * <p>
 * 用于显示当前 CLI 工具版本，便于诊断、升级、兼容性确认等场景。
 * </p>
 *
 * <pre>
 * 用法示例：
 * $ snow version
 * </pre>
 */
public final class VersionCommand implements CLICommand {

    /**
     * 返回命令名，用于 CLI 调用。
     *
     * @return 命令名称字符串（"version"）
     */
    @Override
    public String name() {
        return "version";
    }

    /**
     * 返回命令简介，用于 CLI 帮助或命令列表展示。
     *
     * @return 命令描述字符串
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
        System.out.println("snow version \"" + SnowCLI.SNOW_VERSION + "\"");
        return 0;
    }
}
