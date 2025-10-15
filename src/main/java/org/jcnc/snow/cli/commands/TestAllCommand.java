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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code TestAllCommand} 实现 CLI 命令 {@code test-all}，
 * 支持批量编译与测试多个目录下的示例工程（通过 --dir 指定；缺省为 playground/Demo）。
 *
 * <p>
 * <b>命令用法：</b>
 * <ul>
 *   <li>{@code snow test-all} —— 在默认目录 {@code playground/Demo} 下编译并运行全部 Demo</li>
 *   <li>{@code snow test-all --dir=playground/Demo} —— 指定父目录，遍历其子目录</li>
 *   <li>{@code snow test-all --dir=playground/Demo/DemoA} —— 指定单个 demo 目录</li>
 *   <li>{@code snow test-all --dir=demo/set1 --dir=demo/set2 --dir=/abs/one} —— 指定多个目录，合并测试</li>
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
 *   <li>支持多个根目录；每个根目录若存在子目录则以子目录为 demo，否则尝试将根目录本身作为单个 demo</li>
 *   <li>如有 project.cloud 且指定了 --snow-path，则调用外部 snow；否则使用内部 CompileTask</li>
 *   <li>每个 demo 支持超时与中途按 Enter 跳过；统计通过/失败并打印总结</li>
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
     * 命令行参数带空格时自动加引号（仅用于打印）
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
        return "Compile and run all demo examples in the specified directories (default: playground/Demo/DemoA).";
    }

    @Override
    public void printUsage() {
        System.out.println("Usage:");
        System.out.println("  snow test-all [options]");
        System.out.println("Options:");
        System.out.println("  --dir=<path>             指定要测试的根目录；可重复使用多次。");
        System.out.println("                           传父目录（遍历其子目录）或直接传单个 demo 目录。");
        System.out.println("                           未指定时默认使用 playground/Demo/DemoA。");
        System.out.println("  --no-run                 仅编译不运行");
        System.out.println("  --verbose                输出详细信息");
        System.out.println("  --stop-on-failure        首次失败/异常时中止（超时不触发）");
        System.out.println("  --timeout=<ms>           设置单个 Demo 超时（毫秒，默认 2000）");
        System.out.println("  --snow-path=<path|auto>  指定 snow(.exe) 路径；auto 自动在 target/release/**/bin 下查找");
        System.out.println("  测试时可随时按 [Enter] 跳过当前 demo，继续后续测试");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  snow test-all");
        System.out.println("  snow test-all --dir=playground/Demo/DemoA");
        System.out.println("  snow test-all --dir=demo/set1 --dir=demo/set2 --dir=/abs/one");
    }

    @Override
    public int execute(String[] args) throws Exception {
        boolean runAfterCompile = true;
        boolean verbose = false;
        boolean stopOnFailure = false;
        String externalSnowPath = null;
        boolean requestedAuto = false; // 用户是否传了 auto
        long timeoutMs = DEFAULT_TIMEOUT_MS;

        // 支持多个 --dir，按声明顺序去重保序
        Set<Path> demoRoots = new LinkedHashSet<>();

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
            } else if (arg.startsWith("--dir=")) {
                String dir = arg.substring("--dir=".length()).trim();
                if (!dir.isEmpty()) {
                    demoRoots.add(Paths.get(dir));
                }
            }
        }

        // 2. 若未显式指定 --dir，则回退默认目录 playground/Demo/DemoA
        if (demoRoots.isEmpty()) {
            demoRoots.add(Paths.get("playground", "Demo", "DemoA"));
        }

        // 3. 校验所有根目录都存在；任意不存在则报错退出（保持原策略的严格性）
        for (Path root : demoRoots) {
            if (!Files.exists(root)) {
                System.err.println(RED + "Demo directory not found: " + root.toAbsolutePath() + RESET);
                return 1;
            }
        }

        // 4. 处理 --snow-path=auto：尝试自动查找 snow 可执行文件
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

        // 5. 检查外部 snow 路径是否有效（当非 auto 或 auto 已成功解析）
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

        // 6. 聚合所有 demo 目录
        List<Path> demoDirs = new ArrayList<>();
        for (Path root : demoRoots) {
            // 6.1 优先收集子目录
            List<Path> children = new ArrayList<>();
            try (var stream = Files.list(root)) {
                stream.filter(Files::isDirectory)
                        .sorted()
                        .forEach(children::add);
            } catch (IOException e) {
                System.err.println(RED + "Failed to list directory: " + root.toAbsolutePath() + " - " + e.getMessage() + RESET);
                return 1;
            }

            if (children.isEmpty()) {
                // 6.2 无子目录：判断 root 自身是否就是一个 demo
                boolean looksLikeSingleDemo =
                        Files.exists(root.resolve("project.cloud")) ||
                                Files.exists(root.resolve("project.snow")) ||
                                Files.exists(root.resolve("src"));
                if (looksLikeSingleDemo) {
                    demoDirs.add(root);
                } else {
                    System.out.println(BRIGHT_YEL + "No demo directories found in " + root.toAbsolutePath() + RESET);
                }
            } else {
                demoDirs.addAll(children);
            }
        }

        // 去重（跨根目录可能出现同一物理路径被重复指定）
        demoDirs = new ArrayList<>(new LinkedHashSet<>(demoDirs));
        // 总体排序（按路径字典序）
        demoDirs.sort(Comparator.comparing(Path::toString));

        if (demoDirs.isEmpty()) {
            System.out.println(BRIGHT_YEL + "No demo directories to test." + RESET);
            return 0;
        }

        System.out.println(BRIGHT_CYAN + "Found " + demoDirs.size() + " demo "
                + (demoDirs.size() == 1 ? "directory" : "directories")
                + " from " + demoRoots.size() + " root "
                + (demoRoots.size() == 1 ? "path" : "paths")
                + ". Starting tests..." + RESET + "\n");
        System.out.println(BRIGHT_YEL + "Timeout per demo: " + timeoutMs + " ms" + RESET);
        System.out.println(BRIGHT_CYAN + "[提示] 测试进行时可随时按 [Enter] 跳过当前 demo" + RESET);

        AtomicInteger passed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        List<String> failedTests = new ArrayList<>();

        // 7. 启动输入监听线程：检测 [Enter] 跳过当前 demo
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

        // 8. 遍历测试每个 Demo 目录
        for (Path demoDir : demoDirs) {
            skipCurrent = false; // 每个 demo 前重置

            String demoName = demoDir.getFileName().toString();
            if (verbose)
                System.out.println(CYAN + "Testing " + demoName + " (" + demoDir.toAbsolutePath() + ")..." + RESET);

            boolean hasCloud = Files.exists(demoDir.resolve("project.cloud"));
            ExecutorService executor = Executors.newSingleThreadExecutor();

            try {
                Callable<Integer> task;

                // 8.1 优先尝试外部 CLI 模式，否则回退内部 CompileTask
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

                // 8.2 支持超时与按 Enter 跳过逻辑
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

        // 9. 停止输入监听线程
        inputThreadRunning = false;
        try {
            inputThread.interrupt();
        } catch (Exception ignore) {
        }

        // 10. 输出测试总结
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
     */
    private int runExternalSnowBuildAndMaybeRun(String snowPath, Path demoDir,
                                                boolean runAfterCompile, boolean verbose) throws Exception {
        int build = execExternal(snowPath, demoDir, verbose, "build");
        if (build != 0) return build;
        if (!runAfterCompile) return 0;
        return execExternal(snowPath, demoDir, verbose, "run");
    }

    /**
     * 执行一条外部 snow 命令（工作目录为 demoDir），支持 verbose 输出。
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
