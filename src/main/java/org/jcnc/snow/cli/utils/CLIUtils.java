package org.jcnc.snow.cli.utils;

import org.jcnc.snow.cli.CLICommand;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 命令行界面通用工具类，提供全局帮助和版本标志、全局选项定义，以及打印通用用法的方法。
 */
public class CLIUtils {

    /**
     * 全局帮助标志集合，支持 "help"、"-h"、"--help"。
     */
    public static final Set<String> GLOBAL_HELP_FLAGS = Set.of(
            "help", "-h", "--help"
    );

    /**
     * 全局版本标志集合，支持 "-v"、"--version"。
     */
    public static final Set<String> GLOBAL_VERSION_FLAGS = Set.of(
            "-v", "--version"
    );

    /**
     * 全局选项列表，包括帮助和版本选项的描述。
     */
    public static final List<Option> GLOBAL_OPTIONS = List.of(
            new Option(List.of("-h", "--help"), "Show this help message and exit"),
            new Option(List.of("-v", "--version"), "Print snow programming language version and exit")
    );

    /**
     * 打印命令行工具的通用用法说明。
     * <p>
     *
     * @param commands 可用子命令名称到命令处理器的映射，用于列出所有子命令及其描述
     */
    public static void printGeneralUsage(Map<String, Supplier<CLICommand>> commands) {
        System.out.println("Usage:");
        System.out.println("  snow [OPTIONS] <command>");
        System.out.println();
        System.out.println("Options:");
        for (Option opt : GLOBAL_OPTIONS) {
            String flags = String.join(", ", opt.flags());
            System.out.printf("  %-15s %s%n", flags, opt.description());
        }
        System.out.println();
        System.out.println("Commands:");
        commands.forEach((name, supplier) -> {
            CLICommand c = supplier.get();
            System.out.printf("  %-10s %s%n", name, c.description());
        });
        System.out.println();
        System.out.println("Use \"snow <command> --help\" for command-specific options.");
    }

    /**
     * 全局选项的数据结构，包含选项标志和描述。
     *
     * @param flags       选项标志列表，例如 ["-h", "--help"]
     * @param description 选项功能描述
     */
    public record Option(List<String> flags, String description) {
    }
}
