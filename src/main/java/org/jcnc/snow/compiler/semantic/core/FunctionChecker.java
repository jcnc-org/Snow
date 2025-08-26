package org.jcnc.snow.compiler.semantic.core;

import org.jcnc.snow.compiler.parser.ast.DeclarationNode;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.ModuleNode;
import org.jcnc.snow.compiler.parser.ast.ReturnNode;
import org.jcnc.snow.compiler.semantic.analyzers.base.StatementAnalyzer;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.symbol.Symbol;
import org.jcnc.snow.compiler.semantic.symbol.SymbolKind;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code FunctionChecker} 是 Snow 编译器语义分析阶段用于检查所有函数体合法性的总控调度器。
 * <p>
 * <b>设计核心：</b>采用“两遍扫描”方案，彻底解决跨模块全局变量/常量类型推断和引用依赖问题：
 * <ul>
 *   <li><b>第一遍</b>：为所有模块预先构建并注册其全局符号表（globals），保证跨模块引用时可见。</li>
 *   <li><b>第二遍</b>：在全局符号表全部就绪后，依次分析所有模块的函数体，实现局部作用域、类型推断、语义校验等任务。</li>
 * </ul>
 * <b>功能职责：</b>
 * <ul>
 *   <li>遍历所有模块，先建立 globals，再遍历并检查所有函数体语句。</li>
 *   <li>为每个函数体构建完整符号表，并注册参数变量。</li>
 *   <li>分发每条语句到对应 {@link StatementAnalyzer} 进行类型检查和错误校验。</li>
 *   <li>自动检查非 void 函数 return 完备性。</li>
 *   <li>记录所有语义错误，便于前端高亮和诊断。</li>
 * </ul>
 *
 * @param ctx 全局语义分析上下文，持有模块信息、符号表、错误收集等资源
 */
public record FunctionChecker(Context ctx) {

    /**
     * 主入口：对所有模块的所有函数体进行语义检查（两遍扫描实现）。
     * <p>
     * <b>第一遍</b>：为每个模块提前构建全局符号表（包含本模块所有全局变量和常量），
     * 并注册到 {@link ModuleInfo}，确保跨模块引用时所有全局符号都已可用。
     * <br>
     * <b>第二遍</b>：遍历所有模块的所有函数，对每个函数体：
     * <ul>
     *   <li>构建局部作用域，父作用域为对应模块的 globals；</li>
     *   <li>注册参数变量；</li>
     *   <li>依次分发每条语句到对应 {@link StatementAnalyzer}，进行类型和语义检查；</li>
     *   <li>自动校验非 void 函数 return 完备性；</li>
     *   <li>将所有发现的问题统一记录到 {@link SemanticError} 列表。</li>
     * </ul>
     *
     * @param mods 所有模块的 AST 根节点集合
     */
    public void check(Iterable<ModuleNode> mods) {
        List<ModuleNode> moduleList = new ArrayList<>();
        // ---------- 第1遍：收集所有全局符号表 ----------
        for (ModuleNode mod : mods) {
            moduleList.add(mod);

            // 获取当前模块的元信息
            ModuleInfo mi = ctx.modules().get(mod.name());
            // 创建本模块全局作用域（无父作用域）
            SymbolTable globalScope = new SymbolTable(null);

            // 注册所有全局变量/常量到符号表
            for (DeclarationNode g : mod.globals()) {
                var t = ctx.parseType(g.getType());
                SymbolKind k = g.isConst() ? SymbolKind.CONSTANT : SymbolKind.VARIABLE;
                String dupType = g.isConst() ? "常量" : "变量";
                // 检查重复声明
                if (!globalScope.define(new Symbol(g.getName(), t, k))) {
                    ctx.errors().add(new SemanticError(
                            g,
                            dupType + "重复声明: " + g.getName()
                    ));
                }
            }
            // 注册到模块信息，供跨模块引用
            mi.setGlobals(globalScope);
        }

        // ---------- 第2遍：遍历所有函数，分析函数体 ----------
        for (ModuleNode mod : moduleList) {
            ModuleInfo mi = ctx.modules().get(mod.name());
            SymbolTable globalScope = mi.getGlobals();

            for (FunctionNode fn : mod.functions()) {
                // 构建函数局部作用域，父作用域为 globalScope
                SymbolTable locals = new SymbolTable(globalScope);

                // 注册函数参数为局部变量
                fn.parameters().forEach(p ->
                        locals.define(new Symbol(
                                p.name(),
                                ctx.parseType(p.type()),
                                SymbolKind.VARIABLE
                        ))
                );

                // 分析函数体内每条语句
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
