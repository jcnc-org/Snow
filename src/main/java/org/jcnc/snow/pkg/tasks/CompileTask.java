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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.jcnc.snow.common.SnowConfig.print;

/**
 * {@code CompileTask} 是 CLI 编译任务的实现类，
 * 负责将 Snow 源码文件（.snow）编译为 VM 字节码文件（.water）。
 *
 * <p>功能包括：</p>
 * <ul>
 *   <li>解析命令行参数，收集源码文件</li>
 *   <li>执行词法、语法、语义分析</li>
 *   <li>生成 IR 并进行入口重排与剪枝</li>
 *   <li>转换为 VM code 并输出 .water 文件</li>
 *   <li>可选立即运行生成的程序</li>
 * </ul>
 */
public record CompileTask(Project project, String[] args) implements Task {

    /**
     * 使用默认空参数数组的构造方法。
     *
     * @param project 当前项目
     */
    public CompileTask(Project project) {
        this(project, new String[0]);
    }

    /**
     * 根据参数推断输出文件路径。
     *
     * <p>优先级：</p>
     * <ol>
     *   <li>用户指定的输出名</li>
     *   <li>指定的目录名</li>
     *   <li>单文件场景：源文件名去掉扩展名</li>
     *   <li>默认 "program"</li>
     * </ol>
     *
     * @param sources 源文件列表
     * @param outName 输出文件名（可选）
     * @param dir     源码目录（可选）
     * @return 输出文件路径
     */
    private static Path deriveOutputPath(List<Path> sources, String outName, Path dir) {
        String base;
        if (outName != null) {
            base = outName;
        } else if (dir != null) {
            base = dir.getFileName().toString();
        } else if (sources.size() == 1) {
            base = sources.getFirst().getFileName().toString().replaceFirst("\\.snow$", "");
        } else {
            base = "program";
        }
        return Path.of(base + ".water");
    }

    /**
     * 调整入口函数位置，将 {@code main} 或 {@code *.main} 移至首位。
     *
     * @param in 原始 IRProgram
     * @return 调整后的 IRProgram
     */
    private static IRProgram reorderForEntry(IRProgram in) {
        List<IRFunction> ordered = new ArrayList<>(in.functions());
        for (int i = 0; i < ordered.size(); i++) {
            String fn = ordered.get(i).name();
            if ("main".equals(fn) || fn.endsWith(".main")) {
                Collections.swap(ordered, 0, i);
                break;
            }
        }
        IRProgram out = new IRProgram();
        ordered.forEach(out::add);
        return out;
    }

    /**
     * 调用图可达性分析，移除未使用函数。
     *
     * @param in 原始 IRProgram
     * @return 剪枝后的 IRProgram
     */
    private static IRProgram pruneUnreachable(IRProgram in) {
        if (in.functions().isEmpty()) return in;

        Map<String, IRFunction> byName = new LinkedHashMap<>();
        for (IRFunction f : in.functions()) byName.put(f.name(), f);

        // 找入口函数
        String entry = null;
        for (String n : byName.keySet()) {
            if ("main".equals(n) || n.endsWith(".main")) {
                entry = n;
                break;
            }
        }
        if (entry == null) entry = byName.keySet().iterator().next();

        Set<String> visited = new LinkedHashSet<>();
        Deque<String> work = new ArrayDeque<>();
        visited.add(entry);
        work.add(entry);

        while (!work.isEmpty()) {
            String cur = work.removeFirst();
            IRFunction fn = byName.get(cur);
            if (fn == null) continue;

            for (IRInstruction inst : fn.body()) {
                if (inst instanceof org.jcnc.snow.compiler.ir.instruction.CallInstruction call) {
                    String callee = call.getFunctionName();
                    if (byName.containsKey(callee)) {
                        if (visited.add(callee)) work.addLast(callee);
                        continue;
                    }
                    String suffix = "." + callee;
                    for (String cand : byName.keySet()) {
                        if (cand.endsWith(suffix)) {
                            if (visited.add(cand)) work.addLast(cand);
                        }
                    }
                }
            }
        }

        IRProgram out = new IRProgram();
        for (IRFunction f : in.functions()) {
            if (visited.contains(f.name())) out.add(f);
        }
        return out;
    }

    /**
     * 收集保留的模块名（根据函数所属模块）。
     *
     * @param program 剪枝后的 IRProgram
     * @return 模块名集合
     */
    private static Set<String> collectKeptModules(IRProgram program) {
        Set<String> modules = new LinkedHashSet<>();
        for (IRFunction f : program.functions()) {
            String name = f.name();
            int dot = name.indexOf('.');
            if (dot > 0) {
                modules.add(name.substring(0, dot));
            } else if ("main".equals(name)) {
                modules.add("main");
            }
        }
        return modules;
    }

    /**
     * 从路径中提取 "DemoXX" 形式的子路径，若无则返回文件名。
     *
     * @param src 源文件路径
     * @return 子路径或文件名
     */
    private static String fromDemoXX(Path src) {
        for (int i = 0; i < src.getNameCount(); i++) {
            String part = src.getName(i).toString();
            if (part.matches("Demo\\d+")) {
                return src.subpath(i, src.getNameCount()).toString();
            }
        }
        return src.getFileName().toString();
    }

    /**
     * 执行任务入口，调用 {@link #execute(String[])}。
     *
     * @throws Exception 执行过程中出错
     */
    @Override
    public void run() throws Exception {
        execute(this.args);
    }

    /**
     * 编译并生成 .water 文件，可选立即运行 VM。
     *
     * @param args 命令行参数
     * @return 0 成功，非 0 表示失败
     * @throws Exception 编译或 I/O 过程中出错
     */
    public int execute(String[] args) throws Exception {
        boolean runAfterCompile = false;
        String outputName = null;
        Path dir = null;
        List<Path> sources = new ArrayList<>();

        // 解析参数
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "run" -> runAfterCompile = true;
                case "--debug" -> SnowConfig.MODE = Mode.DEBUG;
                case "-o" -> {
                    if (i + 1 < args.length) outputName = args[++i];
                    else {
                        System.err.println("Missing argument for -o");
                        new CompileCommand().printUsage();
                        return 1;
                    }
                }
                case "-d" -> {
                    if (i + 1 < args.length) dir = Path.of(args[++i]);
                    else {
                        System.err.println("Missing argument for -d");
                        new CompileCommand().printUsage();
                        return 1;
                    }
                }
                default -> {
                    if (args[i].endsWith(".snow")) {
                        sources.add(Path.of(args[i]));
                    } else {
                        System.err.println("Unknown option or file: " + args[i]);
                        new CompileCommand().printUsage();
                        return 1;
                    }
                }
            }
        }

        // 收集源码文件
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

        if (sources.isEmpty()) {
            System.err.println("No .snow source files found.");
            return 1;
        }
        if (sources.size() > 1 && outputName == null && dir == null) {
            System.err.println("Please specify output name using -o <name>");
            return 1;
        }

        // 词法 / 语法分析
        List<Node> allAst = new ArrayList<>();
        Map<Path, String> codeByPath = new LinkedHashMap<>();
        for (Path src : sources) {
            String code = Files.readString(src, StandardCharsets.UTF_8);
            codeByPath.put(src, code);
            LexerEngine lex = new LexerEngine(code, src.toString());
            if (!lex.getErrors().isEmpty()) return 1;
            ParserContext ctx = new ParserContext(lex.getAllTokens(), src.toString());
            allAst.addAll(new ParserEngine(ctx).parse());
        }

        // 语义分析
        SemanticAnalyzerRunner.runSemanticAnalysis(allAst, false);

        // AST -> IR，入口重排 + 剪枝
        IRProgram program = new IRProgramBuilder().buildProgram(allAst);
        program = reorderForEntry(program);
        program = pruneUnreachable(program);

        // 打印保留的源码
        print("### Snow 源代码");
        Set<String> keptModules = collectKeptModules(program);
        for (Path src : sources) {
            String stem = src.getFileName().toString().replaceFirst("\\.snow$", "");
            if (keptModules.contains(stem)) {
                print("#### " + fromDemoXX(src));
                print(codeByPath.get(src));
            }
        }

        // IR 输出
        print("### IR");
        print(program.toString());

        // IR -> VM code
        VMProgramBuilder builder = new VMProgramBuilder();
        List<InstructionGenerator<? extends IRInstruction>> gens =
                InstructionGeneratorProvider.defaultGenerators();
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

        // 写出 .water 文件
        Path outFile = deriveOutputPath(sources, outputName, dir);
        Files.write(outFile, vmCode, StandardCharsets.UTF_8);
        print("Written to " + outFile.toAbsolutePath());

        // 可选运行
        if (runAfterCompile) {
            print("\n=== Launching VM ===");
            VMLauncher.main(new String[]{outFile.toString()});
            print("\n=== VM exited ===");
        }

        return 0;
    }
}
