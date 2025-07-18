package org.jcnc.snow.compiler.lexer.scanners;

import org.jcnc.snow.compiler.lexer.core.LexerContext;
import org.jcnc.snow.compiler.lexer.core.LexicalException;
import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenFactory;
import org.jcnc.snow.compiler.lexer.token.TokenType;

/**
 * {@code IdentifierTokenScanner} —— 标识符扫描器，负责识别源代码中的标识符（如变量名、函数名等）。
 *
 * <p>标识符的识别遵循以下规则: </p>
 * <ul>
 *     <li>标识符必须以字母（A-Z，a-z）或下划线（_）开头。</li>
 *     <li>标识符的后续字符可以是字母、数字（0-9）或下划线。</li>
 * </ul>
 *
 * <p>在扫描过程中，标识符会被处理为一个 {@link Token} 对象。如果该标识符是一个关键字，
 * 扫描器会通过 {@link TokenFactory} 自动识别并返回相应的 {@link TokenType}。</p>
 *
 * <p>本扫描器实现了一个有限状态机（FSM），它能够在不同状态之间转换，确保标识符的正确识别。</p>
 */
public class IdentifierTokenScanner extends AbstractTokenScanner {

    /**
     * 判断当前字符是否可以作为标识符的起始字符。
     * <p>如果字符为字母或下划线，则认为是标识符的开始。</p>
     *
     * @param c   当前字符
     * @param ctx 当前词法上下文
     * @return 如果字符是标识符的起始字符，则返回 {@code true}；否则返回 {@code false}。
     */
    @Override
    public boolean canHandle(char c, LexerContext ctx) {
        return Character.isLetter(c) || c == '_';
    }

    /**
     * 执行标识符扫描。
     * <p>使用状态机模式扫描标识符。首先从初始状态开始，读取标识符的起始字符（字母或下划线）。
     * 然后，进入标识符状态，继续读取标识符字符（字母、数字或下划线）。一旦遇到不符合标识符规则的字符，
     * 标识符扫描结束，返回一个 {@link Token}。</p>
     *
     * @param ctx  词法上下文，用于获取字符流
     * @param line 当前行号（1 基）
     * @param col  当前列号（1 基）
     * @return 返回一个包含标识符或关键字的 {@link Token} 对象。
     * @throws LexicalException 如果标识符以非法字符（如点号）开头，则抛出异常
     */
    @Override
    protected Token scanToken(LexerContext ctx, int line, int col) {
        StringBuilder lexeme = new StringBuilder(); // 用于构建标识符的字符串
        State currentState = State.INITIAL; // 初始状态

        // 遍历字符流，直到遇到不合法的字符或流结束
        while (!ctx.isAtEnd()) {
            char currentChar = ctx.peek(); // 获取当前字符
            switch (currentState) {
                case INITIAL:
                    // 初始状态，标识符开始
                    if (Character.isLetter(currentChar) || currentChar == '_') {
                        lexeme.append(ctx.advance()); // 接受当前字符
                        currentState = State.IDENTIFIER; // 进入标识符状态
                    } else {
                        return null; // 当前字符不符合标识符的规则，返回 null
                    }
                    break;

                case IDENTIFIER:
                    // 标识符状态，继续读取合法标识符字符
                    if (Character.isLetterOrDigit(currentChar) || currentChar == '_') {
                        lexeme.append(ctx.advance()); // 继续接受合法字符
                    } else {
                        // 当前字符不符合标识符的规则，标识符结束，返回 token
                        return TokenFactory.create(lexeme.toString(), line, col);
                    }
                    break;
            }
        }

        // 如果字符流结束，返回标识符 token
        return TokenFactory.create(lexeme.toString(), line, col);
    }

    /**
     * 枚举类型表示标识符扫描的状态。
     */
    private enum State {
        INITIAL,  // 初始状态，等待标识符的开始
        IDENTIFIER // 标识符状态，继续读取标识符字符
    }
}
