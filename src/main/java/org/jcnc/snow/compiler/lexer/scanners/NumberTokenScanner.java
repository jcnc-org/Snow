package org.jcnc.snow.compiler.lexer.scanners;

import org.jcnc.snow.compiler.lexer.core.LexerContext;
import org.jcnc.snow.compiler.lexer.core.LexicalException;
import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;

/**
 * NumberTokenScanner —— 基于有限状态机（FSM）的数字字面量解析器。
 * <p>
 * 该扫描器负责将源码中的数字字符串切分为 NUMBER_LITERAL token，当前支持：
 * <ol>
 *   <li>十进制整数（如 0，42，123456）</li>
 *   <li>十进制小数（如 3.14，0.5）</li>
 *   <li>单字符类型后缀（如 2.0f，255B，合法集合见 SUFFIX_CHARS）</li>
 * </ol>
 * <p>
 * 如果后续需要支持科学计数法、下划线分隔符、不同进制等，只需扩展现有状态机的转移规则。
 * <pre>
 * 状态机简述：
 *   INT_PART   --'.'-->  DEC_POINT
 *      |                 |
 *      |                 v
 *     else------------> END
 *      |
 *      v
 *   DEC_POINT  --digit--> FRAC_PART
 *                         |
 *                         v
 *                     else--> END
 * </pre>
 * 状态说明：
 * <ul>
 *   <li>INT_PART   ：读取整数部分，遇到 '.' 进入 DEC_POINT，否则结束。</li>
 *   <li>DEC_POINT  ：已读到小数点，必须下一个字符是数字，否则报错。</li>
 *   <li>FRAC_PART  ：读取小数部分，遇非法字符则结束主体。</li>
 *   <li>END        ：主体扫描结束，进入后缀/尾随字符判定。</li>
 * </ul>
 * 错误处理策略：
 * <ol>
 *   <li>数字后跟未知字母（如 42X）—— 抛出 LexicalException</li>
 *   <li>数字与合法后缀间有空白（如 3 L）—— 抛出 LexicalException</li>
 *   <li>小数点后缺失数字（如 1.）—— 抛出 LexicalException</li>
 * </ol>
 * 支持的单字符类型后缀包括：b, s, l, f, d 及其大写形式。若需支持多字符后缀，可将该集合扩展为 Set<String>。
 */
public class NumberTokenScanner extends AbstractTokenScanner {

    /**
     * 支持的单字符类型后缀集合。
     * 包含：b, s, l, f, d 及其大写形式。
     * 对于多字符后缀，可扩展为 Set<String> 并在扫描尾部做贪婪匹配。
     */
    private static final String SUFFIX_CHARS = "bslfdBSLFD";

    /**
     * 判断是否由该扫描器处理。
     * 仅当首字符为数字时，NumberTokenScanner 介入处理。
     *
     * @param c   当前待判断字符
     * @param ctx 当前 LexerContext（可用于进一步判断）
     * @return 如果为数字返回 true，否则返回 false
     */
    @Override
    public boolean canHandle(char c, LexerContext ctx) {
        return Character.isDigit(c);
    }

    /**
     * 按照有限状态机读取完整数字字面量，并对尾随字符进行合法性校验。
     * <p>
     * 主体流程：
     * <ol>
     * <li>整数部分、可选小数点和小数部分扫描。</li>
     * <li>检查合法的类型后缀。</li>
     * <li>检查非法尾随字符，如未知字母、空白后缀或非法 '/'。</li>
     * <li>生成并返回 NUMBER_LITERAL Token。</li>
     * </ol>
     *
     * @param ctx  当前 LexerContext（提供游标、前瞻等功能）
     * @param line 源码起始行号（1 基）
     * @param col  源码起始列号（1 基）
     * @return NUMBER_LITERAL 类型的 Token
     * @throws LexicalException 如果遇到非法格式或未受支持的尾随字符
     */
    @Override
    protected Token scanToken(LexerContext ctx, int line, int col) {
        StringBuilder literal = new StringBuilder();
        State state = State.INT_PART;

        // 1. 主体扫描 —— 整数 / 小数
        mainLoop:
        while (!ctx.isAtEnd() && state != State.END) {
            char ch = ctx.peek();
            switch (state) {
                case INT_PART:
                    if (Character.isDigit(ch)) {
                        literal.append(ctx.advance());
                    } else if (ch == '.') {
                        state = State.DEC_POINT;
                        literal.append(ctx.advance());
                    } else {
                        state = State.END; // 整数已结束
                    }
                    break;

                case DEC_POINT:
                    if (Character.isDigit(ch)) {
                        state = State.FRAC_PART;
                        literal.append(ctx.advance());
                    } else {
                        // 如 "1." —— 语言规范不允许尾点数字
                        throw new LexicalException("小数点后必须跟数字", line, col);
                    }
                    break;

                case FRAC_PART:
                    if (Character.isDigit(ch)) {
                        literal.append(ctx.advance());
                    } else {
                        state = State.END; // 小数字符串结束
                    }
                    break;

                default:
                    break mainLoop; // 理论不会到达
            }
        }

        // 2. 后缀及非法尾随字符检查
        if (!ctx.isAtEnd()) {
            char next = ctx.peek();

            // 2‑A. 合法单字符后缀
            if (SUFFIX_CHARS.indexOf(next) >= 0) {
                literal.append(ctx.advance());
            }
            // 2‑B. 紧跟未知字母（如 42X）
            else if (Character.isLetter(next)) {
                throw new LexicalException("未知的数字类型后缀 '" + next + "'", line, col);
            }
            // 2‑C. 数字后出现空白 + 字母（如 3 L）
            else if (Character.isWhitespace(next) && next != '\n') {
                int off = 1;
                char look;
                while (true) {
                    look = ctx.peekAhead(off);
                    if (look == '\n' || look == '\0') break; // 行尾或 EOF
                    if (!Character.isWhitespace(look)) break;
                    off++;
                }
                if (Character.isLetter(look)) {
                    throw new LexicalException("数字字面量与类型后缀之间不允许有空白符", line, col);
                }
            }
            // 其他字符（分号、运算符、括号等）留给外层扫描流程处理
        }

        // 3. 生成并返回 Token
        return new Token(TokenType.NUMBER_LITERAL, literal.toString(), line, col);
    }

    /**
     * FSM 内部状态。
     * 每次读取一个字符后，根据“当前状态 + 当前字符”决定转移。
     */
    private enum State {
        /**
         * 整数部分（尚未读到小数点）
         */
        INT_PART,
        /**
         * 已读到小数点，但还未读到第一位小数数字
         */
        DEC_POINT,
        /**
         * 小数部分（小数点右侧）
         */
        FRAC_PART,
        /**
         * 主体结束，准备处理后缀或交还控制权
         */
        END
    }
}
