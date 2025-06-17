package org.jcnc.snow.compiler.cli.commands;

import org.jcnc.snow.compiler.cli.CLICommand;
import org.jcnc.snow.compiler.cli.SnowCLI;

/**
 * <p>
 * 子命令：`snow version`
 * <br>
 * 用于打印当前 SnowCLI 的版本号。
 * </p>
 */
public final class VersionCommand implements CLICommand {

    @Override
    public String name() {
        return "version";
    }

    @Override
    public String description() {
        return "Print snow version.";
    }

    @Override
    public void printUsage() {
        System.out.println("Usage:");
        System.out.println("  snow version");
    }

    @Override
    public int execute(String[] args) {
        // 直接输出 SnowCLI.VERSION 常量
        System.out.println("snow version " + SnowCLI.VERSION);
        return 0;
    }
}
