package org.jcnc.snow.compiler.cli;

import org.jcnc.snow.compiler.cli.commands.CompileCommand;
import org.jcnc.snow.compiler.cli.commands.RunCommand;

import java.util.*;

/**
 * <p>
 * Snow 语言统一命令行入口（CLI）。
 * <br>
 * 负责命令注册、解析与调度。
 * </p>
 * <ul>
 *   <li>支持核心命令自动注册（compile/run）。</li>
 *   <li>支持通过 ServiceLoader 自动发现并注册第三方命令。</li>
 *   <li>统一异常处理和 usage 帮助输出。</li>
 * </ul>
 */
public final class SnowCLI {

    /** 命令注册表，按插入顺序保存命令名到实现的映射。 */
    private final Map<String, CLICommand> registry = new LinkedHashMap<>();

    /**
     * 构造 CLI，自动注册所有可用命令。
     * <ul>
     *   <li>通过 ServiceLoader 加载所有外部扩展命令。</li>
     *   <li>始终内置注册 compile、run 等核心命令。</li>
     * </ul>
     */
    public SnowCLI() {
        // 1. 自动发现 ServiceLoader 扩展命令
        ServiceLoader.load(CLICommand.class).forEach(this::register);
        // 2. 注册核心命令，保证 CLI 可用
        register(new CompileCommand());
        register(new RunCommand());
    }

    /**
     * 注册一个命令到 CLI（命令名唯一，若已注册则跳过）。
     *
     * @param cmd 待注册命令
     */
    private void register(CLICommand cmd) {
        registry.putIfAbsent(cmd.name(), cmd);
    }

    /**
     * 解析命令行参数并执行相应命令。
     *
     * @param args 命令行参数
     * @return 进程退出码（0=成功, 1=未知命令, -1=命令异常）
     */
    public int run(String[] args) {
        // 无参数或 help，打印全局用法
        if (args.length == 0
                || Set.of("help", "-h", "--help").contains(args[0])) {
            printGlobalUsage();
            return 0;
        }

        // 根据命令名查找注册表
        CLICommand cmd = registry.get(args[0]);
        if (cmd == null) {
            System.err.printf("Unknown command: %s%n%n", args[0]);
            printGlobalUsage();
            return 1;
        }

        // 提取命令余下参数（不包含命令名）
        String[] sub = Arrays.copyOfRange(args, 1, args.length);
        try {
            return cmd.execute(sub);
        } catch (Exception e) {
            System.err.printf("Error executing command '%s': %s%n",
                    cmd.name(), e.getMessage());
            e.printStackTrace(System.err);
            return -1;
        }
    }

    /**
     * 打印全局帮助信息（所有已注册命令的 usage）。
     */
    private void printGlobalUsage() {
        System.out.println("""
                Usage: snow <command> [options]

                Available commands:""");
        int pad = registry.keySet().stream()
                .mapToInt(String::length).max().orElse(10) + 2;
        registry.values().stream()
                .sorted(Comparator.comparing(CLICommand::name))
                .forEach(c -> System.out.printf("  %-" + pad + "s%s%n",
                        c.name(), c.description()));

        System.out.println("""
                
                Use 'snow <command> --help' for command-specific details.
                """);
    }

    /**
     * CLI 程序主入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        int code = new SnowCLI().run(args);
        System.exit(code);
    }
}
