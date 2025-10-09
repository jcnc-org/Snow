package org.jcnc.snow.cli.commands;

import org.jcnc.snow.cli.api.CLICommand;
import org.jcnc.snow.pkg.model.Project;
import org.jcnc.snow.pkg.tasks.CompileTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code TestAllCommand} 实现 CLI 命令 {@code test-all}，
 * 支持批量编译与测试 playground/Demo 目录下所有示例工程。
 *
 * <p>
 * <b>命令用法：</b>
 * <ul>
 *   <li>{@code snow test-all} —— 编译并运行全部 Demo 示例</li>
 *   <li>{@code snow test-all --no-run} —— 仅编译不运行</li>
 *   <li>{@code snow test-all --verbose} —— 输出详细测试信息</li>
 *   <li>{@code snow test-all --stop-on-failure} —— 首次失败/异常时立即终止批量</li>
 *   <li>{@code snow test-all --timeout=2000} —— 单个 Demo 的最大执行时间（默认 2000 毫秒）</li>
 *   <li>{@code snow test-all --snow-path=[path|auto]} —— 指定 snow 可执行文件路径，auto 时自动检测</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>核心功能与行为：</b>
 * <ul>
 *   <li>遍历 {@code playground/Demo} 下所有子目录</li>
 *   <li>每个子目录单独构建（如有 project.cloud 且 --snow-path 指定，则调用外部 snow，否则用内部 CompileTask）</li>
 *   <li>支持每个 demo 最大超时设置，超时则显示 ? 并标记为失败</li>
 *   <li>可选：编译后自动运行</li>
 *   <li>测试时随时按 {@code Enter} 跳过当前 demo，继续后续（全平台兼容，非信号/kill）</li>
 *   <li>统计测试通过/失败数量，终端输出彩色提示，详细列表总结</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>返回值约定：</b>
 * <ul>
 *   <li>{@code 0} —— 所有 Demo 测试通过或无 Demo</li>
 *   <li>{@code 1} —— 有 Demo 测试失败/异常、参数错误、外部执行文件找不到等</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>异常与边界说明：</b>
 * <ul>
 *   <li>playground/Demo 不存在时直接输出错误并返回 1</li>
 *   <li>如 --snow-path 非 auto 且找不到指定文件，立即报错退出</li>
 *   <li>单个 demo 编译或执行超时，输出 ? 并视为失败，不触发 stop-on-failure</li>
 *   <li>stop-on-failure 只在正常编译/运行失败或抛出异常时触发中止，超时和跳过不会中断批量</li>
 *   <li>如测试中按 Enter，立即跳过当前 demo（包含执行与超时等待），继续下一个</li>
 * </ul>
 * </p>
 */
public final class TestAllCommand implements CLICommand {

    /**
     * 默认单个 demo 的最大执行超时时间（毫秒）
     */
    private static final long DEFAULT_TIMEOUT_MS = 2000L;

    // ANSI 控制台输出样式
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String RED = "\u001B[31m";
    private static final String BRIGHT_RED = "\u001B[91m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BRIGHT_YEL = "\u001B[93m";
    private static final String CYAN = "\u001B[36m";
    private static final String BRIGHT_CYAN = "\u001B[96m";
    private static final String BRIGHT_GRN = "\u001B[92m";

    /**
     * 跳过当前 demo 的全局标志，监听输入线程通过 [Enter] 设置
     */
    private static volatile boolean skipCurrent = false;
    /**
     * 监听输入线程的运行标志
     */
    private static volatile boolean inputThreadRunning = true;

    /**
     * 命令行参数带空格时自动加引号
     */
    private static List<String> quoteArgs(List<String> args) {
        List<String> out = new ArrayList<>(args.size());
        for (String a : args) out.add(a.contains(" ") ? '"' + a + '"' : a);
        return out;
    }

    /**
     * 判断是否为 Windows 平台
     */
    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /**
     * 自动从 target/release 目录递归查找最新的 snow(.exe) 可执行文件。
     *
     * @return 若找到则返回绝对路径，否则 null
     */
    private static Path resolveSnowFromTargetRelease() {
        String exeName = isWindows() ? "snow.exe" : "snow";
        Path base = Paths.get("target", "release");
        if (!Files.exists(base)) return null;

        List<Path> candidates = new ArrayList<>();
        try (var stream = Files.walk(base, 4)) {
            stream.filter(p -> p.getFileName() != null
                            && p.getFileName().toString().equalsIgnoreCase(exeName)
                            && Files.isRegularFile(p))
                    .forEach(candidates::add);
        } catch (IOException ignored) {
        }

        if (candidates.isEmpty()) return null;

        candidates.sort(Comparator.comparing((Path p) -> {
            try {
                return Files.getLastModifiedTime(p);
            } catch (IOException e) {
                return FileTime.fromMillis(0);
            }
        }).reversed());

        return candidates.getFirst();
    }

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
        System.out.println("  snow test-all [options]");
        System.out.println("Options:");
        System.out.println("  --no-run                 仅编译不运行");
        System.out.println("  --verbose                输出详细信息");
        System.out.println("  --stop-on-failure        首次失败/异常时中止（超时不触发）");
        System.out.println("  --timeout=<ms>           设置单个 Demo 超时（毫秒，默认 2000）");
        System.out.println("  --snow-path=<path|auto>  指定 snow.exe 路径；auto 自动在 target/release/**/bin 下查找");
        System.out.println("  测试时可随时按 [Enter] 跳过当前 demo，继续后续测试");
    }

    @Override
    public int execute(String[] args) throws Exception {
        boolean runAfterCompile = true;
        boolean verbose = false;
        boolean stopOnFailure = false;
        String externalSnowPath = null;
        boolean requestedAuto = false; // 用户是否传了 auto
        long timeoutMs = DEFAULT_TIMEOUT_MS;

        // 1. 解析命令行参数
        for (String arg : args) {
            if ("--no-run".equals(arg)) {
                runAfterCompile = false;
            } else if ("--verbose".equals(arg)) {
                verbose = true;
            } else if ("--stop-on-failure".equals(arg)) {
                stopOnFailure = true;
            } else if (arg.startsWith("--snow-path=")) {
                externalSnowPath = arg.substring("--snow-path=".length()).trim();
                requestedAuto = "auto".equalsIgnoreCase(externalSnowPath);
            } else if (arg.startsWith("--timeout=")) {
                try {
                    timeoutMs = Long.parseLong(arg.substring("--timeout=".length()).trim());
                } catch (NumberFormatException e) {
                    System.err.println(RED + "Invalid timeout value: " + arg + RESET);
                    return 1;
                }
            }
        }

        // 2. 处理 --snow-path=auto：尝试自动查找 snow 可执行文件
        if (requestedAuto) {
            Path resolved = resolveSnowFromTargetRelease();
            if (resolved == null) {
                if (verbose) {
                    System.out.println(BRIGHT_YEL +
                            "Cannot resolve snow executable from target/release. Fallback to internal CompileTask." + RESET);
                }
                externalSnowPath = null; // 回退到内部模式
            } else {
                externalSnowPath = resolved.toAbsolutePath().toString();
            }
        }

        Path demoRoot = Paths.get("playground", "Demo");
        if (!Files.exists(demoRoot)) {
            System.err.println(RED + "Demo directory not found: " + demoRoot.toAbsolutePath() + RESET);
            return 1;
        }

        // 3. 检查外部 snow 路径
        Path exePath = null;
        if (externalSnowPath != null) exePath = Paths.get(externalSnowPath);
        if (!requestedAuto) {
            if (exePath != null && !Files.exists(exePath)) {
                System.err.println(RED + "Specified snow executable not found: " + exePath.toAbsolutePath() + RESET);
                return 1;
            }
        }
        if (externalSnowPath != null && verbose) {
            System.out.println(BRIGHT_CYAN + "Using external snow executable: " + exePath.toAbsolutePath() + RESET);
        }

        // 4. 获取全部 Demo 子目录
        List<Path> demoDirs = Files.list(demoRoot)
                .filter(Files::isDirectory)
                .sorted()
                .toList();

        if (demoDirs.isEmpty()) {
            System.out.println(BRIGHT_YEL + "No demo directories found in " + demoRoot.toAbsolutePath() + RESET);
            return 0;
        }

        System.out.println(BRIGHT_CYAN + "Found " + demoDirs.size() + " demo directories. Starting tests..." + RESET + "\n");
        System.out.println(BRIGHT_YEL + "Timeout per demo: " + timeoutMs + " ms" + RESET);
        System.out.println(BRIGHT_CYAN + "[提示] 测试进行时可随时按 [Enter] 跳过当前 demo" + RESET);

        AtomicInteger passed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        List<String> failedTests = new ArrayList<>();

        // 5. 启动输入监听线程：检测 [Enter] 跳过当前 demo
        Thread inputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                while (inputThreadRunning) {
                    String line = reader.readLine();
                    if (line == null) break;
                    skipCurrent = true;
                    System.err.println(BRIGHT_YEL + "[Enter] Skip current demo and continue..." + RESET);
                }
            } catch (Exception ignore) {
            }
        }, "Demo-SkipListener");
        inputThread.setDaemon(true);
        inputThread.start();

        // 6. 遍历测试每个 Demo 子目录
        for (Path demoDir : demoDirs) {
            skipCurrent = false; // 每个 demo 前重置

            String demoName = demoDir.getFileName().toString();
            if (verbose) System.out.println(CYAN + "Testing " + demoName + "..." + RESET);

            boolean hasCloud = Files.exists(demoDir.resolve("project.cloud"));
            ExecutorService executor = Executors.newSingleThreadExecutor();

            try {
                Callable<Integer> task;

                // 6.1 优先尝试外部 CLI 模式，否则回退内部 CompileTask
                if (hasCloud && externalSnowPath != null) {
                    String finalSnow = externalSnowPath;
                    boolean finalRun = runAfterCompile;
                    boolean finalVerbose = verbose;
                    task = () -> runExternalSnowBuildAndMaybeRun(finalSnow, demoDir, finalRun, finalVerbose);
                } else {
                    if (!hasCloud && externalSnowPath != null && verbose) {
                        System.out.println(BRIGHT_CYAN + "No project.cloud found; fallback to internal CompileTask for "
                                + demoName + RESET);
                    }
                    List<String> compileArgs = new ArrayList<>();
                    compileArgs.add("-d");
                    compileArgs.add(demoDir.toString());
                    compileArgs.add("-o");
                    compileArgs.add("target/" + demoName);
                    if (runAfterCompile) compileArgs.add("run");

                    task = () -> new CompileTask(
                            Project.fromFlatMap(Collections.emptyMap()),
                            compileArgs.toArray(new String[0])
                    ).execute(compileArgs.toArray(new String[0]));
                }

                Future<Integer> future = executor.submit(task);
                int result = 0;

                // 6.2 支持超时与按 Enter 跳过逻辑
                try {
                    long startTime = System.currentTimeMillis();
                    while (true) {
                        try {
                            result = future.get(200, TimeUnit.MILLISECONDS);
                            break;
                        } catch (TimeoutException te) {
                            if (skipCurrent) {
                                future.cancel(true);
                                if (verbose) {
                                    System.out.println(YELLOW + "! " + demoName + " SKIPPED by Enter" + RESET);
                                } else {
                                    System.out.print(BRIGHT_YEL + "S" + RESET);
                                }
                                break;
                            }
                            if ((System.currentTimeMillis() - startTime) >= timeoutMs) {
                                throw new TimeoutException();
                            }
                        }
                    }
                    if (skipCurrent) continue; // 跳到下一个 demo
                } catch (TimeoutException te) {
                    future.cancel(true);
                    if (!verbose) {
                        System.out.print(BRIGHT_YEL + "?" + RESET);
                    } else {
                        System.out.println(BOLD + BRIGHT_YEL + "✗ " + demoName
                                + " TIMEOUT > " + timeoutMs + "ms" + RESET);
                    }
                    failed.incrementAndGet();
                    failedTests.add(demoName + " (Timeout > " + timeoutMs + "ms)");
                    continue;
                }

                if (skipCurrent) continue; // 跳到下一个 demo

                if (result == 0) {
                    if (!verbose) System.out.print(BRIGHT_GRN + "." + RESET);
                    else System.out.println(BOLD + BRIGHT_RED + "✓ " + demoName + " PASSED" + RESET);
                    passed.incrementAndGet();
                } else {
                    if (!verbose) System.out.print(RED + "F" + RESET);
                    else System.out.println(BOLD + RED + "✗ " + demoName + " FAILED (exit=" + result + ")" + RESET);
                    failed.incrementAndGet();
                    failedTests.add(demoName);
                    if (stopOnFailure) {
                        System.out.println("\n\n" + BOLD + RED + "=== Test stopped due to failure ===" + RESET);
                        break;
                    }
                }

            } catch (Exception e) {
                if (skipCurrent) {
                    if (verbose) {
                        System.out.println(YELLOW + "! " + demoName + " SKIPPED by Enter" + RESET);
                    } else {
                        System.out.print(BRIGHT_YEL + "S" + RESET);
                    }
                    continue;
                }
                if (!verbose) System.out.print(RED + "E" + RESET);
                else
                    System.out.println(BOLD + RED + "✗ " + demoName + " FAILED with exception: " + e.getMessage() + RESET);
                failed.incrementAndGet();
                failedTests.add(demoName + " (Exception: " + e.getMessage() + ")");
                if (stopOnFailure) {
                    System.out.println("\n\n" + BOLD + RED + "=== Test stopped due to exception ===" + RESET);
                    executor.shutdownNow();
                    break;
                }
            } finally {
                executor.shutdownNow();
            }
            System.out.flush();
        }

        // 7. 停止输入监听线程
        inputThreadRunning = false;
        try {
            inputThread.interrupt();
        } catch (Exception ignore) {
        }

        // 8. 输出测试总结
        System.out.println("\n");
        System.out.println(BOLD + CYAN + "=== Test Summary ===" + RESET);
        System.out.println(BRIGHT_GRN + "Passed: " + passed.get() + RESET);
        System.out.println(RED + "Failed: " + failed.get() + RESET);
        System.out.println("Total:  " + (passed.get() + failed.get()));

        if (!failedTests.isEmpty()) {
            System.out.println("\n" + BOLD + YELLOW + "Failed tests:" + RESET);
            for (String f : failedTests) {
                System.out.println("  - " + f);
            }
        }

        return failed.get() > 0 ? 1 : 0;
    }

    /**
     * 在 demo 目录下运行外部 snow 命令：build（必要）+ run（可选）
     *
     * @param snowPath        snow 可执行文件路径
     * @param demoDir         当前 demo 目录
     * @param runAfterCompile 是否编译后运行
     * @param verbose         是否详细输出
     * @return 执行 exit code，0 表示成功
     * @throws Exception 调用过程中的所有异常
     */
    private int runExternalSnowBuildAndMaybeRun(String snowPath, Path demoDir,
                                                boolean runAfterCompile, boolean verbose) throws Exception {
        int build = execExternal(snowPath, demoDir, verbose, "build");
        if (build != 0) return build;
        if (!runAfterCompile) return 0;
        return execExternal(snowPath, demoDir, verbose, "run");
    }

    /**
     * 执行一条外部 snow 命令（工作目录为 demoDir），
     * 输出内容支持 verbose 模式。
     *
     * @param snowPath snow 可执行文件路径
     * @param demoDir  当前 demo 目录
     * @param verbose  是否详细输出
     * @param args     额外参数
     * @return 执行 exit code
     * @throws Exception 启动/等待过程的异常
     */
    private int execExternal(String snowPath, Path demoDir,
                             boolean verbose, String... args) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(snowPath);
        Collections.addAll(cmd, args);

        if (verbose) {
            System.out.println(BRIGHT_CYAN + "CMD (" + demoDir.getFileName() + "): " + String.join(" ", quoteArgs(cmd)) + RESET);
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(demoDir.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        Thread t = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (verbose) System.out.println("  | " + line);
                }
            } catch (IOException ignore) {
            }
        });
        t.setDaemon(true);
        t.start();

        return process.waitFor();
    }
}
