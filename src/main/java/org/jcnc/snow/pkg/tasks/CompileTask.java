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
 * CompileTask: 将 .snow 源文件编译为 VM 字节码文件 (.water) 的 CLI 任务实现。
 * <p>
 * 用法示例：
 * <pre>
 *   snow compile [run] [-o &lt;name&gt;] [-d &lt;srcDir&gt;] [file1.snow file2.snow ...]
 * </pre>
 * 支持编译完成后立即运行生成的 VM。
 */
public record CompileTask(Project project, String[] args) implements Task {

    /* ------------------------------------------------------------------ */
    /* 1. 构造方法和成员变量                                            */
    /* ------------------------------------------------------------------ */

    /**
     * 使用默认空参数数组的构造方法。
     *
     * @param project 当前项目对象
     */
    public CompileTask(Project project) {
        this(project, new String[0]);
    }

    /* ------------------------------------------------------------------ */
    /* 2. 工具方法                                                       */
    /* ------------------------------------------------------------------ */

    /**
     * 推断输出 .water 文件的路径。
     * <p>
     * 规则：
     * <ul>
     *   <li>若指定了 outName，则以 outName 为基名</li>
     *   <li>否则若指定了目录，则以目录名为基名</li>
     *   <li>否则若只有一个源文件，则以该文件名（去 .snow 后缀）为基名</li>
     *   <li>否则默认为 "program"</li>
     * </ul>
     *
     * @param sources 源文件列表
     * @param outName 用户指定的输出文件基名
     * @param dir     用户指定的源目录
     * @return 推断得到的 .water 文件路径
     */
    private static Path deriveOutputPath(List<Path> sources,
                                         String outName,
                                         Path dir) {
        String base;
        if (outName != null) {
            base = outName;
        } else if (dir != null) {
            base = dir.getFileName().toString();
        } else if (sources.size() == 1) {
            base = sources.getFirst()
                    .getFileName()
                    .toString()
                    .replaceFirst("\\.snow$", "");
        } else {
            base = "program";
        }
        return Path.of(base + ".water");
    }

    /**
     * 将 main 或 *.main 函数调整到列表首位，确保程序入口的 PC = 0。
     *
     * @param in 原始 IRProgram
     * @return 调整入口顺序后的 IRProgram
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

    /* ------------------------------------------------------------------ */
    /* 3. 任务执行入口                                                   */
    /* ------------------------------------------------------------------ */

    /**
     * 执行任务入口，调用 execute 并处理异常。
     *
     * @throws Exception 若执行过程出现异常
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

        /* ---------------- 3.1 解析命令行参数 ---------------- */
        boolean runAfterCompile = false;
        String outputName = null;
        Path dir = null;
        List<Path> sources = new ArrayList<>();

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

        /* ---------------- 3.2 递归收集目录中的 .snow 文件 ---------------- */
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

        /* ---------------- 4. 词法和语法分析 ---------------- */
        List<Node> allAst = new ArrayList<>();

        print("## 编译器输出");
        print("### Snow 源代码");

        for (Path src : sources) {
            String code = Files.readString(src, StandardCharsets.UTF_8);
            print("#### " + src.getFileName());
            print(code);

            LexerEngine lex = new LexerEngine(code, src.toString());
            if (!lex.getErrors().isEmpty()) return 1;

            ParserContext ctx = new ParserContext(lex.getAllTokens(), src.toString());
            allAst.addAll(new ParserEngine(ctx).parse());
        }

        /* ---------------- 5. 语义分析 ---------------- */
        SemanticAnalyzerRunner.runSemanticAnalysis(allAst, false);

        /* ---------------- 6. AST 转 IR，并调整入口 ---------------- */
        IRProgram program = new IRProgramBuilder().buildProgram(allAst);
        program = reorderForEntry(program);  // 确保 main 在首位

//        print("### AST");
//        if (SnowConfig.isDebug()) ASTPrinter.printJson(allAst);

        print("### IR");
        print(program.toString());

        /* ---------------- 7. IR 转 VM 指令 ---------------- */
        VMProgramBuilder builder = new VMProgramBuilder();
        List<InstructionGenerator<? extends IRInstruction>> gens =
                InstructionGeneratorProvider.defaultGenerators();

        for (IRFunction fn : program.functions()) {
            Map<IRVirtualRegister, Integer> slotMap =
                    new RegisterAllocator().allocate(fn);
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

        /* ---------------- 8. 写出 .water 文件 ---------------- */
        Path outFile = deriveOutputPath(sources, outputName, dir);
        Files.write(outFile, vmCode, StandardCharsets.UTF_8);
        print("Written to " + outFile.toAbsolutePath());

        /* ---------------- 9. 可选运行 VM ---------------- */
        if (runAfterCompile) {
            print("\n=== Launching VM ===");
            VMLauncher.main(new String[]{outFile.toString()});
            print("\n=== VM exited ===");
        }

        return 0;
    }
}
