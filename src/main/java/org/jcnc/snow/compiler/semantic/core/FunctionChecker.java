package org.jcnc.snow.compiler.semantic.core;

import org.jcnc.snow.compiler.parser.ast.DeclarationNode;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.IfNode;
import org.jcnc.snow.compiler.parser.ast.LoopNode;
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
 * {@code FunctionChecker} 负责对所有模块的函数体进行两遍扫描式的语义检查。
 * <p>
 * 检查流程为：
 * <ol>
 *     <li>第一遍：为每个模块构建全局符号表，注册所有模块级变量与常量声明（并检查重复/未知类型）。</li>
 *     <li>第二遍：在所有全局符号表准备好后，依次分析各模块下所有函数的参数和函数体语句。</li>
 * </ol>
 * 检查要点：
 * <ul>
 *     <li>类型未知或变量/常量重复声明时，均收集为语义错误。</li>
 *     <li>所有函数参数注册为局部变量。</li>
 *     <li>所有函数体语句分派到对应 StatementAnalyzer 实例做分析。</li>
 *     <li>非 void 返回类型的函数，必须有至少一条 return。</li>
 * </ul>
 */
public record FunctionChecker(Context ctx) {

    /**
     * 对传入的所有模块做函数体的两遍扫描式语义检查。
     *
     * @param mods 所有待分析的模块 AST 节点集合
     */
    public void check(Iterable<ModuleNode> mods) {
        List<ModuleNode> moduleList = new ArrayList<>();

        // ---------- 第一遍：构建并注册各模块全局符号表 ----------
        for (ModuleNode mod : mods) {
            ctx.setCurrentModule(mod.name());   // 标记当前模块
            moduleList.add(mod);

            ModuleInfo mi = ctx.modules().get(mod.name());
            SymbolTable globalScope = new SymbolTable(null); // 模块级全局作用域

            // 处理所有全局变量/常量声明
            for (DeclarationNode g : mod.globals()) {
                Type t = ctx.parseType(g.getType()); // 解析声明类型
                if (t == null) {
                    // 类型未知，记录错误，兜底为 int 类型，避免后续 NullPointer
                    ctx.errors().add(new SemanticError(g, "未知类型: " + g.getType()));
                    t = BuiltinType.INT;
                }
                SymbolKind kind = g.isConst() ? SymbolKind.CONSTANT : SymbolKind.VARIABLE;
                String dupType = g.isConst() ? "常量" : "变量";
                // 注册符号表（防止重名）
                if (!globalScope.define(new Symbol(g.getName(), t, kind))) {
                    ctx.errors().add(new SemanticError(g, dupType + "重复声明: " + g.getName()));
                }
            }
            // 将全局符号表挂载到模块信息对象
            mi.setGlobals(globalScope);
        }

        // ---------- 第二遍：遍历各模块函数并分析函数体 ----------
        for (ModuleNode mod : moduleList) {
            ctx.setCurrentModule(mod.name());
            ModuleInfo mi = ctx.modules().get(mod.name());
            SymbolTable globalScope = mi.getGlobals(); // 全局作用域

            for (FunctionNode fn : mod.functions()) {
                // 构建函数的局部作用域（父作用域为模块全局）
                SymbolTable locals = new SymbolTable(globalScope);

                // 注册所有函数参数到局部作用域，类型未知时兜底为 int
                fn.parameters().forEach(p -> {
                    Type t = ctx.parseType(p.type());
                    if (t == null) {
                        ctx.errors().add(new SemanticError(p, "未知类型: " + p.type()));
                        t = BuiltinType.INT;
                    }
                    locals.define(new Symbol(p.name(), t, SymbolKind.VARIABLE));
                });

                // 分析函数体所有语句
                for (StatementNode stmt : fn.body()) {
                    StatementAnalyzer<StatementNode> analyzer =
                            ctx.getRegistry().getStatementAnalyzer(stmt);
                    if (analyzer != null) {
                        // 传递语义分析器“实例”，避免类型擦除/反射调用
                        analyzer.analyze(ctx, mi, fn, locals, stmt);
                    } else {
                        // 语句类型未支持，收集错误
                        ctx.errors().add(new SemanticError(stmt, "不支持的语句类型: " + stmt));
                    }
                }

                // 非 void 函数，要求必须含至少一条 return 语句
                Type ret = ctx.parseType(fn.returnType());
                if (ret != null && ret != BuiltinType.VOID) {
                    boolean hasReturn = containsReturn(fn.body());
                    if (!hasReturn) {
                        ctx.errors().add(new SemanticError(fn, "非 void 函数必须包含至少一条 return 语句"));
                    }
                }
            }
        }
    }

    /**
     * 递归检查语句列表中是否存在 return 语句。
     */
    private boolean containsReturn(List<StatementNode> statements) {
        if (statements == null) {
            return false;
        }
        for (StatementNode statement : statements) {
            if (containsReturn(statement)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 递归检查单个语句（以及其嵌套语句）是否存在 return。
     */
    private boolean containsReturn(StatementNode statement) {
        if (statement == null) {
            return false;
        }
        if (statement instanceof ReturnNode) {
            return true;
        }
        if (statement instanceof IfNode ifNode) {
            return containsReturn(ifNode.thenBranch()) || containsReturn(ifNode.elseBranch());
        }
        if (statement instanceof LoopNode loopNode) {
            return containsReturn(loopNode.init())
                    || containsReturn(loopNode.step())
                    || containsReturn(loopNode.body());
        }
        return false;
    }
}
