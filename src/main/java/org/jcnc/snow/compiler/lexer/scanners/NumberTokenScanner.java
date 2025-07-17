package org.jcnc.snow.compiler.lexer.scanners;

import org.jcnc.snow.compiler.lexer.core.LexerContext;
import org.jcnc.snow.compiler.lexer.core.LexicalException;
import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;

/**
 * NumberTokenScanner —— 基于有限状态机（FSM）的数字字面量解析器。
 * <p>
 * 该扫描器负责将源码中的数字字符串切分为 NUMBER_LITERAL token，当前支持:
 * <ol>
 *   <li>十进制整数（如 0，42，123456）</li>
 *   <li>十进制小数（如 3.14，0.5）</li>
 *   <li>单字符类型后缀（如 2.0f，255B，合法集合见 SUFFIX_CHARS）</li>
 * </ol>
 * <p>
 * 如果后续需要支持科学计数法、下划线分隔符、不同进制等，只需扩展现有状态机的转移规则。
 *
 * <pre>
 * 状态机简述:
 *   INT_PART   --'.'-->  DEC_POINT
 *      |                 |
 *      |                 v
 *     else------------> END
 *      |
 *      v
 *   DEC_POINT  --digit--> FRAC_PART
 * </pre>
 * 状态说明:
 * <ul>
 *   <li>INT_PART   : 读取整数部分，遇到 '.' 进入 DEC_POINT，否则结束。</li>
 *   <li>DEC_POINT  : 已读到小数点，必须下一个字符是数字，否则报错。</li>
 *   <li>FRAC_PART  : 读取小数部分，遇非法字符则结束主体。</li>
 *   <li>END        : 主体扫描结束，进入后缀/尾随字符判定。</li>
 * </ul>
 * <p>
 * 错误处理策略:
 * <ol>
 *   <li>数字后跟未知字母（如 42X）—— 抛出 LexicalException</li>
 *   <li>数字与合法后缀间有空白（如 3 L）—— 抛出 LexicalException</li>
 *   <li>小数点后缺失数字（如 1.）—— 抛出 LexicalException</li>
 * </ol>
 * <p>
 * 支持的单字符类型后缀包括: b, s, l, f, d 及其大写形式。若需支持多字符后缀，可将该集合扩展为 Set<String>。
 */
public class NumberTokenScanner extends AbstractTokenScanner {

    /**
     * 支持的单字符类型后缀集合。
     * 包含: b, s, l, f, d 及其大写形式。
     * 对于多字符后缀，可扩展为 Set<String> 并在扫描尾部做贪婪匹配。
     */
    private static final String SUFFIX_CHARS = "bslfdBSLFD";

    /**
     * 判断是否由该扫描器处理。
     * 仅当首字符为数字时，NumberTokenScanner 介入处理。
     *
     * @param c   当前待判断字符
     * @param ctx 当前 LexerContext
     * @return 如果为数字返回 true，否则返回 false
     */
    @Override
    public boolean canHandle(char c, LexerContext ctx) {
        return Character.isDigit(c);
    }

    /**
     * 按照有限状态机读取完整数字字面量，并对尾随字符进行合法性校验。
     *
     * @param ctx  当前 LexerContext
     * @param line 源码起始行号（1 基）
     * @param col  源码起始列号（1 基）
     * @return NUMBER_LITERAL 类型的 Token
     * @throws LexicalException 如果遇到非法格式或未受支持的尾随字符
     */
    @Override
    protected Token scanToken(LexerContext ctx, int line, int col) throws LexicalException {
        StringBuilder literal = new StringBuilder();
        State state = State.INT_PART;

        boolean lastWasUnderscore = false; // 记录前一个是否是下划线
        boolean sawDigit = false;          // 当前段落是否有数字（防止以下划线开头）

        /* ───── 1. 主体扫描 —— 整数 / 小数 ───── */
        mainLoop:
        while (!ctx.isAtEnd() && state != State.END) {
            char ch = ctx.peek();
            switch (state) {
                /* 整数部分 */
                case INT_PART:
                    if (Character.isDigit(ch)) {
                        literal.append(ctx.advance());
                        lastWasUnderscore = false;
                        sawDigit = true;
                    } else if (ch == '_') {
                        if (!sawDigit)
                            throw new LexicalException("数字不能以下划线开头", line, col);
                        if (lastWasUnderscore)
                            throw new LexicalException("数字中下划线不能连续出现", line, col);
                        literal.append(ctx.advance());
                        lastWasUnderscore = true;
                    } else if (ch == '.') {
                        if (lastWasUnderscore)
                            throw new LexicalException("下划线不能出现在小数点前", line, col);
                        state = State.DEC_POINT;
                        literal.append(ctx.advance());
                        // 不要重置sawDigit！
                        // sawDigit = false;  // 移除此句
                    } else {
                        if (lastWasUnderscore)
                            throw new LexicalException("数字不能以下划线结尾", line, col);
                        state = State.END;
                    }
                    break;

                /* 已读到小数点，下一字符必须是数字 */
                case DEC_POINT:
                    if (Character.isDigit(ch)) {
                        literal.append(ctx.advance());
                        state = State.FRAC_PART;
                        sawDigit = true;
                    } else if (ch == '_') { // 防止小数点后直接跟下划线
                        throw new LexicalException("小数点后不能直接跟下划线", line, col);
                    } else {
                        throw new LexicalException("小数点后必须跟数字", line, col);
                    }
                    break;

                /* 小数部分 */

                case FRAC_PART:
                    if (Character.isDigit(ch)) {
                        literal.append(ctx.advance());
                        lastWasUnderscore = false;
                    } else if (ch == '_') { // 小数部分下划线检查
                        if (lastWasUnderscore)
                            throw new LexicalException("数字中下划线不能连续出现", line, col);
                        literal.append(ctx.advance());
                        lastWasUnderscore = true;
                    } else {
                        if (lastWasUnderscore)
                            throw new LexicalException("数字不能以下划线结尾", line, col);
                        state = State.END;
                    }
                    break;

                default:
                    break mainLoop;
            }
        }

        // 主体结束后，下划线不能在末尾
        if (lastWasUnderscore)
            throw new LexicalException("数字不能以下划线结尾", line, col);

        /* ───── 2. 后缀及非法尾随字符检查 ───── */
        if (!ctx.isAtEnd()) {
            char next = ctx.peek();

            /* 2-A. 合法单字符后缀（紧邻数字，无空格） */
            if (SUFFIX_CHARS.indexOf(next) >= 0) {
                literal.append(ctx.advance());

                /* 后缀只能出现一次：再次出现字母/数字/点即视为非法 */
                if (!ctx.isAtEnd()) {
                    char peekAfterSuffix = ctx.peek();
                    if (Character.isLetter(peekAfterSuffix)
                            || Character.isDigit(peekAfterSuffix)
                            || peekAfterSuffix == '.') {
                        throw new LexicalException(
                                "数字类型后缀只能是单字符，非法续接 '" + peekAfterSuffix + "'",
                                line, col);
                    }
                }

                /* 2-B. **非法字母**（既不是后缀，也没有空白隔开） */
            } else if (Character.isLetter(next)) {
                throw new LexicalException(
                        "数字后不能紧跟未知标识符 '" + next + "'", line, col);
                /* 2-C. **非法下划线** */
            } else if (next == '_') {
                throw new LexicalException(
                        "数字后不能紧跟下划线 '_'", line, col);
            }
            /* 其余情况交由外层扫描器处理（包括空白及其它符号） */
        }

        /* ───── 3. 生成并返回 Token ───── */
        return new Token(TokenType.NUMBER_LITERAL, literal.toString().replace("_", ""), line, col);
    }

    /**
     * FSM 内部状态定义
     */
    private enum State {
        /**
         * 整数部分
         */
        INT_PART,
        /**
         * 已读到小数点，但还未读到第一位小数数字
         */
        DEC_POINT,
        /**
         * 小数部分
         */
        FRAC_PART,
        /**
         * 主体结束，准备处理后缀或交还控制权
         */
        END
    }
}
