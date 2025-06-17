package org.jcnc.snow.compiler.cli;

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
import org.jcnc.snow.vm.engine.VMMode;
import org.jcnc.snow.vm.engine.VirtualMachineEngine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * SnowCompiler CLI —— 多文件 / 单文件 / 目录 模式。
 */
public class SnowCompiler {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("""
                    Usage:
                      snow  <file1.snow> [file2.snow …]
                      snow -d <srcDir>     (compile all *.snow recursively)
                    """);
            return;
        }

        /* ---------- 1. 收集所有待编译源码 ---------- */
        List<Path> sources = collectSources(args);
        if (sources.isEmpty()) {
            System.err.println("No .snow source files found.");
            return;
        }

        /* ---------- 2. 逐个词法+语法分析，合并 AST ---------- */
        List<Node> allAst = new ArrayList<>();
        for (Path p : sources) {
            if (!Files.exists(p)) {
                System.err.println("File not found: " + p);
                return;
            }
            String code = Files.readString(p, StandardCharsets.UTF_8);

            // 保持原有“## 源代码”打印，但标注文件名，兼容旧脚本
            System.out.println("## 源代码 (" + p.getFileName() + ")");
            System.out.println(code);

            LexerEngine lexer = new LexerEngine(code, p.toString());
            ParserContext ctx = new ParserContext(lexer.getAllTokens(), p.toString());
            allAst.addAll(new ParserEngine(ctx).parse());
        }

        /* ---------- 3. 语义分析 ---------- */
        SemanticAnalyzerRunner.runSemanticAnalysis(allAst, false);

        /* ---------- 4. AST → IR ---------- */
        IRProgram program = new IRProgramBuilder().buildProgram(allAst);
        program = reorderForEntry(program);    // 保证入口 main 在首位

        System.out.println("## 编译器输出");
        System.out.println("### AST");
        ASTPrinter.printJson(allAst);
        System.out.println("### IR");
        System.out.println(program);

        /* ---------- 5. IR → VM 指令 ---------- */
        VMProgramBuilder builder = new VMProgramBuilder();
        List<InstructionGenerator<? extends IRInstruction>> generators = InstructionGeneratorProvider.defaultGenerators();

        for (IRFunction fn : program.functions()) {
            Map<IRVirtualRegister, Integer> slotMap =
                    new RegisterAllocator().allocate(fn);
            new VMCodeGenerator(slotMap, builder, generators).generate(fn);
        }
        List<String> finalCode = builder.build();

        System.out.println("### VM code");
        finalCode.forEach(System.out::println);

        /* ---------- 6. 运行虚拟机 ---------- */
        VirtualMachineEngine vm = new VirtualMachineEngine(VMMode.RUN);
        vm.execute(finalCode);
        vm.printLocalVariables();
    }


    /**
     * 根据参数收集待编译文件：
     * - snow file1 file2 …      ← 多文件 / 单文件
     * - snow -d srcDir          ← 目录递归所有 *.snow
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
     * 把 main 函数放到 Program.functions()[0]，保证 PC=0 即入口；
     * 如果用户未写 main，则保持原顺序（语义分析会报错）。
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
}
