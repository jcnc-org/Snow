package org.jcnc.snow.pkg.tasks;

import org.jcnc.snow.cli.commands.CompileCommand;
import org.jcnc.snow.compiler.backend.util.OpHelper;
import org.jcnc.snow.compiler.backend.alloc.RegisterAllocator;
import org.jcnc.snow.compiler.backend.builder.VMCodeGenerator;
import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.backend.generator.InstructionGeneratorProvider;
import org.jcnc.snow.compiler.ir.builder.IRProgramBuilder;
import org.jcnc.snow.compiler.ir.core.IRFunction;
import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.core.IRProgram;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.lexer.core.LexerEngine;
import org.jcnc.snow.compiler.parser.ast.base.Node;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.core.ParserEngine;
import org.jcnc.snow.compiler.parser.function.ASTPrinter;
import org.jcnc.snow.compiler.semantic.core.SemanticAnalyzerRunner;
import org.jcnc.snow.pkg.model.Project;
import org.jcnc.snow.vm.VMLauncher;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * CLI 任务：编译 .snow 源文件为 VM 字节码（.water 文件）。
 * <p>
 * 支持单文件、多文件和目录递归编译，并可在编译后立即运行虚拟机。<br>
 * 命令行参数支持 run、-o、-d 及直接指定源文件。
 * </p>
 *
 * <pre>
 * 用法示例：
 * $ snow compile [run] [-o &lt;name&gt;] [-d &lt;srcDir&gt;] [file1.snow file2.snow ...]
 * </pre>
 */
public final class CompileTask implements Task {
    /** 项目信息 */
    private final Project project;
    /** 原始命令行参数 */
    private final String[] args;

    /**
     * 创建一个编译任务。
     *
     * @param project  项目信息对象
     * @param args     命令行参数数组
     */
    public CompileTask(Project project, String[] args) {
        this.project = project;
        this.args = args;
    }

    /**
     * 创建一个不带参数的编译任务。
     *
     * @param project 项目信息对象
     */
    public CompileTask(Project project) {
        this(project, new String[0]);
    }

    /**
     * 执行编译任务。该方法会解析参数并调用 {@link #execute(String[])} 进行实际编译流程。
     *
     * @throws Exception 执行过程中出现任意异常时抛出
     */
    @Override
    public void run() throws Exception {
        execute(this.args);
    }

    /**
     * 编译 .snow 源文件为 VM 字节码，并可选择立即运行。
     * <ul>
     *   <li>支持参数 run（编译后运行）、-o（输出文件名）、-d（递归目录）、直接指定多个源文件。</li>
     *   <li>输出源代码、AST、IR、最终 VM code，并写出 .water 文件。</li>
     * </ul>
     *
     * @param args 命令行参数数组
     * @return 0 表示成功，非 0 表示失败
     * @throws Exception 编译或写入过程中出现异常时抛出
     */
    public int execute(String[] args) throws Exception {
        // ---------------- 解析命令行参数 ----------------
        boolean runAfterCompile = false;
        String outputName = null;
        Path dir = null;
        List<Path> sources = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "run" -> runAfterCompile = true;
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
                    if (arg.endsWith(".snow")) {
                        sources.add(Path.of(arg));
                    } else {
                        System.err.println("Unknown option or file: " + arg);
                        new CompileCommand().printUsage();
                        return 1;
                    }
                }
            }
        }

        // --------- 如果指定了目录则递归收集所有 *.snow ---------
        if (dir != null) {
            if (!Files.isDirectory(dir)) {
                System.err.println("Not a directory: " + dir);
                return 1;
            }
            try (var stream = Files.walk(dir)) {
                stream.filter(p -> p.toString().endsWith(".snow"))
                        .sorted()                       // 确保稳定顺序
                        .forEach(sources::add);
            }
        }

        if (sources.isEmpty()) {
            System.err.println("No .snow source files found.");
            return 1;
        }

        // 多文件但未指定 -o 且非目录编译 —— 提示必须指定输出名
        if (sources.size() > 1 && outputName == null && dir == null) {
            System.err.println("Please specify output name using -o <name>");
            return 1;
        }

        // ---------------- 1. 词法/语法分析，并打印源代码 ----------------
        List<Node> allAst = new ArrayList<>();

        System.out.println("## 编译器输出");
        System.out.println("### Snow 源代码");

        for (Path p : sources) {
            if (!Files.exists(p)) {
                System.err.println("File not found: " + p);
                return 1;
            }

            String code = Files.readString(p, StandardCharsets.UTF_8);

            // 打印源码
            System.out.println("#### " + p.getFileName());
            System.out.println(code);

            // 词法、语法分析
            LexerEngine lexer = new LexerEngine(code, p.toString());
            ParserContext ctx = new ParserContext(lexer.getAllTokens(), p.toString());
            allAst.addAll(new ParserEngine(ctx).parse());
        }

        // ---------------- 2. 语义分析 ----------------
        SemanticAnalyzerRunner.runSemanticAnalysis(allAst, false);

        // ---------------- 3. AST → IR，并将 main 函数移动至首位 ----------------
        IRProgram program = new IRProgramBuilder().buildProgram(allAst);
        program = reorderForEntry(program);

        // 打印 AST 和 IR
        System.out.println("### AST");
        ASTPrinter.printJson(allAst);

        System.out.println("### IR");
        System.out.println(program);

        // ---------------- 4. IR → VM 指令 ----------------
        VMProgramBuilder builder = new VMProgramBuilder();
        List<InstructionGenerator<? extends IRInstruction>> generators =
                InstructionGeneratorProvider.defaultGenerators();

        for (IRFunction fn : program.functions()) {
            Map<IRVirtualRegister, Integer> slotMap =
                    new RegisterAllocator().allocate(fn);
            new VMCodeGenerator(slotMap, builder, generators).generate(fn);
        }
        List<String> finalCode = builder.build();

        System.out.println("### VM code");
        for (int i = 0; i < finalCode.size(); i++) {
            String[] parts = finalCode.get(i).split(" ");
            String name = OpHelper.opcodeName(parts[0]);
            parts = Arrays.copyOfRange(parts, 1, parts.length);
            System.out.printf("%04d: %-10s %s\n", i, name, String.join(" ", parts));
        }

        // ---------------- 5. 写出 .water 文件 ----------------
        Path outputFile = deriveOutputPath(sources, outputName, dir);
        Files.write(outputFile, finalCode, StandardCharsets.UTF_8);
        System.out.println("Written to " + outputFile.toAbsolutePath());

        // ---------------- 6. 可选：立即运行 VM ----------------
        if (runAfterCompile) {
            System.out.println("\n=== Launching VM ===");
            VMLauncher.main(new String[]{outputFile.toString()});
        }

        return 0;
    }

    /**
     * 推断 .water 输出文件名。
     * <ul>
     *   <li>如果指定 -o，直接使用该名称。</li>
     *   <li>目录编译时，取目录名。</li>
     *   <li>单文件编译时，取文件名去掉 .snow 后缀。</li>
     *   <li>否则默认 "program"。</li>
     * </ul>
     *
     * @param sources   源文件路径列表
     * @param outName   输出文件名（如有指定，否则为 null）
     * @param dir       源码目录（如有指定，否则为 null）
     * @return 推断出的输出文件路径（.water 文件）
     */
    private static Path deriveOutputPath(List<Path> sources, String outName, Path dir) {
        String base;
        if (outName != null) {
            base = outName;
        } else if (dir != null) {
            base = dir.getFileName().toString();
        } else if (sources.size() == 1) {
            base = sources.getFirst().getFileName().toString()
                    .replaceFirst("\\.snow$", "");
        } else {
            base = "program";
        }
        return Path.of(base + ".water");
    }

    /**
     * 将 main 函数调整至函数列表首位，确保程序入口为 PC=0。
     *
     * @param in 原始 IRProgram
     * @return 调整入口后的 IRProgram
     */
    private static IRProgram reorderForEntry(IRProgram in) {
        List<IRFunction> ordered = new ArrayList<>(in.functions());
        for (int i = 0; i < ordered.size(); i++) {
            if ("main".equals(ordered.get(i).name())) {
                Collections.swap(ordered, 0, i);
                break;
            }
        }
        IRProgram out = new IRProgram();
        ordered.forEach(out::add);
        return out;
    }
}
