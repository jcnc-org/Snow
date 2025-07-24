package org.jcnc.snow.compiler.lexer.utils;

import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;

import java.util.List;

/**
 * TokenPrinter 用于以表格形式将词法分析生成的 Token 列表输出到控制台。
 * <p>
 * 输出包含每个 Token 的行号、列号、类型以及词素（lexeme），
 * 并对换行、制表符等特殊字符进行转义显示。
 * </p>
 */
public class TokenPrinter {

    /**
     * 将给定的 Token 列表打印到标准输出（控制台）。
     * <p>
     * 输出格式: 
     * <pre>
     * line   col    type             lexeme
     * ----------------------------------------------------
     * 1      1      KEYWORD          module
     * 1      7      IDENTIFIER       MyModule
     * ...
     * </pre>
     * 并且对 lexeme 中的换行符、制表符、回车符等进行转义（\n、\t、\r），
     * 以便在表格中保持排版整齐。如果遇到类型为 {@link TokenType#NEWLINE} 的 Token，
     * 会在该行后额外插入一个空行以增强可读性。
     *
     * @param tokens 要打印的 Token 列表，不应为 null；列表中的每个元素
     *               都应包含有效的行号、列号、类型和词素信息
     */
    public static void print(List<Token> tokens) {
        // 打印表头: 列名对齐，宽度分别为 6、6、16
        System.out.printf("%-6s %-6s %-16s %s%n", "line", "col", "type", "lexeme");
        System.out.println("----------------------------------------------------");

        // 逐个 Token 输出对应信息
        for (Token token : tokens) {
            // 对 lexeme 中的特殊字符进行转义，避免表格错位
            String lexeme = token.getLexeme()
                    .replace("\n", "\\n")
                    .replace("\t", "\\t")
                    .replace("\r", "\\r");

            // 按照固定格式输出: 行号、列号、类型、词素
            System.out.printf("%-6d %-6d %-16s %s%n",
                    token.getLine(),
                    token.getCol(),
                    token.getType(),
                    lexeme
            );

            // 如果当前 Token 是换行符类型，则额外打印一行空白行
            if (token.getType() == TokenType.NEWLINE) {
                System.out.println();
            }
        }
    }

}
