package org.jcnc.snow.compiler.lexer.core;

import org.jcnc.snow.compiler.lexer.base.TokenScanner;
import org.jcnc.snow.compiler.lexer.scanners.*;
import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.utils.TokenPrinter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code LexerEngine} 是编译器前端的词法分析器核心实现。
 * <p>
 * 负责将源代码字符串按顺序扫描并转换为一系列 {@link Token} 实例，
 * 每个 Token 表示语法上可识别的最小单位（如标识符、关键字、常量、运算符等）。
 * <p>
 * 分析流程通过注册多个 {@link TokenScanner} 扫描器实现类型识别，
 * 并由 {@link LexerContext} 提供字符流与位置信息支持。
 * 支持文件名传递，遇到非法字符时会以“文件名:行:列:错误信息”输出简洁诊断。
 * </p>
 */
public class LexerEngine {
    /**
     * 扫描生成的 Token 序列（包含文件结束符 EOF）。
     * 每个 Token 表示源代码中的一个词法单元。
     */
    private final List<Token> tokens = new ArrayList<>();

    /**
     * 当前源文件的绝对路径，用于错误信息定位。
     */
    private final String absPath;

    /**
     * 词法上下文，负责字符流读取与位置信息维护。
     */
    private final LexerContext context;

    /**
     * Token 扫描器集合，按优先级顺序排列，
     * 用于识别不同类别的 Token（如空白、注释、数字、标识符等）。
     */
    private final List<TokenScanner> scanners;

    /**
     * 词法分析过程中收集到的全部词法错误。
     */
    private final List<LexicalError> errors = new ArrayList<>();

    /**
     * 构造词法分析器（假定输入源自标准输入，文件名默认为 &lt;stdin&gt;）。
     *
     * @param source 源代码文本
     */
    public LexerEngine(String source) {
        this(source, "<stdin>");
    }

    /**
     * 构造词法分析器，并指定源文件名（用于诊断信息）。
     * 构造时立即进行全量扫描，扫描结束后打印所有 Token 并报告词法错误。
     *
     * @param source     源代码文本
     * @param sourceName 文件名或来源描述（如"Main.snow"）
     */
    public LexerEngine(String source, String sourceName) {
        this.absPath = new File(sourceName).getAbsolutePath();
        this.context = new LexerContext(source);
        this.scanners = List.of(
                new WhitespaceTokenScanner(), // 跳过空格、制表符等
                new NewlineTokenScanner(),    // 处理换行符，生成 NEWLINE Token
                new CommentTokenScanner(),    // 处理单行/多行注释
                new NumberTokenScanner(),     // 识别整数与浮点数字面量
                new IdentifierTokenScanner(), // 识别标识符和关键字
                new StringTokenScanner(),     // 处理字符串常量
                new OperatorTokenScanner(),   // 识别运算符
                new SymbolTokenScanner(),     // 识别括号、分号等符号
                new UnknownTokenScanner()     // 捕捉无法识别的字符，最后兜底
        );

        // 主扫描流程，遇到非法字符立即输出错误并终止进程
        try {
            scanAllTokens();
        } catch (LexicalException le) {
            // 输出：绝对路径: 行 x, 列 y: 错误信息
            System.err.printf(
                    "%s: 行 %d, 列 %d: %s%n",
                    absPath,
                    le.getLine(),
                    le.getColumn(),
                    le.getReason()
            );
            System.exit(65); // 65 = EX_DATAERR
        }
        TokenPrinter.print(this.tokens);
        LexerEngine.report(this.getErrors());
    }

    /**
     * 静态报告方法。
     * <p>
     * 打印所有词法分析过程中收集到的错误信息。
     * 如果无错误，输出词法分析通过的提示。
     *
     * @param errors 词法错误列表
     */
    public static void report(List<LexicalError> errors) {
        if (errors != null && !errors.isEmpty()) {
            System.err.println("\n词法分析发现 " + errors.size() + " 个错误：");
            errors.forEach(err -> System.err.println("  " + err));
        } else {
            System.out.println("\n## 词法分析通过，没有发现错误\n");
        }
    }

    /**
     * 主扫描循环，将源代码转为 Token 序列。
     * <p>
     * 依次尝试每个扫描器，直到找到可处理当前字符的扫描器为止。
     * 扫描到结尾后补充 EOF Token。
     * 若遇到词法异常则收集错误并跳过当前字符，避免死循环。
     */
    private void scanAllTokens() {
        while (!context.isAtEnd()) {
            char currentChar = context.peek();
            boolean handled = false;
            for (TokenScanner scanner : scanners) {
                if (scanner.canHandle(currentChar, context)) {
                    try {
                        scanner.handle(context, tokens);
                    } catch (LexicalException le) {
                        // 收集词法错误，不直接退出
                        errors.add(new LexicalError(
                                absPath, le.getLine(), le.getColumn(), le.getReason()
                        ));
                        // 跳过当前字符，防止死循环
                        context.advance();
                    }
                    handled = true;
                    break;
                }
            }
            if (!handled) {
                // 万一没有任何扫描器能处理，跳过一个字符防止死循环
                context.advance();
            }
        }
        tokens.add(Token.eof(context.getLine()));
    }

    /**
     * 获取全部 Token（包含 EOF），返回只读列表。
     *
     * @return 词法分析结果 Token 列表
     */
    public List<Token> getAllTokens() {
        return List.copyOf(tokens);
    }

    /**
     * 返回全部词法错误（返回只读列表）。
     *
     * @return 词法错误列表
     */
    public List<LexicalError> getErrors() {
        return List.copyOf(errors);
    }
}
