package org.jcnc.snow.compiler.semantic.core;

import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.ModuleNode;
import org.jcnc.snow.compiler.parser.ast.DeclarationNode;
import org.jcnc.snow.compiler.parser.ast.ReturnNode;
import org.jcnc.snow.compiler.semantic.analyzers.base.StatementAnalyzer;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.symbol.Symbol;
import org.jcnc.snow.compiler.semantic.symbol.SymbolKind;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;

/**
 * {@code FunctionChecker} 是语义分析阶段中用于检查函数体语句合法性的调度器。
 * <p>
 * 它逐个遍历所有模块中的函数定义，并对函数体中的每一条语句调用对应的语义分析器，
 * 执行类型检查、作用域验证、错误记录等任务。
 * <p>
 * 核心职责包括:
 * <ul>
 *   <li>为每个函数构建局部符号表并注册函数参数为变量；</li>
 *   <li>分发函数体语句至相应的 {@link StatementAnalyzer}；</li>
 *   <li>记录未支持语句类型为语义错误；</li>
 *   <li>依赖上下文 {@link Context} 提供模块信息、类型解析、错误收集等服务。</li>
 * </ul>
 *
 * @param ctx 全局语义分析上下文，提供模块信息、注册表、错误记录等支持
 */
public record FunctionChecker(Context ctx) {

    /**
     * 构造函数体检查器。
     *
     * @param ctx 当前语义分析上下文
     */
    public FunctionChecker {
    }

    /**
     * 执行函数体检查流程。
     * <p>
     * 对所有模块中的所有函数依次进行处理:
     * <ol>
     *   <li>查找模块对应的 {@link ModuleInfo}；</li>
     *   <li>创建函数局部符号表 {@link SymbolTable}，并注册所有参数变量；</li>
     *   <li>对函数体中的每一条语句分发到已注册的分析器进行语义分析；</li>
     *   <li>若某条语句无可用分析器，则记录为 {@link SemanticError}。</li>
     * </ol>
     *
     * @param mods 所有模块的 AST 根节点集合
     */
    public void check(Iterable<ModuleNode> mods) {
        for (ModuleNode mod : mods) {
            // 获取当前模块对应的语义信息
            ModuleInfo mi = ctx.modules().get(mod.name());

            // 先构建全局符号表
            SymbolTable globalScope = new SymbolTable(null);
            // 根据 isConst() 决定种类
            for (DeclarationNode g : mod.globals()) {
                var t = ctx.parseType(g.getType());
                SymbolKind k = g.isConst() ? SymbolKind.CONSTANT : SymbolKind.VARIABLE;

                // 错误信息按常量/变量区分
                String dupType = g.isConst() ? "常量" : "变量";
                if (!globalScope.define(new Symbol(g.getName(), t, k))) {
                    ctx.errors().add(new SemanticError(
                            g,
                            dupType + "重复声明: " + g.getName()
                    ));
                }
            }

            // 遍历模块中所有函数定义
            for (FunctionNode fn : mod.functions()) {

                // 构建函数局部作用域符号表，父作用域为 globalScope
                SymbolTable locals = new SymbolTable(globalScope);

                // 将函数参数注册为局部变量
                fn.parameters().forEach(p ->
                        locals.define(new Symbol(
                                p.name(),
                                ctx.parseType(p.type()),
                                SymbolKind.VARIABLE
                        ))
                );

                // 遍历并分析函数体内的每条语句
                for (var stmt : fn.body()) {
                    var analyzer = ctx.getRegistry().getStatementAnalyzer(stmt);
                    if (analyzer != null) {
                        analyzer.analyze(ctx, mi, fn, locals, stmt);
                    } else {
                        ctx.errors().add(new SemanticError(
                                stmt,
                                "不支持的语句类型: " + stmt
                        ));
                    }
                }

                // 检查非 void 函数是否至少包含一条 return 语句
                var returnType = ctx.parseType(fn.returnType());
                if (returnType != BuiltinType.VOID) {
                    boolean hasReturn = fn.body().stream()
                            .anyMatch(stmtNode -> stmtNode instanceof ReturnNode);
                    if (!hasReturn) {
                        ctx.errors().add(new SemanticError(
                                fn,
                                "非 void 函数必须包含至少一条 return 语句"
                        ));
                    }
                }
            }
        }
    }
}
