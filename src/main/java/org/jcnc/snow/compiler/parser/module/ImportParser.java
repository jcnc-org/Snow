package org.jcnc.snow.compiler.parser.module;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.ImportNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.context.TokenStream;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code ImportParser} 用于解析源码中的 import 导入语句，支持逗号分隔与多级模块名。
 *
 * <p><b>支持语法</b>：</p>
 * <pre>
 * import: foo
 * import: foo, bar, baz
 * import: os.file
 * import: net.http.client
 * </pre>
 *
 * <p>
 * 每个导入将解析为 {@link ImportNode}，其 {@code moduleName} 字段为完整点分名称（如 "os.file"）。
 * 该名称可直接映射到源文件路径（如 {@code os/file.snow}）。
 * </p>
 *
 * <p><b>用法</b>：调用 {@link #parse(ParserContext)} 解析一行 import，返回本行所有导入节点。</p>
 */
public class ImportParser {

    /**
     * 解析一行 import 语句（以换行结束），返回所有导入节点。
     *
     * @param ctx 解析上下文
     * @return 导入节点列表，每个节点包含模块名和位置信息
     */
    public List<ImportNode> parse(ParserContext ctx) {
        TokenStream ts = ctx.getTokens();

        // 'import' ':'
        ts.expect("import");
        ts.expect(":");

        List<ImportNode> imports = new ArrayList<>();

        do {
            // 每个模块名的起始位置信息
            int line = ts.peek().getLine();
            int column = ts.peek().getCol();
            String file = ctx.getSourceName();

            // 解析形如 IDENT ('.' IDENT)*
            String moduleName = parseQualifiedName(ts);

            imports.add(new ImportNode(moduleName, new NodeContext(line, column, file)));
        } while (ts.match(",")); // 逗号分隔的多个导入

        // 行结束
        ts.expectType(TokenType.NEWLINE);
        return imports;
    }

    /**
     * 解析由一个或多个标识符通过点号连接的限定名（qualified name）。
     * 形如：IDENT ('.' IDENT)*
     *
     * @param ts 词法流
     * @return 完整点分模块名
     */
    private String parseQualifiedName(TokenStream ts) {
        StringBuilder sb = new StringBuilder();
        // 第一个标识符
        sb.append(ts.expectType(TokenType.IDENTIFIER).getLexeme());
        // 后续 .IDENTIFIER
        while (ts.match(".")) {
            sb.append('.').append(ts.expectType(TokenType.IDENTIFIER).getLexeme());
        }
        return sb.toString();
    }
}
