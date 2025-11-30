package org.jcnc.snow.pkg.tasks;

import org.jcnc.snow.cli.commands.CompileCommand;
import org.jcnc.snow.common.Mode;
import org.jcnc.snow.common.SnowConfig;
import org.jcnc.snow.compiler.backend.alloc.RegisterAllocator;
import org.jcnc.snow.compiler.backend.builder.VMCodeGenerator;
import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.backend.generator.InstructionGeneratorProvider;
import org.jcnc.snow.compiler.backend.utils.OpHelper;
import org.jcnc.snow.compiler.ir.builder.core.IRProgramBuilder;
import org.jcnc.snow.compiler.ir.core.IRFunction;
import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.core.IRProgram;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.lexer.core.LexerEngine;
import org.jcnc.snow.compiler.parser.ast.base.Node;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.core.ParserEngine;
import org.jcnc.snow.compiler.semantic.core.SemanticAnalyzerRunner;
import org.jcnc.snow.pkg.model.Project;
import org.jcnc.snow.vm.VMLauncher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.jcnc.snow.common.SnowConfig.print;

/**
 * CompileTask 负责将 .snow 源文件编译为 VM 字节码（.water 文件），是命令行编译任务的具体实现。
 * <p>
 * 主要功能：
 * <ul>
 *     <li>递归收集目录下所有 .snow 文件</li>
 *     <li>支持命令行参数自定义输出文件名、目录、是否自动运行 VM</li>
 *     <li>输出编译各阶段的关键信息（源码、IR、VM code）</li>
 *     <li>可选自动运行生成的字节码</li>
 * </ul>
 */
public record CompileTask(Project project, String[] args) implements Task {

    /**
     * 构造方法，使用空参数数组。
     *
     * @param project 当前项目对象
     */
    public CompileTask(Project project) {
        this(project, new String[0]); // 调用主构造方法，传入空参数数组
    }

    /**
     * 推断输出 .water 文件的路径。
     * <ul>
     *   <li>如指定 outName，则用该值作为基名</li>
     *   <li>否则如指定目录，则用目录名作为基名</li>
     *   <li>否则如仅有一个源文件，则用文件名（去掉 .snow）</li>
     *   <li>否则用默认 "program"</li>
     * </ul>
     */
    private static Path deriveOutputPath(List<Path> sources, String outName, Path dir) {
        Path outputPath;
        if (outName != null) { // 优先使用用户指定的输出名
            outputPath = Path.of(outName);
            // 如果没有扩展名或者扩展名不是.water，则添加.water扩展名
            if (!outputPath.toString().endsWith(".water")) {
                outputPath = outputPath.resolveSibling(outputPath.getFileName() + ".water");
            }
        } else if (dir != null) { // 如果指定了目录，则使用目录名作为基名
            String base = dir.getFileName().toString();
            outputPath = dir.resolve(base + ".water");
        } else if (sources.size() == 1) { // 单个源文件时，使用源文件名（去掉扩展名）
            String base = sources.getFirst()
                    .getFileName()
                    .toString()
                    .replaceFirst("\\.snow$", "");
            outputPath = Path.of(base + ".water");
        } else { // 默认情况
            outputPath = Path.of("program.water");
        }
        return outputPath; // 返回完整的输出路径
    }

    /**
     * 将 main 或 *.main 函数调整到列表首位，确保入口 PC=0。
     */
    private static IRProgram reorderForEntry(IRProgram in) {
        List<IRFunction> ordered = new ArrayList<>(in.functions()); // 拷贝函数列表
        for (int i = 0; i < ordered.size(); i++) {
            String fn = ordered.get(i).name();
            // 找到 main 或 <模块名>.main 函数，交换到首位
            if ("main".equals(fn) || fn.endsWith(".main")) {
                Collections.swap(ordered, 0, i);
                break;
            }
        }
        IRProgram out = new IRProgram();
        ordered.forEach(out::add); // 按新顺序重新加入 IRProgram
        return out;
    }

    private static String fromDemoXX(Path src) {
        for (int i = 0; i < src.getNameCount(); i++) {
            String part = src.getName(i).toString();
            if (part.matches("Demo\\d+")) { // 精确匹配 DemoXX 形式
                return src.subpath(i, src.getNameCount()).toString();
            }
        }
        // fallback，只返回文件名
        return src.getFileName().toString();
    }

    // 1. 从项目根/源码目录向上找最近 lib
    private static Path findNearestLibDir(Path start) {
        String env = System.getenv("SNOW_LIB");
        if (env == null) env = System.getProperty("snow.lib");
        if (env != null) {
            Path p = Path.of(env);
            if (Files.isDirectory(p)) return p;
        }
        if (start == null) return null;
        Path cur = start.toAbsolutePath();
        while (cur != null) {
            Path lib = cur.resolve("lib");
            if (Files.isDirectory(lib)) return lib;
            cur = cur.getParent();
        }

        // 如果在项目目录中找不到lib，则尝试查找Snow SDK目录
        return findSnowSdkLibDir();
    }

    /**
     * 查找Snow SDK安装目录下的lib目录
     * 查找顺序：
     * 1. SNOW_HOME环境变量指定的目录
     * 2. snow.home系统属性指定的目录
     * 3. 可执行文件所在目录的上级目录
     *
     * @return Snow SDK的lib目录路径，如果找不到则返回null
     */
    private static Path findSnowSdkLibDir() {
        // 1. 检查SNOW_HOME环境变量
        String snowHome = System.getenv("SNOW_HOME");
        if (snowHome != null) {
            Path sdkLib = Path.of(snowHome).resolve("lib");
            if (Files.isDirectory(sdkLib)) {
                return sdkLib;
            }
        }

        // 2. 检查snow.home系统属性
        String snowHomeProperty = System.getProperty("snow.home");
        if (snowHomeProperty != null) {
            Path sdkLib = Path.of(snowHomeProperty).resolve("lib");
            if (Files.isDirectory(sdkLib)) {
                return sdkLib;
            }
        }

        // 3. 尝试从可执行文件路径推断SDK目录
        try {
            // 获取当前JAR文件或类文件的路径
            Path classPath = Paths.get(CompileTask.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());

            // 向上查找可能的SDK目录结构
            Path current = classPath.toAbsolutePath().getParent();
            while (current != null) {
                // 检查是否存在lib目录
                Path libDir = current.resolve("lib");
                if (Files.isDirectory(libDir)) {
                    return libDir;
                }
                current = current.getParent();
            }
        } catch (Exception e) {
            // 忽略异常，继续其他查找方式
        }

        return null;
    }

    // 2. 收集 lib 下全部 .snow 文件
    private static List<Path> collectSnowFiles(Path dir) throws IOException {
        List<Path> ret = new ArrayList<>();
        try (Stream<Path> s = Files.walk(dir)) {
            s.filter(p -> p.toString().endsWith(".snow"))
                    .sorted()
                    .forEach(ret::add);
        }
        return ret;
    }

    // 3. 从源码提取 import 模块名（支持 import: a.b.c, x.y, Foo）——逗号分隔每个都取最后一段
    private static Set<String> extractImportsFromText(String code) {
        Set<String> r = new LinkedHashSet<>();
        Pattern p = Pattern.compile("(?m)^\\s*import:\\s*([A-Za-z0-9_\\.,\\s]+)\\s*$");
        Matcher m = p.matcher(code);
        while (m.find()) {
            String group = m.group(1).trim();
            for (String entry : group.split(",")) {
                String mod = entry.trim();
                if (mod.isEmpty()) continue;
                String[] parts = mod.split("\\.");
                r.add(parts[parts.length - 1]);
            }
        }
        return r;
    }

    // 4. 读 module: xxx
    private static String readModuleName(Path file) throws IOException {
        String head = Files.readString(file, StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("(?m)^\\s*module:\\s*([A-Za-z0-9_]+)\\s*$").matcher(head);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    // 5. 建索引: 模块名 -> 文件路径
    private static Map<String, Path> indexLibModules(Path libDir) throws IOException {
        Map<String, Path> idx = new LinkedHashMap<>();
        for (Path f : collectSnowFiles(libDir)) {
            String mod = readModuleName(f);
            if (mod != null && !idx.containsKey(mod)) {
                idx.put(mod, f);
            }
        }
        return idx;
    }

    // 6. 读取库文件的 import
    private static Set<String> readImportsOfLibFile(Path libFile) throws IOException {
        String code = Files.readString(libFile, StandardCharsets.UTF_8);
        return extractImportsFromText(code);
    }

    // 7. 闭包：从直接 import 出发递归抓取传递依赖
    private static List<Path> resolveNeededLibFiles(Path libDir, Set<String> projectImports) throws IOException {
        if (libDir == null || projectImports.isEmpty()) return List.of();
        Map<String, Path> idx = indexLibModules(libDir);
        Set<Path> needed = new LinkedHashSet<>();
        Deque<String> q = new ArrayDeque<>(projectImports);
        Set<String> seen = new HashSet<>();

        while (!q.isEmpty()) {
            String mod = q.removeFirst();
            if (!seen.add(mod)) continue;
            Path f = idx.get(mod);
            if (f == null) continue; // 没有的模块交语义报错
            if (needed.add(f)) {
                for (String dep : readImportsOfLibFile(f)) {
                    if (!seen.contains(dep)) q.addLast(dep);
                }
            }
        }
        return new ArrayList<>(needed);
    }

    @Override
    public void run() throws Exception {
        execute(this.args);
    }

    /**
     * 编译 .snow 文件为 .water 字节码文件，可选自动运行。
     *
     * @param args 命令行参数
     * @return 0 成功，非0失败
     */
    public int execute(String[] args) throws Exception {

        boolean runAfterCompile = false; // 是否编译后自动运行
        String outputName = null;        // 用户指定的输出文件名
        Path dir = null;                 // 源文件目录
        List<Path> sources = new ArrayList<>(); // 源文件列表

        // 解析命令行参数
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "run" -> runAfterCompile = true; // run 表示编译后运行 VM
                case "--debug" -> SnowConfig.MODE = Mode.DEBUG; // 开启 debug 模式
                case "--trace" -> SnowConfig.setInstructionTraceEnabled(true); // 输出指令级 trace
                case "-o" -> { // 指定输出文件名
                    if (i + 1 < args.length) outputName = args[++i];
                    else {
                        System.err.println("Missing argument for -o");
                        new CompileCommand().printUsage();
                        return 1;
                    }
                }
                case "-d" -> { // 指定目录
                    if (i + 1 < args.length) dir = Path.of(args[++i]);
                    else {
                        System.err.println("Missing argument for -d");
                        new CompileCommand().printUsage();
                        return 1;
                    }
                }
                default -> {
                    // 识别 .snow 源文件，否则报错
                    if (args[i].endsWith(".snow")) sources.add(Path.of(args[i]));
                    else {
                        System.err.println("Unknown option or file: " + args[i]);
                        new CompileCommand().printUsage();
                        return 1;
                    }
                }
            }
        }

        // 如果指定了目录，则递归收集 .snow 文件
        if (dir != null) {
            if (!Files.isDirectory(dir)) {
                System.err.println("Not a directory: " + dir);
                return 1;
            }
            try (var stream = Files.walk(dir)) {
                stream.filter(p -> p.toString().endsWith(".snow"))
                        .sorted()
                        .forEach(sources::add);
            }
        }

        // 校验输入文件
        if (sources.isEmpty()) {
            System.err.println("No .snow source files found.");
            return 1;
        }
        if (sources.size() > 1 && outputName == null && dir == null) {
            System.err.println("Please specify output name using -o <name>");
            return 1;
        }

        print("## 编译器输出");
        print("### Snow 源代码");

        // 1. 先处理用户源码，收集 import
        List<Node> projectAst = new ArrayList<>();
        Set<String> projectImports = new LinkedHashSet<>();

        for (Path src : sources) {
            String code = Files.readString(src, StandardCharsets.UTF_8);
            print("#### " + fromDemoXX(src));
            print(code);

            projectImports.addAll(extractImportsFromText(code));
            LexerEngine lex = new LexerEngine(code, src.toString());
            if (!lex.getErrors().isEmpty()) return 1;
            ParserContext ctx = new ParserContext(lex.getAllTokens(), src.toString());
            projectAst.addAll(new ParserEngine(ctx).parse());
        }

        // 2. 只加载需要的标准库（含递归依赖）
        Path baseDirForLib = (dir != null) ? dir : sources.getFirst().getParent();
        Path libDir = findNearestLibDir(baseDirForLib);
        List<Node> libAst = new ArrayList<>();
        if (libDir != null) {
            List<Path> neededLibFiles = resolveNeededLibFiles(libDir, projectImports);
            for (Path libSrc : neededLibFiles) {
                String code = Files.readString(libSrc, StandardCharsets.UTF_8);
                LexerEngine lex = new LexerEngine(code, libSrc.toString());
                if (!lex.getErrors().isEmpty()) return 1;
                ParserContext ctx0 = new ParserContext(lex.getAllTokens(), libSrc.toString());
                libAst.addAll(new ParserEngine(ctx0).parse());
            }
        }

        // 3. 合并 AST
        List<Node> allAst = new ArrayList<>(libAst.size() + projectAst.size());
        allAst.addAll(libAst);
        allAst.addAll(projectAst);

        // 4. 语义分析
        SemanticAnalyzerRunner.runSemanticAnalysis(allAst, false);

        // 5. AST → IR
        IRProgram program = new IRProgramBuilder().buildProgram(allAst);
        program = reorderForEntry(program);

        print("### IR");
        print(program.toString());

        // 6. IR → VM
        VMProgramBuilder builder = new VMProgramBuilder();
        List<InstructionGenerator<? extends IRInstruction>> gens = InstructionGeneratorProvider.defaultGenerators();

        for (IRFunction fn : program.functions()) {
            Map<IRVirtualRegister, Integer> slotMap = new RegisterAllocator().allocate(fn);
            new VMCodeGenerator(slotMap, builder, gens).generate(fn);
        }
        List<String> vmCode = builder.build();

        print("### VM code");
        if (SnowConfig.isDebug()) {
            for (int i = 0; i < vmCode.size(); i++) {
                String[] parts = vmCode.get(i).split(" ");
                String name = OpHelper.opcodeName(parts[0]);
                parts = Arrays.copyOfRange(parts, 1, parts.length);
                print("%04d: %-10s %s%n", i, name, String.join(" ", parts));
            }
        }

        Path outFile = deriveOutputPath(sources, outputName, dir);
        // 确保输出目录存在
        Path parentDir = outFile.getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }
        Files.write(outFile, vmCode, StandardCharsets.UTF_8);
        print("Written to " + outFile.toAbsolutePath());

        if (runAfterCompile) {
            print("\nLaunching VM");
            VMLauncher.main(new String[]{outFile.toString()});
            print("\nVM exited");
        }
        return 0;
    }
}