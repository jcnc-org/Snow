package org.jcnc.snow.compiler.cli.commands;

import org.jcnc.snow.compiler.cli.CLICommand;
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
import org.jcnc.snow.vm.VMLauncher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * <p>
 * 编译器命令行入口：实现 `snow compile` 命令。
 * 支持编译一个或多个 .snow 源文件为 VM 字节码文件，或编译后直接运行。
 * </p>
 * <ul>
 *   <li>支持编译单个文件、多个文件、或目录递归批量编译。</li>
 *   <li>支持 compile-only 或 compile-then-run 模式。</li>
 *   <li>完成词法、语法、语义分析，IR 构建，寄存器分配，VM 指令生成。</li>
 * </ul>
 * <p>
 * 用法：<br>
 * <code>snow compile foo.snow</code><br>
 * <code>snow compile -d src</code><br>
 * <code>snow compile run foo.snow</code>
 * </p>
 */
public final class CompileCommand implements CLICommand {

    /**
     * 获取命令名称。
     *
     * @return 命令名 "compile"
     */
    @Override
    public String name() { return "compile"; }

    /**
     * 获取命令的简要描述。
     *
     * @return 命令描述文本
     */
    @Override
    public String description() {
        return "Compile .snow source files into VM byte-code.";
    }

    /**
     * 打印用法说明。
     */
    @Override
    public void printUsage() {
        System.out.println("""
                Usage:
                  snow compile <file1.snow> [file2.snow …]   (compile only)
                  snow compile -d <srcDir>                   (recursively compile)
                  snow compile run <file.snow> […]           (compile then run)
                """);
    }

    /**
     * 执行 compile 命令。
     *
     * @param args 命令行参数
     * @return 0 表示成功，非0表示错误
     * @throws Exception 发生任何编译、文件、运行错误
     */
    @Override
    public int execute(String[] args) throws Exception {
        // 0. 检查是否包含 run
        boolean runAfterCompile = false;
        int offset = 0;
        if (args.length > 0 && "run".equals(args[0])) {
            runAfterCompile = true;
            offset = 1;
        }
        if (args.length - offset == 0) {
            printUsage();
            return 1;
        }
        String[] srcArgs = Arrays.copyOfRange(args, offset, args.length);

        List<Path> sources = collectSources(srcArgs);
        if (sources.isEmpty()) {
            System.err.println("No .snow source files found.");
            return 1;
        }

        // 1. 词法+语法分析，合并AST
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

        // 3. AST -> IR
        IRProgram program = new IRProgramBuilder().buildProgram(allAst);
        program = reorderForEntry(program);    // 保证入口 main 在首位

        System.out.println("## 编译器输出");
        System.out.println("### AST");
        ASTPrinter.printJson(allAst);
        System.out.println("### IR");
        System.out.println(program);

        // 4. IR -> VM指令
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

        // 5. 将VM指令写入文件
        Path outputFile = deriveOutputPath(sources);
        Files.write(outputFile, finalCode, StandardCharsets.UTF_8);
        System.out.println("Written to " + outputFile.toAbsolutePath());

        // 6. 若指定run，立即执行
        if (runAfterCompile) {
            System.out.println("\n=== Launching VM ===");
            VMLauncher.main(new String[]{outputFile.toString()});
        }
        return 0;
    }

    /**
     * 收集所有待编译的源文件。
     * <ul>
     *     <li>参数 "-d <srcDir>"：递归收集目录下所有 .snow 文件</li>
     *     <li>否则认为是文件列表参数</li>
     * </ul>
     *
     * @param args 命令行参数
     * @return 源文件路径列表，若未找到返回空列表
     * @throws IOException 文件IO错误
     */
    private static List<Path> collectSources(String[] args) throws IOException {
        if (args.length == 2 && "-d".equals(args[0])) {
            Path dir = Path.of(args[1]);
            if (!Files.isDirectory(dir)) {
                System.err.println("Not a directory: " + dir);
                return List.of();
            }
            try (var stream = Files.walk(dir)) {
                return stream.filter(p -> p.toString().endsWith(".snow"))
                        .sorted()          // 稳定顺序，方便比对输出
                        .toList();
            }
        }
        // 普通文件参数
        return Arrays.stream(args).map(Path::of).toList();
    }

    /**
     * 保证 main 函数排在 Program.functions()[0]，使 PC=0 即为程序入口。
     * <ul>
     *     <li>若存在 main，则与第0个函数互换</li>
     *     <li>若不存在 main，则顺序不变（后续语义分析会报错）</li>
     * </ul>
     *
     * @param in 原始IR程序对象
     * @return 处理后的IR程序对象
     */
    private static IRProgram reorderForEntry(IRProgram in) {
        List<IRFunction> ordered = new ArrayList<>(in.functions());
        int idx = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if ("main".equals(ordered.get(i).name())) {
                idx = i;
                break;
            }
        }
        if (idx > 0) Collections.swap(ordered, 0, idx);

        IRProgram out = new IRProgram();
        ordered.forEach(out::add);
        return out;
    }

    /**
     * 推断输出文件名。
     * <ul>
     *     <li>单文件编译：输出同名 .vm 文件</li>
     *     <li>多文件/目录编译：输出 "program.vm" 到当前目录</li>
     * </ul>
     *
     * @param sources 输入源文件路径列表
     * @return 输出的 VM 文件路径
     */
    private static Path deriveOutputPath(List<Path> sources) {
        if (sources.size() == 1) {
            Path src = sources.getFirst();
            String name = src.getFileName().toString()
                    .replaceFirst("\\.snow$", "");
            return src.resolveSibling(name + ".vm");
        }
        return Path.of("program.vm");
    }
}
