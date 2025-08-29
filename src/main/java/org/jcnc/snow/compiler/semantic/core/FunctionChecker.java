package org.jcnc.snow.compiler.semantic.core;

import org.jcnc.snow.compiler.parser.ast.DeclarationNode;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.ModuleNode;
import org.jcnc.snow.compiler.parser.ast.ReturnNode;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;
import org.jcnc.snow.compiler.semantic.analyzers.base.StatementAnalyzer;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.symbol.Symbol;
import org.jcnc.snow.compiler.semantic.symbol.SymbolKind;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * 对所有模块的函数体进行两遍扫描的语义检查：
 * 1) 先为每个模块建立全局符号表（globals），注册模块级变量/常量；
 * 2) 再在全局表就绪后，依次分析各函数体。
 */
public record FunctionChecker(Context ctx) {

    public void check(Iterable<ModuleNode> mods) {
        List<ModuleNode> moduleList = new ArrayList<>();

        // ---------- 第一遍：构建并注册各模块全局符号表 ----------
        for (ModuleNode mod : mods) {
            ctx.setCurrentModule(mod.name());
            moduleList.add(mod);

            ModuleInfo mi = ctx.modules().get(mod.name());
            SymbolTable globalScope = new SymbolTable(null);

            for (DeclarationNode g : mod.globals()) {
                Type t = ctx.parseType(g.getType());
                if (t == null) {
                    ctx.errors().add(new SemanticError(g, "未知类型: " + g.getType()));
                    t = BuiltinType.INT; // 兜底，避免后续 NPE
                }
                SymbolKind kind = g.isConst() ? SymbolKind.CONSTANT : SymbolKind.VARIABLE;
                String dupType = g.isConst() ? "常量" : "变量";
                if (!globalScope.define(new Symbol(g.getName(), t, kind))) {
                    ctx.errors().add(new SemanticError(g, dupType + "重复声明: " + g.getName()));
                }
            }
            mi.setGlobals(globalScope);
        }

        // ---------- 第二遍：遍历各模块函数并分析函数体 ----------
        for (ModuleNode mod : moduleList) {
            ctx.setCurrentModule(mod.name());
            ModuleInfo mi = ctx.modules().get(mod.name());
            SymbolTable globalScope = mi.getGlobals();

            for (FunctionNode fn : mod.functions()) {
                // 构建函数局部作用域：父作用域为全局
                SymbolTable locals = new SymbolTable(globalScope);

                // 注册函数参数为局部变量
                fn.parameters().forEach(p -> {
                    Type t = ctx.parseType(p.type());
                    if (t == null) {
                        ctx.errors().add(new SemanticError(p, "未知类型: " + p.type()));
                        t = BuiltinType.INT;
                    }
                    locals.define(new Symbol(p.name(), t, SymbolKind.VARIABLE));
                });

                // 分析函数体语句 —— 关键修复：传“实例”而不是 Class
                for (StatementNode stmt : fn.body()) {
                    @SuppressWarnings("unchecked")
                    StatementAnalyzer<StatementNode> analyzer =
                            (StatementAnalyzer<StatementNode>) ctx.getRegistry().getStatementAnalyzer(stmt);
                    if (analyzer != null) {
                        analyzer.analyze(ctx, mi, fn, locals, stmt);
                    } else {
                        ctx.errors().add(new SemanticError(stmt, "不支持的语句类型: " + stmt));
                    }
                }

                // 非 void 的函数必须至少包含一条 return
                Type ret = ctx.parseType(fn.returnType());
                if (ret != null && ret != BuiltinType.VOID) {
                    boolean hasReturn = fn.body().stream().anyMatch(s -> s instanceof ReturnNode);
                    if (!hasReturn) {
                        ctx.errors().add(new SemanticError(fn, "非 void 函数必须包含至少一条 return 语句"));
                    }
                }
            }
        }
    }
}
