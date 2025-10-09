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
 * </ul>
 * </p>
 *
 * <p>
 * <b>返回值：</b>
 * <ul>
 *   <li>{@code 0} — 所有测试通过</li>
 *   <li>{@code 1} — 有测试失败或 Demo 目录不存在</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>异常：</b>
 * <ul>
 *   <li>Demo 目录不存在时直接返回 1</li>
 *   <li>单个 Demo 执行或编译异常时，详细输出异常堆栈并计为失败</li>
 *   <li>如启用 {@code --stop-on-failure}，遇到首次失败立即终止循环</li>
 * </ul>
 * </p>
 */
public final class TestAllCommand implements CLICommand {

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
            System.err.println("Demo directory not found: " + demoRoot.toAbsolutePath());
            return 1;
        }

        // 2. 获取所有 Demo 子目录
        List<Path> demoDirs = Files.list(demoRoot)
                .filter(Files::isDirectory)
                .sorted()
                .collect(Collectors.toList());

        if (demoDirs.isEmpty()) {
            System.out.println("No demo directories found in " + demoRoot.toAbsolutePath());
            return 0;
        }

        System.out.println("Found " + demoDirs.size() + " demo directories. Starting tests...\n");

        AtomicInteger passed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        List<String> failedTests = new ArrayList<>();

        // 3. 遍历测试每个 Demo 子目录
        for (Path demoDir : demoDirs) {
            String demoName = demoDir.getFileName().toString();
            if (verbose) {
                System.out.println("Testing " + demoName + "...");
            }

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

                // 3.2 执行编译（可选运行）任务
                int result = new CompileTask(Project.fromFlatMap(Collections.emptyMap()),
                        compileArgs.toArray(new String[0])).execute(compileArgs.toArray(new String[0]));

                if (result == 0) {
                    if (!verbose) {
                        System.out.print(".");
                    } else {
                        System.out.println("✓ " + demoName + " PASSED");
                    }
                    passed.incrementAndGet();
                } else {
                    if (!verbose) {
                        System.out.print("F");
                    } else {
                        System.out.println("✗ " + demoName + " FAILED");
                    }
                    failed.incrementAndGet();
                    failedTests.add(demoName);

                    // stop-on-failure: 首次失败立即停止
                    if (stopOnFailure) {
                        System.out.println("\n\n=== Test stopped due to failure ===");
                        break;
                    }
                }
            } catch (Exception e) {
                if (!verbose) {
                    System.out.print("E");
                } else {
                    System.out.println("✗ " + demoName + " FAILED with exception: " + e.getMessage());
                    e.printStackTrace();
                }
                failed.incrementAndGet();
                failedTests.add(demoName + " (Exception: " + e.getMessage() + ")");

                // 错误详细输出
                System.err.println("\n=== Error in " + demoName + " ===");
                System.err.println("Directory: " + demoDir.toAbsolutePath());
                e.printStackTrace();
                System.err.println("==================================\n");

                // stop-on-failure: 首次异常立即停止
                if (stopOnFailure) {
                    System.out.println("\n\n=== Test stopped due to exception ===");
                    break;
                }
            }

            // 输出及时刷新
            System.out.flush();
        }

        // 4. 输出测试总结
        System.out.println("\n\n=== Test Summary ===");
        System.out.println("Passed: " + passed.get());
        System.out.println("Failed: " + failed.get());
        System.out.println("Total:  " + (passed.get() + failed.get()));

        // 5. 输出失败列表
        if (!failedTests.isEmpty()) {
            System.out.println("\nFailed tests:");
            for (String failedTest : failedTests) {
                System.out.println("  - " + failedTest);
            }
        }

        return failed.get() > 0 ? 1 : 0;
    }
}
