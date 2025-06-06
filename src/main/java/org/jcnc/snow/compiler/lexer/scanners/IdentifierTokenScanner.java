package org.jcnc.snow.compiler.lexer.scanners;

import org.jcnc.snow.compiler.lexer.core.LexerContext;
import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenFactory;

/**
 * 标识符扫描器：处理标识符的识别，如变量名、函数名等。
 * <p>
 * 识别规则如下：
 * <ul>
 *     <li>必须以字母或下划线（_）开头</li>
 *     <li>后续字符可以是字母、数字或下划线</li>
 * </ul>
 * <p>
 * 扫描完成后会调用 {@link TokenFactory} 自动判断是否为关键字，
 * 并返回对应类型的 {@link Token}。
 */
public class IdentifierTokenScanner extends AbstractTokenScanner {

    /**
     * 判断是否可以处理当前位置的字符。
     * <p>如果字符为字母或下划线，则认为是标识符的起始。</p>
     *
     * @param c   当前字符
     * @param ctx 当前词法上下文
     * @return 如果是标识符起始字符，则返回 true
     */
    @Override
    public boolean canHandle(char c, LexerContext ctx) {
        return Character.isLetter(c) || c == '_';
    }

    /**
     * 执行标识符的扫描逻辑。
     * <p>连续读取满足标识符规则的字符序列，交由 {@code TokenFactory} 创建对应的 Token。</p>
     *
     * @param ctx  词法上下文
     * @param line 当前行号
     * @param col  当前列号
     * @return 标识符或关键字类型的 Token
     */
    @Override
    protected Token scanToken(LexerContext ctx, int line, int col) {
        String lexeme = readWhile(ctx, ch -> Character.isLetterOrDigit(ch) || ch == '_');
        return TokenFactory.create(lexeme, line, col);
    }
}
