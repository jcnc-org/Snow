package org.jcnc.snow.compiler.lexer.core;

import org.jcnc.snow.compiler.lexer.base.TokenScanner;
import org.jcnc.snow.compiler.lexer.scanners.*;
import org.jcnc.snow.compiler.lexer.token.Token;

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
     * 扫描生成的 Token 序列（包含文件结束符 EOF）
     */
    private final List<Token> tokens = new ArrayList<>();

    /**
     * 词法上下文，提供字符流读取与位置信息
     */
    private final LexerContext context;

    /**
     * Token 扫描器集合，按优先级顺序组织，用于识别不同类别的 Token
     */
    private final List<TokenScanner> scanners;

    /**
     * 构造词法分析器（假定输入源自标准输入，文件名默认为 <stdin>）
     *
     * @param source 源代码文本
     */
    public LexerEngine(String source) {
        this(source, "<stdin>");
    }

    /**
     * 构造词法分析器，并指定源文件名（用于诊断信息）。
     * 构造时立即进行全量扫描。
     *
     * @param source     源代码文本
     * @param sourceName 文件名或来源描述（如"main.snow"）
     */
    public LexerEngine(String source, String sourceName) {
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
            // 输出：文件名:行:列: 错误信息，简洁明了
            System.err.printf(
                    "%s:%d:%d: %s%n",
                    sourceName,
                    le.getLine(),      // 获取出错行号
                    le.getColumn(),    // 获取出错列号
                    le.getMessage()    // 错误描述
            );
            System.exit(65); // 65 = EX_DATAERR，标准数据错误退出码
        }
    }

    /**
     * 主扫描循环，将源代码转为 Token 序列
     * 依次尝试每个扫描器，直到找到可处理当前字符的扫描器为止
     * 扫描到结尾后补充 EOF Token
     */
    private void scanAllTokens() {
        while (!context.isAtEnd()) {
            char currentChar = context.peek();
            // 依次查找能处理当前字符的扫描器
            for (TokenScanner scanner : scanners) {
                if (scanner.canHandle(currentChar, context)) {
                    scanner.handle(context, tokens);
                    break; // 已处理，跳到下一个字符
                }
            }
        }
        // 末尾补一个 EOF 标记
        tokens.add(Token.eof(context.getLine()));
    }

    /**
     * 获取全部 Token（包含 EOF），返回只读列表
     *
     * @return 词法分析结果 Token 列表
     */
    public List<Token> getAllTokens() {
        return List.copyOf(tokens);
    }
}
