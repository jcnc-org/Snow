package org.jcnc.snow.compiler.lexer.core;

import org.jcnc.snow.compiler.lexer.base.TokenScanner;
import org.jcnc.snow.compiler.lexer.scanners.*;
import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.lexer.utils.TokenPrinter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Snow 语言词法分析器核心实现。
 * <p>采用“<b>先扫描 → 后批量校验 → 统一报告</b>”策略:
 * <ol>
 *   <li>{@link #scanAllTokens()}— 用扫描器链把字符流拆成 {@link Token}</li>
 *   <li>{@link #validateTokens()}— 基于 token 序列做轻量上下文校验</li>
 *   <li>{@link #report(List)}— 一次性输出所有词法错误</li>
 * </ol></p>
 */
public class LexerEngine {

    private final List<Token> tokens = new ArrayList<>();   // 扫描结果
    private final List<LexicalError> errors = new ArrayList<>();
    private final String absPath;                           // 绝对路径
    private final LexerContext context;                      // 字符流
    private final List<TokenScanner> scanners;               // 扫描器链

    /**
     * 创建并立即执行扫描-校验-报告流程。
     *
     * @param source     源代码文本
     * @param sourceName 文件名（诊断用）
     */
    public LexerEngine(String source, String sourceName) {
        this.absPath = new File(sourceName).getAbsolutePath();
        this.context = new LexerContext(source);
        this.scanners = List.of(
                new WhitespaceTokenScanner(),
                new NewlineTokenScanner(),
                new CommentTokenScanner(),
                new NumberTokenScanner(),
                new IdentifierTokenScanner(),
                new StringTokenScanner(),
                new OperatorTokenScanner(),
                new SymbolTokenScanner(),
                new UnknownTokenScanner()
        );

        /* 1. 扫描 */
        scanAllTokens();
        /* 2. 后置整体校验 */
        validateTokens();
        /* 3. 打印 token */
        TokenPrinter.print(tokens);
        /* 4. 统一报告错误 */
        report(errors);
    }

    public static void report(List<LexicalError> errors) {
        if (errors == null || errors.isEmpty()) {
            System.out.println("\n## 词法分析通过，没有发现错误\n");
            return;
        }
        System.err.println("\n词法分析发现 " + errors.size() + " 个错误: ");
        errors.forEach(e -> System.err.println("\t" + e));
    }

    public List<Token> getAllTokens() {
        return List.copyOf(tokens);
    }

    public List<LexicalError> getErrors() {
        return List.copyOf(errors);
    }

    /**
     * 逐字符扫描: 依次尝试各扫描器；扫描器抛出的
     * {@link LexicalException} 被捕获并转为 {@link LexicalError}。
     */
    private void scanAllTokens() {
        while (!context.isAtEnd()) {
            char ch = context.peek();
            boolean handled = false;

            for (TokenScanner s : scanners) {
                if (!s.canHandle(ch, context)) continue;

                try {
                    s.handle(context, tokens);
                } catch (LexicalException le) {
                    errors.add(new LexicalError(
                            absPath, le.getLine(), le.getColumn(), le.getReason()
                    ));
                    skipInvalidLexeme();
                }
                handled = true;
                break;
            }

            if (!handled) context.advance();     // 理论不会走到，保险
        }
        tokens.add(Token.eof(context.getLine()));
    }

    /**
     * 跳过当前位置起连续的“标识符 / 数字 / 下划线 / 点”字符。
     * <p>这样可以把诸如 {@code 1abc} 的残余 {@code abc}、{@code _}、
     * {@code .999} 等一次性忽略，避免后续被误识别为新的 token。</p>
     */
    private void skipInvalidLexeme() {
        while (!context.isAtEnd()) {
            char c = context.peek();
            if (Character.isWhitespace(c)) break;           // 空白 / 换行
            if (!Character.isLetterOrDigit(c)
                    && c != '_' && c != '.') break;         // 符号分隔
            context.advance();                              // 否则继续吞掉
        }
    }

    /**
     * 目前包含2条规则: <br>
     * 1. Declare-Ident declare 后必须紧跟合法标识符，并且只能一个<br>
     * 2. Double-Ident declare 后若出现第二个 IDENTIFIER 视为多余<br>
     * <p>发现问题仅写入 {@link #errors}，不抛异常。</p>
     */
    private void validateTokens() {
        for (int i = 0; i < tokens.size(); i++) {
            Token tok = tokens.get(i);

            /* ---------- declare 规则 ---------- */
            if (tok.getType() == TokenType.KEYWORD
                    && "declare".equalsIgnoreCase(tok.getLexeme())) {

                // 第一个非 NEWLINE token
                Token id1 = findNextNonNewline(i);
                if (id1 == null || id1.getType() != TokenType.IDENTIFIER) {
                    errors.add(err(
                            (id1 == null ? tok : id1),
                            "declare 后必须跟合法标识符 (以字母或 '_' 开头)"
                    ));
                    continue; // 若首标识符就错，后续检查可略
                }

                // 检查是否有第二个 IDENTIFIER
                Token id2 = findNextNonNewline(tokens.indexOf(id1));
                if (id2 != null && id2.getType() == TokenType.IDENTIFIER) {
                    errors.add(err(id2, "declare 声明中出现多余的标识符"));
                }
            }
        }
    }

    /**
     * index 右侧最近非 NEWLINE token；无则 null
     */
    private Token findNextNonNewline(int index) {
        for (int j = index + 1; j < tokens.size(); j++) {
            Token t = tokens.get(j);
            if (t.getType() != TokenType.NEWLINE) return t;
        }
        return null;
    }

    /**
     * 构造统一的 LexicalError
     */
    private LexicalError err(Token t, String msg) {
        return new LexicalError(absPath, t.getLine(), t.getCol(), msg);
    }
}
