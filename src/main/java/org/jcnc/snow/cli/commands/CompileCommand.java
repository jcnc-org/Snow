package org.jcnc.snow.cli.commands;

import org.jcnc.snow.compiler.backend.alloc.RegisterAllocator;
import org.jcnc.snow.compiler.backend.builder.VMCodeGenerator;
import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.backend.generator.InstructionGeneratorProvider;
import org.jcnc.snow.cli.CLICommand;
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
import org.jcnc.snow.vm.VMLauncher;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


/**
 * CLI 命令：将 .snow 源文件编译为 VM 字节码（.water 文件）。
 * <p>
 * 支持递归目录、多文件编译，可选编译后立即运行。<br>
 * 命令参数支持 run、-o、-d 等。
 * </p>
 *
 * <pre>
 * 用法示例：
 * $ snow compile [run] [-o &lt;name&gt;] [-d &lt;srcDir&gt;] [file1.snow file2.snow …]
 * </pre>
 */
public final class CompileCommand implements CLICommand {

    /* --------------------------------------------------------------------- */
    /* CLICommand 接口实现                                                   */
    /* --------------------------------------------------------------------- */

    @Override
    public String name() {
        return "compile";
    }

    @Override
    public String description() {
        return "Compile .snow source files into VM byte-code (.water).";
    }

    @Override
    public void printUsage() {
        System.out.println("Usage:");
        System.out.println("  snow compile [run] [-o <name>] [-d <srcDir>] [file1.snow file2.snow …]");
        System.out.println("Options:");
        System.out.println("  run           compile then run");
        System.out.println("  -o <name>     specify output base name (without .water suffix)");
        System.out.println("  -d <srcDir>   recursively compile all .snow files in directory");
    }

    /* --------------------------------------------------------------------- */
    /* 核心：执行 compile 子命令                                              */
    /* --------------------------------------------------------------------- */

    @Override
    public int execute(String[] args) throws Exception {
        /* ---------------- 解析命令行参数 ---------------- */
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
                        printUsage();
                        return 1;
                    }
                }
                case "-d" -> {
                    if (i + 1 < args.length) dir = Path.of(args[++i]);
                    else {
                        System.err.println("Missing argument for -d");
                        printUsage();
                        return 1;
                    }
                }
                default -> {
                    if (arg.endsWith(".snow")) {
                        sources.add(Path.of(arg));
                    } else {
                        System.err.println("Unknown option or file: " + arg);
                        printUsage();
                        return 1;
                    }
                }
            }
        }

        /* --------- 如果指定了目录则递归收集所有 *.snow --------- */
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

        /* 多文件但未指定 -o 且非目录编译 —— 提示必须指定输出名 */
        if (sources.size() > 1 && outputName == null && dir == null) {
            System.err.println("Please specify output name using -o <name>");
            return 1;
        }

        /* ----------------------------------------------------------------- */
        /* 1. 词法 + 语法分析；同时打印源代码                                */
        /* ----------------------------------------------------------------- */
        List<Node> allAst = new ArrayList<>();

        System.out.println("## 编译器输出");
        System.out.println("### Snow 源代码");        // ========== 新增：二级标题 ==========

        for (Path p : sources) {
            if (!Files.exists(p)) {
                System.err.println("File not found: " + p);
                return 1;
            }

            String code = Files.readString(p, StandardCharsets.UTF_8);

            // ------- 打印每个文件的源码 -------
            System.out.println("#### " + p.getFileName());
            System.out.println(code);
            // --------------------------------------------------------

            /* 词法 + 语法 */
            LexerEngine lexer = new LexerEngine(code, p.toString());
            ParserContext ctx = new ParserContext(lexer.getAllTokens(), p.toString());
            allAst.addAll(new ParserEngine(ctx).parse());
        }

        /* ----------------------------------------------------------------- */
        /* 2. 语义分析                                                       */
        /* ----------------------------------------------------------------- */
        SemanticAnalyzerRunner.runSemanticAnalysis(allAst, false);

        /* ----------------------------------------------------------------- */
        /* 3. AST → IR，并把 main 函数调到首位                                */
        /* ----------------------------------------------------------------- */
        IRProgram program = new IRProgramBuilder().buildProgram(allAst);
        program = reorderForEntry(program);

        /* ---------------- 打印 AST / IR ---------------- */
        System.out.println("### AST");
        ASTPrinter.printJson(allAst);

        System.out.println("### IR");
        System.out.println(program);

        /* ----------------------------------------------------------------- */
        /* 4. IR → VM 指令                                                   */
        /* ----------------------------------------------------------------- */
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
        finalCode.forEach(System.out::println);

        /* ----------------------------------------------------------------- */
        /* 5. 写出 .water 文件                                               */
        /* ----------------------------------------------------------------- */
        Path outputFile = deriveOutputPath(sources, outputName, dir);
        Files.write(outputFile, finalCode, StandardCharsets.UTF_8);
        System.out.println("Written to " + outputFile.toAbsolutePath());

        /* ----------------------------------------------------------------- */
        /* 6. 可选：立即运行 VM                                              */
        /* ----------------------------------------------------------------- */
        if (runAfterCompile) {
            System.out.println("\n=== Launching VM ===");
            VMLauncher.main(new String[]{outputFile.toString()});
        }

        return 0;
    }

    /* --------------------------------------------------------------------- */
    /* 辅助方法                                                              */
    /* --------------------------------------------------------------------- */

    /**
     * 根据输入情况推断 .water 输出文件名：
     * <ul>
     *   <li>若指定 -o，则直接使用</li>
     *   <li>目录编译：取目录名</li>
     *   <li>单文件编译：取文件名去掉 .snow</li>
     *   <li>其他情况兜底为 "program"</li>
     * </ul>
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
     * 把 main 函数交换到程序函数列表首位，确保 PC=0 即入口。
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
