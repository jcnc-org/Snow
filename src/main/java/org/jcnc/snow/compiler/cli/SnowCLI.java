package org.jcnc.snow.compiler.cli;

import org.jcnc.snow.compiler.cli.commands.CompileCommand;
import org.jcnc.snow.compiler.cli.commands.RunCommand;
import org.jcnc.snow.compiler.cli.commands.VersionCommand;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * SnowCLI 是项目的命令行入口类，负责解析用户输入、
 * 分发子命令，并统一处理帮助、版本和错误输出。
 */
public class SnowCLI {
    /** Snow 编程语言的版本号。 */
    public static final String VERSION = "1.0.0";

    /** 全局帮助标志，当输入匹配时显示帮助信息。 */
    private static final Set<String> GLOBAL_HELP_FLAGS = Set.of("help", "-h", "--help");

    /** 全局版本标志，当输入匹配时显示版本信息。 */
    private static final Set<String> GLOBAL_VERSION_FLAGS = Set.of("-v", "--version");

    /**
     * 全局选项定义列表。
     * 每个 Option 包含可用标志列表及对应的描述信息。
     */
    private static final List<Option> GLOBAL_OPTIONS = List.of(
            new Option(List.of("-h", "--help"), "Show this help message and exit"),
            new Option(List.of("-v", "--version"), "Print snow programming language version and exit")
    );

    /**
     * 所有可用子命令的映射：
     * 键为命令名称，值为对应命令实例的提供者。
     * 通过 Map.of 初始化，添加新命令时只需在此注册。
     */
    private static final Map<String, Supplier<CLICommand>> COMMANDS = Map.of(
            "compile", CompileCommand::new,
            "run", RunCommand::new,
            "version", VersionCommand::new
    );

    /**
     * 程序主入口。
     * <p>
     * 负责处理以下逻辑：
     * <ul>
     *   <li>全局帮助标志：无参数或 help 标志时显示通用帮助并退出。</li>
     *   <li>全局版本标志：-v/--version 时打印版本并退出。</li>
     *   <li>子命令调度：根据第一个参数匹配子命令并分发执行。</li>
     *   <li>子命令帮助：子命令后带 --help 时显示该命令帮助。</li>
     *   <li>错误处理：捕获执行异常并打印错误信息。</li>
     * </ul>
     *
     * @param args 用户在命令行中输入的参数数组
     */
    public static void main(String[] args) {
        // —— 全局帮助 —— //
        if (args.length == 0 || GLOBAL_HELP_FLAGS.contains(args[0])) {
            printGeneralUsage();
            System.exit(0);
        }

        // —— 全局版本 —— //
        if (GLOBAL_VERSION_FLAGS.contains(args[0])) {
            new VersionCommand().execute(new String[0]);
            System.exit(0);
        }

        // —— 子命令调度 —— //
        String cmdName = args[0];
        Supplier<CLICommand> cmdSupplier = COMMANDS.get(cmdName);
        if (cmdSupplier == null) {
            System.err.println("Unknown command: " + cmdName);
            printGeneralUsage();
            System.exit(1);
        }

        CLICommand cmd = cmdSupplier.get();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        // —— 子命令帮助 —— //
        if (subArgs.length > 0 && GLOBAL_HELP_FLAGS.contains(subArgs[0])) {
            cmd.printUsage();
            System.exit(0);
        }

        // —— 执行子命令 —— //
        try {
            int exitCode = cmd.execute(subArgs);
            System.exit(exitCode);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * 打印动态生成的通用帮助信息。
     * <p>
     * 列出全局选项和所有注册子命令的名称与描述。
     */
    private static void printGeneralUsage() {
        System.out.println("Usage:");
        System.out.println("  snow [OPTIONS] <command>");
        System.out.println();
        System.out.println("Options:");
        // 动态遍历全局选项
        for (Option opt : GLOBAL_OPTIONS) {
            String flags = String.join(", ", opt.flags());
            System.out.printf("  %-15s %s%n", flags, opt.description());
        }
        System.out.println();
        System.out.println("Commands:");
        // 遍历注册的子命令，动态输出
        COMMANDS.forEach((name, supplier) -> {
            CLICommand c = supplier.get();
            System.out.printf("  %-10s %s%n", name, c.description());
        });
        System.out.println();
        System.out.println("Use \"snow <command> --help\" for command-specific options.");
    }

    /**
     * 全局选项的数据结构，包含标志列表和描述信息。
     *
     * @param flags       选项的所有标志，例如 -h, --help
     * @param description 选项的功能说明
     */
    private record Option(List<String> flags, String description) {
    }
}
