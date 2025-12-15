package org.jcnc.snow.cli.commands;

import org.jcnc.snow.cli.api.CLICommand;
import org.jcnc.snow.pkg.model.Project;
import org.jcnc.snow.pkg.tasks.CompileTask;

import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.*;

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

    private enum OutputMode {
        NONE,
        FAIL,
        ALWAYS
    }

    private enum FailureKind {
        COMPILE,
        RUNTIME,
        OTHER
    }

    private record OutputSection(String title, List<String> cmd, String stdout, String stderr, boolean truncated) {
    }

    private record DemoRunResult(int exitCode, List<OutputSection> sections) {
    }

    private record CapturedResult<T>(T value, String stdout, String stderr, boolean truncated) {
    }

    private static final class BoundedOutputStream extends OutputStream {
        private final long maxBytes;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private long written = 0;
        private boolean truncated = false;

        private BoundedOutputStream(long maxBytes) {
            this.maxBytes = Math.max(0, maxBytes);
        }

        @Override
        public void write(int b) {
            if (maxBytes == 0) {
                truncated = true;
                return;
            }
            if (written < maxBytes) {
                buffer.write(b);
            } else {
                truncated = true;
            }
            written++;
        }

        @Override
        public void write(byte[] b, int off, int len) {
            if (b == null || len <= 0) return;
            if (maxBytes == 0) {
                truncated = true;
                written += len;
                return;
            }
            long remaining = maxBytes - Math.min(written, maxBytes);
            if (remaining > 0) {
                int toWrite = (int) Math.min(remaining, len);
                buffer.write(b, off, toWrite);
            }
            if (written + len > maxBytes) truncated = true;
            written += len;
        }

        public boolean truncated() {
            return truncated;
        }

        public String asString() {
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }

    /**
     * 跳过当前 demo 的全局标志，监听输入线程通过 [Enter] 设置
     */
    private static volatile boolean skipCurrent = false;
    /**
     * 监听输入线程的运行标志
     */
    private static volatile boolean inputThreadRunning = true;

    /**
     * Capture mode temporarily redirects {@link System#out}/{@link System#err}. If a demo times out and the task is
     * cancelled, the worker thread may not restore the streams promptly (or at all), causing subsequent CLI output
     * (including the final summary) to disappear. We defensively restore the control streams in the main thread.
     */
    private static void ensureControlStreams(PrintStream out, PrintStream err) {
        if (System.out != out) System.setOut(out);
        if (System.err != err) System.setErr(err);
    }

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

    private static void printStreamBlock(PrintStream out, String name, String content, boolean truncated) {
        out.println("  [" + name + "]");
        if (content == null || content.isBlank()) {
            out.println("    (no output)");
            return;
        }
        String normalized = content.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n", -1);
        int last = lines.length;
        while (last > 0 && lines[last - 1].isEmpty()) last--;
        for (int i = 0; i < last; i++) {
            out.println("    | " + lines[i]);
        }
        if (truncated) {
            out.println("    | ...(truncated)...");
        }
    }

    private static void printSection(PrintStream out, String demoName, OutputSection sec) {
        out.println(BRIGHT_CYAN + "----- " + demoName + " :: " + sec.title() + " -----" + RESET);
        if (sec.cmd() != null && !sec.cmd().isEmpty()) {
            out.println("  $ " + String.join(" ", quoteArgs(sec.cmd())));
        }
        printStreamBlock(out, "stdout", sec.stdout(), sec.truncated());
        printStreamBlock(out, "stderr", sec.stderr(), sec.truncated());
        out.println(BRIGHT_CYAN + "----- end " + demoName + " :: " + sec.title() + " -----" + RESET);
    }

    private static boolean containsAnyIgnoreCase(String haystack, List<String> needles) {
        if (haystack == null || haystack.isEmpty()) return false;
        String lower = haystack.toLowerCase(Locale.ROOT);
        for (String n : needles) {
            if (n == null || n.isEmpty()) continue;
            if (lower.contains(n.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static FailureKind classifyFailure(boolean usedExternalSnow, boolean runAfterCompile, DemoRunResult result) {
        if (usedExternalSnow && result != null) {
            OutputSection build = null;
            OutputSection run = null;
            for (OutputSection sec : result.sections()) {
                if (sec == null || sec.title() == null) continue;
                if (sec.title().startsWith("build")) build = sec;
                else if (sec.title().startsWith("run")) run = sec;
            }
            if (build != null && extractExit(build.title()) != 0) return FailureKind.COMPILE;
            if (runAfterCompile && run != null && extractExit(run.title()) != 0) return FailureKind.RUNTIME;
            return FailureKind.OTHER;
        }

        if (!runAfterCompile) return FailureKind.COMPILE;

        if (result == null || result.sections() == null || result.sections().isEmpty()) return FailureKind.OTHER;
        OutputSection sec = result.sections().getFirst();
        String out = (sec.stdout() == null ? "" : sec.stdout()) + "\n" + (sec.stderr() == null ? "" : sec.stderr());

        List<String> runtimeMarkers = List.of(
                "command execution error",
                "runtime builtin syscall failed",
                "snowpanic",
                "vm error",
                "panic",
                "栈溢出",
                "运行时"
        );
        if (containsAnyIgnoreCase(out, runtimeMarkers)) return FailureKind.RUNTIME;

        List<String> compileMarkers = List.of(
                "语义分析发现",
                "语法分析发现",
                "semantic",
                "syntax",
                "lexer",
                "parser",
                "编译失败",
                "compile failed"
        );
        if (containsAnyIgnoreCase(out, compileMarkers)) return FailureKind.COMPILE;

        // Internal mode cannot always distinguish (no debug logs). Prefer treating unknown non-zero exits as runtime failures.
        return FailureKind.RUNTIME;
    }

    private static CapturedResult<Integer> captureStdoutStderr(Callable<Integer> action, long maxBytes) throws Exception {
        PrintStream prevOut = System.out;
        PrintStream prevErr = System.err;

        BoundedOutputStream outBuf = new BoundedOutputStream(maxBytes);
        BoundedOutputStream errBuf = new BoundedOutputStream(maxBytes);

        try (PrintStream psOut = new PrintStream(outBuf, true, StandardCharsets.UTF_8);
             PrintStream psErr = new PrintStream(errBuf, true, StandardCharsets.UTF_8)) {
            System.setOut(psOut);
            System.setErr(psErr);
            int rc = action.call();
            return new CapturedResult<>(rc, outBuf.asString(), errBuf.asString(), outBuf.truncated() || errBuf.truncated());
        } finally {
            System.setOut(prevOut);
            System.setErr(prevErr);
        }
    }

    private static void pump(InputStream in, BoundedOutputStream sink, PrintStream streamOut) {
        byte[] buf = new byte[4096];
        try (in) {
            int n;
            while ((n = in.read(buf)) >= 0) {
                sink.write(buf, 0, n);
                if (streamOut != null) {
                    streamOut.print(new String(buf, 0, n, StandardCharsets.UTF_8));
                    streamOut.flush();
                }
            }
        } catch (IOException ignore) {
        }
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
        System.out.println("  --show-output=<mode>     输出分块：always | fail | none（默认：verbose=always，否则=fail）");
        System.out.println("  --output-max-kb=<n>      每个 stdout/stderr 最大采集大小（KiB，默认 256；0=不采集）");
        System.out.println("  --stream-output          保留旧行为：实时打印输出（不分块）");
        System.out.println("  测试时可随时按 [Enter] 跳过当前 demo，继续后续测试");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  snow test-all");
        System.out.println("  snow test-all --dir=playground/Demo/DemoA");
        System.out.println("  snow test-all --dir=demo/set1 --dir=demo/set2 --dir=/abs/one");
    }

    @Override
    public int execute(String[] args) throws Exception {
        final PrintStream controlOut = System.out;
        final PrintStream controlErr = System.err;
        ensureControlStreams(controlOut, controlErr);

        boolean runAfterCompile = true;
        boolean verbose = false;
        boolean stopOnFailure = false;
        String externalSnowPath = null;
        boolean requestedAuto = false; // 用户是否传了 auto
        long timeoutMs = DEFAULT_TIMEOUT_MS;
        OutputMode outputMode = null;   // default depends on --verbose
        boolean streamOutput = false;   // legacy live streaming (not block formatted)
        long outputMaxBytes = 256 * 1024; // 256KiB per stream

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
            } else if (arg.startsWith("--show-output=")) {
                String v = arg.substring("--show-output=".length()).trim().toLowerCase(Locale.ROOT);
                outputMode = switch (v) {
                    case "always" -> OutputMode.ALWAYS;
                    case "fail", "failure" -> OutputMode.FAIL;
                    case "none", "never" -> OutputMode.NONE;
                    default -> null;
                };
                if (outputMode == null) {
                    System.err.println(RED + "Invalid --show-output value: " + arg + RESET);
                    return 1;
                }
            } else if (arg.startsWith("--output-max-kb=")) {
                try {
                    long kb = Long.parseLong(arg.substring("--output-max-kb=".length()).trim());
                    outputMaxBytes = Math.max(0, kb) * 1024L;
                } catch (NumberFormatException e) {
                    System.err.println(RED + "Invalid --output-max-kb value: " + arg + RESET);
                    return 1;
                }
            } else if ("--stream-output".equals(arg)) {
                streamOutput = true;
            } else if (arg.startsWith("--dir=")) {
                String dir = arg.substring("--dir=".length()).trim();
                if (!dir.isEmpty()) {
                    demoRoots.add(Paths.get(dir));
                }
            }
        }

        if (outputMode == null) {
            outputMode = verbose ? OutputMode.ALWAYS : OutputMode.FAIL;
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

        int passed = 0;
        int skipped = 0;
        int timeouts = 0;
        int exceptions = 0;
        int compileFailed = 0;
        int runtimeFailed = 0;
        int otherFailed = 0;

        List<String> skippedTests = new ArrayList<>();
        List<String> timeoutTests = new ArrayList<>();
        List<String> compileFailedTests = new ArrayList<>();
        List<String> runtimeFailedTests = new ArrayList<>();
        List<String> otherFailedTests = new ArrayList<>();
        List<String> exceptionTests = new ArrayList<>();

        int executed = 0;
        boolean stoppedEarly = false;

        // 7. 启动输入监听线程：检测 [Enter] 跳过当前 demo
        Thread inputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                while (inputThreadRunning) {
                    String line = reader.readLine();
                    if (line == null) break;
                    skipCurrent = true;
                    controlErr.println(BRIGHT_YEL + "[Enter] Skip current demo and continue..." + RESET);
                }
            } catch (Exception ignore) {
            }
        }, "Demo-SkipListener");
        inputThread.setDaemon(true);
        inputThread.start();

        // 8. 遍历测试每个 Demo 目录
        for (Path demoDir : demoDirs) {
            skipCurrent = false; // 每个 demo 前重置
            ensureControlStreams(controlOut, controlErr);

            String demoName = demoDir.getFileName().toString();
            if (verbose)
                controlOut.println(CYAN + "Testing " + demoName + " (" + demoDir.toAbsolutePath() + ")..." + RESET);

            boolean hasCloud = Files.exists(demoDir.resolve("project.cloud"));
            ExecutorService executor = Executors.newSingleThreadExecutor();

            try {
                executed++;
                Callable<DemoRunResult> task;
                boolean usedExternalSnow = hasCloud && externalSnowPath != null;

                // 8.1 优先尝试外部 CLI 模式，否则回退内部 CompileTask
                if (usedExternalSnow) {
                    String finalSnow = externalSnowPath;
                    boolean finalRun = runAfterCompile;
                    boolean finalVerbose = verbose;
                    boolean finalStream = streamOutput;
                    long finalOutputMaxBytes = outputMaxBytes;
                    task = () -> runExternalSnowBuildAndMaybeRun(finalSnow, demoDir, finalRun, finalVerbose, finalStream, finalOutputMaxBytes);
                } else {
                    if (!hasCloud && externalSnowPath != null && verbose) {
                        System.out.println(BRIGHT_CYAN + "No project.cloud found; fallback to internal CompileTask for "
                                + demoName + RESET);
                    }
                    boolean finalStreamOutput = streamOutput;
                    List<String> compileArgs = new ArrayList<>();
                    compileArgs.add("-d");
                    compileArgs.add(demoDir.toString());
                    compileArgs.add("-o");
                    compileArgs.add("target/" + demoName);
                    if (runAfterCompile) compileArgs.add("run");

                    long finalOutputMaxBytes = outputMaxBytes;
                    task = () -> {
                        if (finalStreamOutput) {
                            int rc = new CompileTask(
                                    Project.fromFlatMap(Collections.emptyMap()),
                                    compileArgs.toArray(new String[0])
                            ).execute(compileArgs.toArray(new String[0]));
                            return new DemoRunResult(rc, List.of());
                        }
                        CapturedResult<Integer> cap = captureStdoutStderr(() -> new CompileTask(
                                Project.fromFlatMap(Collections.emptyMap()),
                                compileArgs.toArray(new String[0])
                        ).execute(compileArgs.toArray(new String[0])), finalOutputMaxBytes);
                        OutputSection sec = new OutputSection("internal CompileTask", List.copyOf(compileArgs), cap.stdout(), cap.stderr(), cap.truncated());
                        return new DemoRunResult(cap.value(), List.of(sec));
                    };
                }

                Future<DemoRunResult> future = executor.submit(task);
                DemoRunResult result = null;

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
                                    controlOut.println(YELLOW + "! " + demoName + " SKIPPED by Enter" + RESET);
                                } else {
                                    controlOut.print(BRIGHT_YEL + "S" + RESET);
                                }
                                break;
                            }
                            if ((System.currentTimeMillis() - startTime) >= timeoutMs) {
                                throw new TimeoutException();
                            }
                        }
                    }
                    if (skipCurrent) {
                        skipped++;
                        skippedTests.add(demoName);
                        continue;
                    }
                } catch (TimeoutException te) {
                    future.cancel(true);
                    ensureControlStreams(controlOut, controlErr);
                    if (!verbose) {
                        controlOut.print(BRIGHT_YEL + "?" + RESET);
                    } else {
                        controlOut.println(BOLD + BRIGHT_YEL + "✗ " + demoName
                                + " TIMEOUT > " + timeoutMs + "ms" + RESET);
                    }
                    timeouts++;
                    timeoutTests.add(demoName + " (Timeout > " + timeoutMs + "ms)");
                    continue;
                }

                if (skipCurrent) {
                    skipped++;
                    skippedTests.add(demoName);
                    continue;
                }

                int exitCode = (result == null) ? 1 : result.exitCode();
                boolean showOutput = verbose && switch (outputMode) {
                    case NONE -> false;
                    case FAIL -> exitCode != 0;
                    case ALWAYS -> true;
                };
                if (showOutput && result != null && !streamOutput) {
                    for (OutputSection sec : result.sections()) {
                        printSection(controlOut, demoName, sec);
                    }
                }

                if (exitCode == 0) {
                    if (!verbose) controlOut.print(BRIGHT_GRN + "." + RESET);
                    else controlOut.println(BOLD + BRIGHT_GRN + "✓ " + demoName + " PASSED" + RESET);
                    passed++;
                } else {
                    if (!verbose) controlOut.print(RED + "F" + RESET);
                    else controlOut.println(BOLD + RED + "✗ " + demoName + " FAILED (exit=" + exitCode + ")" + RESET);
                    FailureKind kind = classifyFailure(usedExternalSnow, runAfterCompile, result);
                    switch (kind) {
                        case COMPILE -> {
                            compileFailed++;
                            compileFailedTests.add(demoName + " (exit=" + exitCode + ")");
                        }
                        case RUNTIME -> {
                            runtimeFailed++;
                            runtimeFailedTests.add(demoName + " (exit=" + exitCode + ")");
                        }
                        case OTHER -> {
                            otherFailed++;
                            otherFailedTests.add(demoName + " (exit=" + exitCode + ")");
                        }
                    }
                    if (stopOnFailure) {
                        controlOut.println("\n\n" + BOLD + RED + "=== Test stopped due to failure ===" + RESET);
                        stoppedEarly = true;
                        break;
                    }
                }

            } catch (Exception e) {
                ensureControlStreams(controlOut, controlErr);
                if (skipCurrent) {
                    if (verbose) {
                        controlOut.println(YELLOW + "! " + demoName + " SKIPPED by Enter" + RESET);
                    } else {
                        controlOut.print(BRIGHT_YEL + "S" + RESET);
                    }
                    skipped++;
                    skippedTests.add(demoName);
                    continue;
                }
                if (!verbose) controlOut.print(RED + "E" + RESET);
                else
                    controlOut.println(BOLD + RED + "✗ " + demoName + " FAILED with exception: " + e.getMessage() + RESET);
                exceptions++;
                exceptionTests.add(demoName + " (Exception: " + e.getMessage() + ")");
                    if (stopOnFailure) {
                        controlOut.println("\n\n" + BOLD + RED + "=== Test stopped due to exception ===" + RESET);
                        executor.shutdownNow();
                        stoppedEarly = true;
                        break;
                    }
            } finally {
                executor.shutdownNow();
                ensureControlStreams(controlOut, controlErr);
            }
            controlOut.flush();
        }

        // 9. 停止输入监听线程
        inputThreadRunning = false;
        try {
            inputThread.interrupt();
        } catch (Exception ignore) {
        }

        // 10. 输出测试总结
        ensureControlStreams(controlOut, controlErr);
        System.out.println("\n");
        System.out.println(BOLD + CYAN + "=== Test Summary ===" + RESET);
        int failed = compileFailed + runtimeFailed + otherFailed;
        int total = passed + skipped + timeouts + exceptions + failed;
        System.out.println(BRIGHT_GRN + "Passed:   " + passed + RESET);
        System.out.println(BRIGHT_YEL + "Skipped:  " + skipped + RESET);
        System.out.println(BRIGHT_YEL + "Timeout:  " + timeouts + RESET);
        System.out.println(RED + "Failed:   " + failed + RESET);
        if (failed > 0) {
            System.out.println(RED + "  - Compile: " + compileFailed + RESET);
            System.out.println(RED + "  - Runtime: " + runtimeFailed + RESET);
            System.out.println(RED + "  - Other:   " + otherFailed + RESET);
        }
        System.out.println(RED + "Exception:" + " " + exceptions + RESET);
        System.out.println("Total:    " + total);
        System.out.println("Executed: " + executed + " / " + demoDirs.size() + (stoppedEarly ? " (stopped early)" : ""));

        if (!timeoutTests.isEmpty()) {
            System.out.println("\n" + BOLD + BRIGHT_YEL + "Timeouts:" + RESET);
            for (String t : timeoutTests) System.out.println("  - " + t);
        }
        if (!exceptionTests.isEmpty()) {
            System.out.println("\n" + BOLD + RED + "Exceptions:" + RESET);
            for (String t : exceptionTests) System.out.println("  - " + t);
        }
        if (!compileFailedTests.isEmpty()) {
            System.out.println("\n" + BOLD + RED + "Compile failures:" + RESET);
            for (String t : compileFailedTests) System.out.println("  - " + t);
        }
        if (!runtimeFailedTests.isEmpty()) {
            System.out.println("\n" + BOLD + RED + "Runtime failures:" + RESET);
            for (String t : runtimeFailedTests) System.out.println("  - " + t);
        }
        if (!otherFailedTests.isEmpty()) {
            System.out.println("\n" + BOLD + YELLOW + "Other failures:" + RESET);
            for (String t : otherFailedTests) System.out.println("  - " + t);
        }
        if (!skippedTests.isEmpty()) {
            System.out.println("\n" + BOLD + YELLOW + "Skipped:" + RESET);
            for (String t : skippedTests) System.out.println("  - " + t);
        }

        return (failed + timeouts + exceptions) > 0 ? 1 : 0;
    }

    /**
     * 在 demo 目录下运行外部 snow 命令：build（必要）+ run（可选）
     */
    private DemoRunResult runExternalSnowBuildAndMaybeRun(String snowPath,
                                                          Path demoDir,
                                                          boolean runAfterCompile,
                                                          boolean verbose,
                                                          boolean streamOutput,
                                                          long outputMaxBytes) throws Exception {
        List<OutputSection> sections = new ArrayList<>();
        OutputSection build = execExternalCaptured(snowPath, demoDir, "build", verbose, streamOutput, outputMaxBytes);
        sections.add(build);
        if (build.cmd() == null) return new DemoRunResult(1, sections);
        int buildExit = extractExit(build.title());
        if (buildExit != 0) return new DemoRunResult(buildExit, sections);
        if (!runAfterCompile) return new DemoRunResult(0, sections);

        OutputSection run = execExternalCaptured(snowPath, demoDir, "run", verbose, streamOutput, outputMaxBytes);
        sections.add(run);
        return new DemoRunResult(extractExit(run.title()), sections);
    }

    /**
     * 执行一条外部 snow 命令（工作目录为 demoDir），支持 verbose 输出。
     */
    private OutputSection execExternalCaptured(String snowPath,
                                               Path demoDir,
                                               String subcommand,
                                               boolean verbose,
                                               boolean streamOutput,
                                               long outputMaxBytes) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(snowPath);
        cmd.add(subcommand);

        if (verbose) {
            System.out.println(BRIGHT_CYAN + "CMD (" + demoDir.getFileName() + "): " + String.join(" ", quoteArgs(cmd)) + RESET);
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(demoDir.toFile());
        pb.redirectErrorStream(false);
        Process process = pb.start();

        BoundedOutputStream stdout = new BoundedOutputStream(outputMaxBytes);
        BoundedOutputStream stderr = new BoundedOutputStream(outputMaxBytes);

        PrintStream live = (verbose && streamOutput) ? System.out : null;
        Thread tOut = new Thread(() -> pump(process.getInputStream(), stdout, live), "snow-stdout");
        Thread tErr = new Thread(() -> pump(process.getErrorStream(), stderr, live), "snow-stderr");
        tOut.setDaemon(true);
        tErr.setDaemon(true);
        tOut.start();
        tErr.start();

        int exit;
        try {
            exit = process.waitFor();
        } catch (InterruptedException ie) {
            process.destroyForcibly();
            throw ie;
        } finally {
            try {
                tOut.join(200);
                tErr.join(200);
            } catch (InterruptedException ignore) {
            }
        }

        return new OutputSection(subcommand + " (exit=" + exit + ")", List.copyOf(cmd),
                stdout.asString(), stderr.asString(), stdout.truncated() || stderr.truncated());
    }

    private static int extractExit(String title) {
        // title: "<cmd> (exit=<n>)"
        int idx = title.lastIndexOf("exit=");
        if (idx < 0) return 1;
        int end = title.indexOf(')', idx);
        String num = (end < 0) ? title.substring(idx + 5) : title.substring(idx + 5, end);
        try {
            return Integer.parseInt(num.trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
