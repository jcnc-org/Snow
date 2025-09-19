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
 * CompileTask 负责将 .snow 源文件编译为 VM 字节码（.water 文件），是命令行编译任务的具体实现。
 * <p>
 * 主要功能：
 * <ul>
 *     <li>递归收集目录下所有 .snow 文件</li>
 *     <li>支持命令行参数自定义输出文件名、目录、是否自动运行 VM</li>
 *     <li>输出编译各阶段的关键信息（源码、IR、VM code）</li>
 *     <li>可选自动运行生成的字节码</li>
 * </ul>
 * <b>用法示例：</b>
 * <pre>
 *   snow compile [run] [-o &lt;name&gt;] [-d &lt;srcDir&gt;] [file1.snow file2.snow ...]
 * </pre>
 */
public record CompileTask(Project project, String[] args) implements Task {

    /**
     * 构造方法，使用空参数数组。
     *
     * @param project 当前项目对象
     */
    public CompileTask(Project project) {
        this(project, new String[0]);
    }

    /**
     * 推断输出 .water 文件的路径。
     * <ul>
     *   <li>如指定 outName，则用该值作为基名</li>
     *   <li>否则如指定目录，则用目录名作为基名</li>
     *   <li>否则如仅有一个源文件，则用文件名（去掉 .snow）</li>
     *   <li>否则用默认 "program"</li>
     * </ul>
     *
     * @param sources 源文件列表
     * @param outName 用户指定的输出文件名（可为 null）
     * @param dir     用户指定的目录（可为 null）
     * @return 推断后的 .water 文件路径
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
     * 将 main 或 *.main 函数调整到列表首位，确保入口 PC=0。
     *
     * @param in 原始 IRProgram
     * @return 入口在首位的 IRProgram
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
     * 执行任务主入口，捕获异常向上抛出。
     *
     * @throws Exception 执行出错
     */
    @Override
    public void run() throws Exception {
        execute(this.args);
    }

    /**
     * 编译 .snow 文件为 .water 字节码文件，可选自动运行。
     * 支持参数：
     * <ul>
     *     <li>run：编译后自动运行</li>
     *     <li>-o name：指定输出文件名</li>
     *     <li>-d srcDir：指定目录递归收集</li>
     *     <li>支持多个源文件</li>
     * </ul>
     * 编译流程：
     * <ol>
     *     <li>解析参数，收集源文件</li>
     *     <li>词法/语法分析，输出 AST</li>
     *     <li>语义分析</li>
     *     <li>AST 转 IR，入口排序</li>
     *     <li>IR 转 VM 指令</li>
     *     <li>写出 .water 文件</li>
     *     <li>（可选）自动运行</li>
     * </ol>
     *
     * @param args 命令行参数
     * @return 0 成功，非0失败
     * @throws Exception 任何编译或IO异常
     */
    public int execute(String[] args) throws Exception {

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

        SemanticAnalyzerRunner.runSemanticAnalysis(allAst, false);

        IRProgram program = new IRProgramBuilder().buildProgram(allAst);
        program = reorderForEntry(program);

        print("### IR");
        print(program.toString());

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

        Path outFile = deriveOutputPath(sources, outputName, dir);
        Files.write(outFile, vmCode, StandardCharsets.UTF_8);
        print("Written to " + outFile.toAbsolutePath());

        if (runAfterCompile) {
            print("\n=== Launching VM ===");
            VMLauncher.main(new String[]{outFile.toString()});
            print("\n=== VM exited ===");
        }

        return 0;
    }
}
