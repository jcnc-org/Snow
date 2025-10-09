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
 * CLI 命令: 批量测试所有 Demo 示例。
 * <p>
 * 该命令会遍历 playground/Demo 目录下的所有子目录，
 * 对每个子目录中的 .snow 文件进行编译和运行测试。
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

        // 解析参数
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

        // 获取所有 Demo 子目录
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

        // 遍历每个 Demo 目录进行测试
        for (Path demoDir : demoDirs) {
            String demoName = demoDir.getFileName().toString();
            if (verbose) {
                System.out.println("Testing " + demoName + "...");
            }

            try {
                // 构造编译参数
                List<String> compileArgs = new ArrayList<>();
                compileArgs.add("-d");
                compileArgs.add(demoDir.toString());
                compileArgs.add("-o");
                compileArgs.add("target/" + demoName);

                if (runAfterCompile) {
                    compileArgs.add("run");
                }

                // 执行编译任务
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

                    // 如果启用了stop-on-failure选项，则在第一个失败时停止
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

                // 打印详细的错误信息，包括堆栈跟踪
                System.err.println("\n=== Error in " + demoName + " ===");
                System.err.println("Directory: " + demoDir.toAbsolutePath());
                e.printStackTrace();
                System.err.println("==================================\n");

                // 如果启用了stop-on-failure选项，则在第一个失败时停止
                if (stopOnFailure) {
                    System.out.println("\n\n=== Test stopped due to exception ===");
                    break;
                }
            }

            // 刷新输出，确保及时显示进度
            System.out.flush();
        }

        // 输出测试总结
        System.out.println("\n\n=== Test Summary ===");
        System.out.println("Passed: " + passed.get());
        System.out.println("Failed: " + failed.get());
        System.out.println("Total:  " + (passed.get() + failed.get()));

        // 如果有失败的测试，列出它们
        if (!failedTests.isEmpty()) {
            System.out.println("\nFailed tests:");
            for (String failedTest : failedTests) {
                System.out.println("  - " + failedTest);
            }
        }

        return failed.get() > 0 ? 1 : 0;
    }
}