package org.jcnc.snow.cli;

import org.jcnc.snow.cli.api.CLICommand;
import org.jcnc.snow.cli.commands.*;
import org.jcnc.snow.cli.utils.CLIUtils;
import org.jcnc.snow.cli.utils.VersionUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Snow 编程语言的命令行接口入口类。
 * <p>
 * 该类解析用户输入的命令行参数，调度相应的子命令（compile、run、version），
 * 并处理全局帮助和版本信息输出。
 * </p>
 */
public class SnowCLI {

    /**
     * Snow 编程语言的当前版本号，从资源文件中加载。
     */
    public static final String SNOW_VERSION = VersionUtils.loadVersion();

    /**
     * 可用子命令名称与对应命令处理器的映射表。
     * 键为子命令名称（"compile", "run", "version"），
     * 值为返回相应 {@link CLICommand} 实例的 Supplier。
     */
    private static final Map<String, Supplier<CLICommand>> COMMANDS = Map.of(
            "generate", GenerateCommand::new,
            "compile", CompileCommand::new,
            "run", RunCommand::new,
            "version", VersionCommand::new,
            "init", InitCommand::new,
            "build", BuildCommand::new,
            "install", InstallCommand::new,
            "publish", PublishCommand::new,
            "clean", CleanCommand::new

    );

    /**
     * 程序入口方法，解析并调度子命令。
     * <p>
     *
     * @param args 命令行参数数组
     *             无参数或首参数为帮助标志时，打印全局用法说明并退出
     *             首参数为版本标志时，打印版本信息并退出
     *             首参数为子命令名时，进一步解析该子命令的参数并执行
     */
    public static void main(String[] args) {
        // 处理全局帮助
        if (args.length == 0 || CLIUtils.GLOBAL_HELP_FLAGS.contains(args[0])) {
            CLIUtils.printGeneralUsage(COMMANDS);
            System.exit(0);
        }

        // 处理全局版本请求
        if (CLIUtils.GLOBAL_VERSION_FLAGS.contains(args[0])) {
            new VersionCommand().execute(new String[0]);
            System.exit(0);
        }

        // 子命令名称
        String cmdName = args[0];
        Supplier<CLICommand> cmdSupplier = COMMANDS.get(cmdName);

        // 未知子命令处理
        if (cmdSupplier == null) {
            System.err.println("Unknown command: " + cmdName);
            CLIUtils.printGeneralUsage(COMMANDS);
            System.exit(1);
        }

        // 创建对应子命令实例
        CLICommand cmd = cmdSupplier.get();
        // 提取子命令参数
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        // 如果子命令请求帮助，则打印该命令的用法说明并退出
        if (subArgs.length > 0 && CLIUtils.GLOBAL_HELP_FLAGS.contains(subArgs[0])) {
            cmd.printUsage();
            System.exit(0);
        }

        // 执行子命令并根据返回的退出码退出
        try {
            int exitCode = cmd.execute(subArgs);
            System.exit(exitCode);
        } catch (Exception e) {
            // 捕获命令执行过程中的异常并打印错误消息
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}