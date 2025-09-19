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
    private static Path deriveOutputPath(List<Path> sources,
                                         String outName,
                                         Path dir) {
        String base;
        if (outName != null) { // 优先使用用户指定的输出名
            base = outName;
        } else if (dir != null) { // 如果指定了目录，则使用目录名作为基名
            base = dir.getFileName().toString();
        } else if (sources.size() == 1) { // 单个源文件时，使用源文件名（去掉扩展名）
            base = sources.getFirst()
                    .getFileName()
                    .toString()
                    .replaceFirst("\\.snow$", "");
        } else { // 默认情况
            base = "program";
        }
        return Path.of(base + ".water"); // 拼接生成 .water 文件路径
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

    /**
     * 执行任务主入口，捕获异常向上抛出
     */
    @Override
    public void run() throws Exception {
        execute(this.args); // 调用 execute 方法
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

        List<Node> allAst = new ArrayList<>(); // 存放解析后的 AST

        print("## 编译器输出");
        print("### Snow 源代码");

        // === 阶段1：词法 + 语法分析 ===
        for (Path src : sources) {
            String code = Files.readString(src, StandardCharsets.UTF_8); // 读取源码
            print("#### " + src.getFileName());
            print(code);

            // 构造词法分析器
            LexerEngine lex = new LexerEngine(code, src.toString());
            if (!lex.getErrors().isEmpty()) return 1; // 有错误则退出

            // 构造语法分析上下文并生成 AST
            ParserContext ctx = new ParserContext(lex.getAllTokens(), src.toString());
            allAst.addAll(new ParserEngine(ctx).parse());
        }

        // === 阶段2：语义分析 ===
        SemanticAnalyzerRunner.runSemanticAnalysis(allAst, false);

        // === 阶段3：AST 转 IR 并调整入口函数位置 ===
        IRProgram program = new IRProgramBuilder().buildProgram(allAst);
        program = reorderForEntry(program);

        print("### IR");
        print(program.toString()); // 输出 IR

        // === 阶段4：IR 转 VM 指令 ===
        VMProgramBuilder builder = new VMProgramBuilder();
        List<InstructionGenerator<? extends IRInstruction>> gens =
                InstructionGeneratorProvider.defaultGenerators();

        for (IRFunction fn : program.functions()) {
            // 分配寄存器槽位
            Map<IRVirtualRegister, Integer> slotMap =
                    new RegisterAllocator().allocate(fn);
            // 生成 VM 代码
            new VMCodeGenerator(slotMap, builder, gens).generate(fn);
        }
        List<String> vmCode = builder.build();

        // 输出 VM 代码
        print("### VM code");
        if (SnowConfig.isDebug()) {
            for (int i = 0; i < vmCode.size(); i++) {
                String[] parts = vmCode.get(i).split(" ");
                String name = OpHelper.opcodeName(parts[0]); // 转换 opcode 为可读名
                parts = Arrays.copyOfRange(parts, 1, parts.length);
                print("%04d: %-10s %s%n", i, name, String.join(" ", parts));
            }
        }

        // === 阶段5：写出 .water 文件 ===
        Path outFile = deriveOutputPath(sources, outputName, dir);
        Files.write(outFile, vmCode, StandardCharsets.UTF_8);
        print("Written to " + outFile.toAbsolutePath());

        // === 阶段6：可选运行 VM ===
        if (runAfterCompile) {
            print("\n=== Launching VM ===");
            VMLauncher.main(new String[]{outFile.toString()});
            print("\n=== VM exited ===");
        }

        return 0;
    }
}
