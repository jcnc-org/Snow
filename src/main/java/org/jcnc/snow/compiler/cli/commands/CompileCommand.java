package org.jcnc.snow.compiler.cli.commands;

import org.jcnc.snow.compiler.backend.alloc.RegisterAllocator;
import org.jcnc.snow.compiler.backend.builder.VMCodeGenerator;
import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.backend.generator.InstructionGeneratorProvider;
import org.jcnc.snow.compiler.cli.CLICommand;
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
 * <p>
 * 编译器命令实现：`snow compile`
 * <br>
 * 将一个或多个 .snow 源文件编译为 VM 字节码文件（.water）。
 * </p>
 * <ul>
 *     <li>支持选项：-o 指定输出基名（不含 .water 后缀）。</li>
 *     <li>支持选项：-d 递归目录编译（输出名自动取目录名）。</li>
 *     <li>支持 run 子命令：编译后立即运行。</li>
 * </ul>
 * <p>
 * 用法：<br>
 * <code>snow compile [run] [-o &lt;name&gt;] [-d &lt;srcDir&gt;] [file1.snow file2.snow …]</code>
 * </p>
 */
public final class CompileCommand implements CLICommand {

    /**
     * 获取命令名。
     *
     * @return "compile"
     */
    @Override
    public String name() {
        return "compile";
    }

    /**
     * 获取命令描述。
     *
     * @return 命令简介
     */
    @Override
    public String description() {
        return "Compile .snow source files into VM byte-code (.water).";
    }

    /**
     * 打印该命令的用法说明。
     */
    @Override
    public void printUsage() {
        System.out.println("Usage:");
        System.out.println("  snow compile [run] [-o <name>] [-d <srcDir>] [file1.snow file2.snow …]");
        System.out.println("Options:");
        System.out.println("  run           compile then run");
        System.out.println("  -o <name>     specify output base name (without .water suffix)");
        System.out.println("  -d <srcDir>   recursively compile all .snow files in directory");
    }

    /**
     * 执行 compile 命令，编译 .snow 源文件。
     *
     * @param args 剩余参数（不含命令名）
     * @return 0 表示成功，1 表示参数错误或编译失败
     * @throws Exception 编译过程中可能抛出的异常
     */
    @Override
    public int execute(String[] args) throws Exception {
        boolean runAfterCompile = false;
        String outputName = null;
        Path dir = null;
        List<Path> sources = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "run":
                    runAfterCompile = true;
                    break;
                case "-o":
                    if (i + 1 < args.length) {
                        outputName = args[++i];
                    } else {
                        System.err.println("Missing argument for -o");
                        printUsage();
                        return 1;
                    }
                    break;
                case "-d":
                    if (i + 1 < args.length) {
                        dir = Path.of(args[++i]);
                    } else {
                        System.err.println("Missing argument for -d");
                        printUsage();
                        return 1;
                    }
                    break;
                default:
                    if (arg.endsWith(".snow")) {
                        sources.add(Path.of(arg));
                    } else {
                        System.err.println("Unknown option or file: " + arg);
                        printUsage();
                        return 1;
                    }
            }
        }

        // 目录编译时收集所有 .snow 文件
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

        // 多文件且未指定输出时错误
        if (sources.size() > 1 && outputName == null && dir == null) {
            System.err.println("Please specify output name using -o <name>");
            return 1;
        }

        // 1. 词法和语法分析
        List<Node> allAst = new ArrayList<>();
        for (Path p : sources) {
            if (!Files.exists(p)) {
                System.err.println("File not found: " + p);
                return 1;
            }
            String code = Files.readString(p, StandardCharsets.UTF_8);
            LexerEngine lexer = new LexerEngine(code, p.toString());
            ParserContext ctx = new ParserContext(lexer.getAllTokens(), p.toString());
            allAst.addAll(new ParserEngine(ctx).parse());
        }

        // 2. 语义分析
        SemanticAnalyzerRunner.runSemanticAnalysis(allAst, false);

        // 3. AST -> IR 并重排序入口
        IRProgram program = new IRProgramBuilder().buildProgram(allAst);
        program = reorderForEntry(program);

        System.out.println("## 编译器输出");
        System.out.println("### AST");
        ASTPrinter.printJson(allAst);
        System.out.println("### IR");
        System.out.println(program);

        // 4. IR -> VM 指令
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

        // 5. 写出 .water 文件
        Path outputFile = deriveOutputPath(sources, outputName, dir);
        Files.write(outputFile, finalCode, StandardCharsets.UTF_8);
        System.out.println("Written to " + outputFile.toAbsolutePath());

        // 6. 可选执行
        if (runAfterCompile) {
            System.out.println("\n=== Launching VM ===");
            VMLauncher.main(new String[]{outputFile.toString()});
        }
        return 0;
    }

    /**
     * 推断输出 .water 文件的路径。
     *
     * @param sources 源文件列表
     * @param outName 用户指定的输出基名
     * @param dir 目录编译时的源目录
     * @return 输出文件的完整路径
     */
    private static Path deriveOutputPath(List<Path> sources, String outName, Path dir) {
        String base;
        if (outName != null) {
            base = outName;
        } else if (dir != null) {
            base = dir.getFileName().toString();
        } else if (sources.size() == 1) {
            base = sources.get(0).getFileName().toString().replaceFirst("\\.snow$", "");
        } else {
            base = "program";
        }
        return Path.of(base + ".water");
    }

    /**
     * 保证 main 函数为程序入口。
     *
     * @param in 输入的 IR 程序
     * @return 重新排序，使 main 函数位于首位的 IR 程序
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
