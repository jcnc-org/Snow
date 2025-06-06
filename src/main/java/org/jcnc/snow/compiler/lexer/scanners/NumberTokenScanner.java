package org.jcnc.snow.compiler.lexer.scanners;

import org.jcnc.snow.compiler.lexer.core.LexerContext;
import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;

/**
 * 数字扫描器：识别整数、小数以及带有<strong>类型后缀</strong>的数字字面量。
 * <p>
 * 支持的格式示例：
 * <ul>
 *     <li>整数：123、0、45678</li>
 *     <li>小数：3.14、0.5、12.0</li>
 *     <li>带类型后缀：2.0f、42L、7s、255B</li>
 * </ul>
 * <p>
 * 语法允许在数字 (整数或小数) 末尾添加以下<strong>单字符后缀</strong>来显式指定常量类型：
 * <pre>b | s | l | f | d   // 分别对应 byte、short、long、float、double
 * B | S | L | F | D   // 同上，大小写皆可</pre>
 * 生成的 Token 类型始终为 {@code NUMBER_LITERAL}，词法单元将携带完整的文本（含后缀，若存在）。
 */
public class NumberTokenScanner extends AbstractTokenScanner {

    /**
     * 可选类型后缀字符集合 (大小写均可)。
     * 与 {@code ExpressionBuilder} 内的后缀解析逻辑保持一致。
     */
    private static final String SUFFIX_CHARS = "bslfdBSLFD";

    /**
     * 判断是否可以处理当前位置的字符。
     * <p>当字符为数字时，表示可能是数字字面量的起始。</p>
     *
     * @param c   当前字符
     * @param ctx 当前词法上下文
     * @return 如果为数字字符，则返回 true
     */
    @Override
    public boolean canHandle(char c, LexerContext ctx) {
        return Character.isDigit(c);
    }

    /**
     * 执行数字扫描逻辑。
     * <ol>
     *     <li>连续读取数字字符，允许出现<strong>一个</strong>小数点，用于识别整数或小数。</li>
     *     <li>读取完主体后，<strong>一次性</strong>检查下一个字符，若属于合法类型后缀则吸收。</li>
     * </ol>
     * 这样可以保证诸如 {@code 2.0f} 被视为一个整体的 {@code NUMBER_LITERAL}，
     * 而不是拆分成 "2.0" 与 "f" 两个 Token。
     *
     * @param ctx  词法上下文
     * @param line 当前行号
     * @param col  当前列号
     * @return 表示数字字面量的 Token
     */
    @Override
    protected Token scanToken(LexerContext ctx, int line, int col) {
        StringBuilder sb = new StringBuilder();
        boolean hasDot = false; // 标识是否已经遇到过小数点

        /*
         * 1️⃣ 扫描整数或小数主体
         * 允许出现一个小数点，其余必须是数字。
         */
        while (!ctx.isAtEnd()) {
            char c = ctx.peek();
            if (c == '.' && !hasDot) {
                hasDot = true;
                sb.append(ctx.advance());
            } else if (Character.isDigit(c)) {
                sb.append(ctx.advance());
            } else {
                break; // 遇到非数字/第二个点 => 主体结束
            }
        }

        /*
         * 2️⃣ 可选类型后缀
         * 如果下一字符是合法后缀字母，则一起纳入当前 Token。
         */
        if (!ctx.isAtEnd()) {
            char suffix = ctx.peek();
            if (SUFFIX_CHARS.indexOf(suffix) >= 0) {
                sb.append(ctx.advance());
            }
        }

        // 构造并返回 NUMBER_LITERAL Token，文本内容形如 "123", "3.14f" 等。
        return new Token(TokenType.NUMBER_LITERAL, sb.toString(), line, col);
    }
}
