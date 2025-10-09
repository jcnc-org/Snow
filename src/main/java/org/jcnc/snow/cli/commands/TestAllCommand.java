package org.jcnc.snow.cli.commands;

import org.jcnc.snow.cli.api.CLICommand;
import org.jcnc.snow.pkg.model.Project;
import org.jcnc.snow.pkg.tasks.CompileTask;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * {@code TestAllCommand} 实现 CLI 命令 {@code test-all}，
 * 批量编译并测试 playground/Demo 目录下所有示例。
 *
 * <p>
 * <b>命令格式：</b>
 * <ul>
 *   <li>{@code snow test-all} — 编译并运行全部 Demo</li>
 *   <li>{@code snow test-all --no-run} — 仅编译不运行</li>
 *   <li>{@code snow test-all --verbose} — 输出详细测试信息</li>
 *   <li>{@code snow test-all --stop-on-failure} — 首次失败时立即中止</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>行为说明：</b>
 * <ul>
 *   <li>遍历 {@code playground/Demo} 下所有子目录</li>
 *   <li>每个子目录按参数编译（并可选运行）所有 .snow 示例</li>
 *   <li>测试统计通过/失败数量，并输出详细结果与汇总</li>
 *   <li>可通过参数控制输出和失败行为</li>
 *   <li><b>新增：</b>单个 Demo 超过 2 秒未完成则判为超时并立即进入下一个 Demo</li>
 * </ul>
 * </p>
 */
public final class TestAllCommand implements CLICommand {

    /**
     * 单个 Demo 的最大允许运行时长（秒）
     */
    private static final long TIMEOUT_SECONDS = 2L;

    // ===== ANSI 样式（跨平台终端普遍支持，Windows 10+ 也支持）=====
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";

    // 前景色
    private static final String RED = "\u001B[31m";
    private static final String BRIGHT_RED = "\u001B[91m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BRIGHT_YEL = "\u001B[93m";
    private static final String CYAN = "\u001B[36m";
    private static final String BRIGHT_CYAN = "\u001B[96m";
    private static final String BRIGHT_GRN = "\u001B[92m";

    @Override
    public String name() {
        return "test-all";
    }

    @Override
    public String description() {
        return "Compile and run all demo examples in playground/Demo directory.";
    }

    @Override
    public void printUsage() {
        System.out.println("Usage:");
        System.out.println("  snow test-all");
        System.out.println("  snow test-all --no-run");
        System.out.println("  snow test-all --verbose");
        System.out.println("  snow test-all --stop-on-failure");
    }

    @Override
    public int execute(String[] args) throws Exception {
        boolean runAfterCompile = true;
        boolean verbose = false;
        boolean stopOnFailure = false;

        // 1. 解析命令行参数
        for (String arg : args) {
            if ("--no-run".equals(arg)) {
                runAfterCompile = false;
            } else if ("--verbose".equals(arg)) {
                verbose = true;
            } else if ("--stop-on-failure".equals(arg)) {
                stopOnFailure = true;
            }
        }

        Path demoRoot = Paths.get("playground", "Demo");
        if (!Files.exists(demoRoot)) {
            System.err.println(RED + "Demo directory not found: " + demoRoot.toAbsolutePath() + RESET);
            return 1;
        }

        // 2. 获取所有 Demo 子目录
        List<Path> demoDirs = Files.list(demoRoot)
                .filter(Files::isDirectory)
                .sorted()
                .toList();

        if (demoDirs.isEmpty()) {
            System.out.println(BRIGHT_YEL + "No demo directories found in " + demoRoot.toAbsolutePath() + RESET);
            return 0;
        }

        System.out.println(BRIGHT_CYAN + "Found " + demoDirs.size() + " demo directories. Starting tests..." + RESET + "\n");

        AtomicInteger passed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        List<String> failedTests = new ArrayList<>();

        // 3. 遍历测试每个 Demo 子目录
        for (Path demoDir : demoDirs) {
            String demoName = demoDir.getFileName().toString();
            if (verbose) {
                System.out.println(CYAN + "Testing " + demoName + "..." + RESET);
            }

            // 为每个 Demo 单独创建一个执行器，保证超时后可强制中断并清理
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                // 3.1 构造编译参数
                List<String> compileArgs = new ArrayList<>();
                compileArgs.add("-d");
                compileArgs.add(demoDir.toString());
                compileArgs.add("-o");
                compileArgs.add("target/" + demoName);

                if (runAfterCompile) {
                    compileArgs.add("run");
                }

                // 3.2 执行编译（可选运行）任务（带 2 秒超时）
                Callable<Integer> task = () -> new CompileTask(
                        Project.fromFlatMap(Collections.emptyMap()),
                        compileArgs.toArray(new String[0])
                ).execute(compileArgs.toArray(new String[0]));

                Future<Integer> future = executor.submit(task);

                int result;
                try {
                    result = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (TimeoutException te) {
                    // 超时：取消任务并视为失败，然后立即进入下一个 Demo
                    future.cancel(true); // 尝试中断
                    if (!verbose) {
                        System.out.print(BRIGHT_YEL + "T" + RESET);
                    } else {
                        System.out.println(BOLD + BRIGHT_YEL + "✗ " + demoName + " TIMEOUT > " + TIMEOUT_SECONDS + "s" + RESET);
                    }
                    failed.incrementAndGet();
                    failedTests.add(demoName + " (Timeout > " + TIMEOUT_SECONDS + "s)");

                    if (stopOnFailure) {
                        System.out.println("\n\n" + BOLD + YELLOW + "=== Test stopped due to timeout ===" + RESET);
                        break;
                    }

                    // 直接继续下一个 demo
                    continue;
                }

                if (result == 0) {
                    if (!verbose) {
                        System.out.print(BRIGHT_GRN + "." + RESET);
                    } else {
                        // 按你的要求：通过用红色醒目标题显示
                        System.out.println(BOLD + BRIGHT_RED + "✓ " + demoName + " PASSED" + RESET);
                    }
                    passed.incrementAndGet();
                } else {
                    if (!verbose) {
                        System.out.print(RED + "F" + RESET);
                    } else {
                        System.out.println(BOLD + RED + "✗ " + demoName + " FAILED" + RESET);
                    }
                    failed.incrementAndGet();
                    failedTests.add(demoName);

                    if (stopOnFailure) {
                        System.out.println("\n\n" + BOLD + RED + "=== Test stopped due to failure ===" + RESET);
                        break;
                    }
                }

            } catch (Exception e) {
                if (!verbose) {
                    System.out.print(RED + "E" + RESET);
                } else {
                    System.out.println(BOLD + RED + "✗ " + demoName + " FAILED with exception: " + e.getMessage() + RESET);
                    e.printStackTrace();
                }
                failed.incrementAndGet();
                failedTests.add(demoName + " (Exception: " + e.getMessage() + ")");

                // 错误详细输出
                System.err.println("\n" + BOLD + RED + "=== Error in " + demoName + " ===" + RESET);
                System.err.println("Directory: " + demoDir.toAbsolutePath());
                e.printStackTrace();
                System.err.println(BOLD + RED + "==================================" + RESET + "\n");

                if (stopOnFailure) {
                    System.out.println("\n\n" + BOLD + RED + "=== Test stopped due to exception ===" + RESET);
                    // 结束前关闭当前执行器
                    executor.shutdownNow();
                    break;
                }
            } finally {
                // 结束前确保线程退出
                executor.shutdownNow();
            }

            // 输出及时刷新
            System.out.flush();
        }

        // 4. 输出测试总结
        System.out.println("\n");
        System.out.println(BOLD + CYAN + "=== Test Summary ===" + RESET);
        System.out.println(BRIGHT_GRN + "Passed: " + passed.get() + RESET);
        System.out.println(RED + "Failed: " + failed.get() + RESET);
        System.out.println("Total:  " + (passed.get() + failed.get()));

        // 5. 输出失败列表
        if (!failedTests.isEmpty()) {
            System.out.println("\n" + BOLD + YELLOW + "Failed tests:" + RESET);
            for (String failedTest : failedTests) {
                System.out.println("  - " + failedTest);
            }
        }

        return failed.get() > 0 ? 1 : 0;
    }
}
