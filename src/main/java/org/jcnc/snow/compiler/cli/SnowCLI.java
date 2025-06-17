package org.jcnc.snow.compiler.cli;

import org.jcnc.snow.compiler.cli.commands.CompileCommand;
import org.jcnc.snow.compiler.cli.commands.RunCommand;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * SnowCLI 是项目的命令行入口类，负责解析用户输入、分发子命令并统一处理帮助和错误。
 */
public class SnowCLI {

    /**
     * 保存所有可用子命令的映射：命令名称 -> 命令实例的提供者
     * 通过 Map.of 初始化，添加新命令时只需在此注册。
     */
    private static final Map<String, Supplier<CLICommand>> COMMANDS = Map.of(
            "compile", CompileCommand::new,
            "run", RunCommand::new
    );

    /**
     * 程序主入口，解析和分发命令。
     *
     * @param args 用户在命令行中输入的参数数组
     */
    public static void main(String[] args) {
        // 如果未给出任何参数，则打印通用帮助并退出
        if (args.length == 0) {
            printGeneralUsage();
            System.exit(1);
        }

        // 第一个参数为子命令名称
        String cmdName = args[0];
        Supplier<CLICommand> cmdSupplier = COMMANDS.get(cmdName);
        // 如果命令不存在，则打印错误和帮助
        if (cmdSupplier == null) {
            System.err.println("Unknown command: " + cmdName);
            printGeneralUsage();
            System.exit(1);
        }

        // 创建子命令实例
        CLICommand cmd = cmdSupplier.get();

        // 提取子命令的剩余参数
        String[] sub = Arrays.copyOfRange(args, 1, args.length);

        // 支持统一的帮助标志：help, -h, --help
        if (sub.length > 0 && Set.of("help", "-h", "--help").contains(sub[0])) {
            // 调用子命令自己的 printUsage
            cmd.printUsage();
            System.exit(0);
        }

        // 执行子命令并捕获异常
        try {
            int exitCode = cmd.execute(sub);
            System.exit(exitCode);
        } catch (Exception e) {
            // 打印错误信息和堆栈
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * 打印动态生成的通用帮助信息。
     * 会遍历 COMMANDS，输出所有子命令的名称及描述信息。
     */
    private static void printGeneralUsage() {
        // 使用方式说明
        System.out.println("Usage: snow <command> [options]");
        System.out.println("Commands:");

        // 对每个注册的子命令，获取 description 并格式化输出
        COMMANDS.forEach((name, supplier) -> {
            CLICommand c = supplier.get();
            // %-10s 保证命令名称列宽度为 10
            System.out.printf("  %-10s %s%n", name, c.description());
        });
        // 提示如何查看子命令帮助
        System.out.println("Use 'snow <command> --help' to see command-specific options.");
    }
}
