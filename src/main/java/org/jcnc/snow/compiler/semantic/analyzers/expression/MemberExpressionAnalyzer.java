package org.jcnc.snow.compiler.semantic.analyzers.expression;

import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.IdentifierNode;
import org.jcnc.snow.compiler.parser.ast.MemberExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.semantic.analyzers.base.ExpressionAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.symbol.Symbol;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.FunctionType;
import org.jcnc.snow.compiler.semantic.type.StructType;
import org.jcnc.snow.compiler.semantic.type.Type;

/**
 * {@code MemberExpressionAnalyzer}
 *
 * <p>
 * 负责成员访问表达式的语义分析与类型推断。
 * 典型形如 <code>a.b</code>、<code>this.x</code>、<code>Module.member</code>。
 * </p>
 *
 * <p>
 * 支持三类成员访问方式：
 * </p>
 * <ol>
 *   <li><b>当前实例字段语法糖</b>：如 <code>this.x</code>，等价于访问当前作用域下名为 <code>x</code> 的变量或字段。</li>
 *   <li><b>跨模块成员访问</b>：如 <code>ModuleName.member</code>，常用于引用其他模块的全局变量、常量、函数等。</li>
 *   <li><b>结构体实例成员/方法访问</b>：如 <code>a.b</code> 或 <code>a.method</code>，a 为结构体变量，b 为字段或方法。</li>
 * </ol>
 */
public class MemberExpressionAnalyzer implements ExpressionAnalyzer<MemberExpressionNode> {

    /**
     * 分析成员表达式，并返回其类型（如字段类型、方法类型等）。
     *
     * @param ctx    全局语义分析上下文
     * @param mi     当前模块信息
     * @param fn     当前函数节点
     * @param locals 局部符号表（当前作用域的变量、形参等）
     * @param expr   当前成员访问表达式（形如 obj.member）
     * @return 该表达式的推断类型，如失败则返回 {@link BuiltinType#INT}
     *
     * <p>
     * 主要处理流程分为四大分支：
     * <ol>
     *   <li>this.x 语法糖（字段或变量访问）</li>
     *   <li>ModuleName.member 跨模块成员访问</li>
     *   <li>结构体实例的成员/方法访问</li>
     *   <li>其它对象成员（不支持）</li>
     * </ol>
     * 出错时会注册语义错误，返回 int 作为降级类型。
     * </p>
     */
    @Override
    public Type analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        MemberExpressionNode expr) {

        ctx.log("检查成员访问: " + expr);

        // =====================================================================
        // 1) 处理 this.x 语法糖
        // =====================================================================
        // 匹配左侧为 IdentifierNode，且为 "this"
        if (expr.object() instanceof IdentifierNode oid) {
            String objName = oid.name();

            if ("this".equals(objName)) {
                // 优先在当前局部符号表查找字段或变量（如结构体字段/参数/局部变量等）
                if (locals != null) {
                    Symbol localSym = locals.resolve(expr.member());
                    if (localSym != null) {
                        return localSym.type();
                    }
                }
                // 再查全局符号表（当前模块全局声明）
                SymbolTable globals = mi != null ? mi.getGlobals() : null;
                if (globals != null) {
                    Symbol globalSym = globals.resolve(expr.member());
                    if (globalSym != null) {
                        return globalSym.type();
                    }
                }
                // 两处都查不到则报错
                ctx.getErrors().add(new SemanticError(expr,
                        "未定义的字段或变量: " + expr.member()));
                ctx.log("错误: 未定义的字段或变量 this." + expr.member());
                // 降级为 int 类型
                return BuiltinType.INT;
            }
        }

        // =====================================================================
        // 2) 处理 ModuleName.member 跨模块访问
        // =====================================================================
        // 左侧为 IdentifierNode，尝试解析为模块名
        if (expr.object() instanceof IdentifierNode(String mod, NodeContext _)) {
            // 是否为已知模块
            boolean moduleExists = ctx.getModules().containsKey(mod);
            // 是否导入了该模块或就是本模块（允许自引用）
            boolean importedOrSelf = mi != null && (mi.getName().equals(mod) || mi.getImports().contains(mod));
            if (moduleExists && importedOrSelf) {
                ModuleInfo target = ctx.getModules().get(mod);
                // 1) 先查模块内函数（支持模块.函数名引用）
                FunctionType ft = target.getFunctions().get(expr.member());
                if (ft != null) return ft;

                // 2) 再查模块全局符号表（全局常量/变量）
                SymbolTable globals = target != null ? target.getGlobals() : null;
                if (globals != null) {
                    Symbol sym = globals.resolve(expr.member());
                    if (sym != null) {
                        return sym.type();
                    }
                }
                // 以上均未找到则报错
                ctx.getErrors().add(new SemanticError(expr,
                        "模块成员未定义: " + mod + "." + expr.member()));
                ctx.log("错误: 模块成员未定义 " + mod + "." + expr.member());
                return BuiltinType.INT;
            }
            // 左侧不是模块名，可能是变量名（结构体实例），进入结构体成员分支
            if (locals != null) {
                Symbol sym = locals.resolve(mod);
                if (sym != null && sym.type() instanceof StructType st) {
                    // 结构体字段访问
                    Type t = st.getFields().get(expr.member());
                    if (t != null) return t;
                    // 结构体方法访问（返回方法类型，用于引用/分派）
                    FunctionType ft = st.getMethods().get(expr.member());
                    if (ft != null) return ft;
                    // 字段和方法都没有，报错
                    ctx.getErrors().add(new SemanticError(expr,
                            "结构体成员未定义: " + st + "." + expr.member()));
                    ctx.log("错误: 结构体成员未定义 " + st + "." + expr.member());
                    return BuiltinType.INT;
                }
            }
            // 非模块非变量均未找到，报错
            ctx.getErrors().add(new SemanticError(expr, "未知或未导入模块: " + mod));
            ctx.log("错误: 未导入模块或未声明变量 " + mod);
            return BuiltinType.INT;
        }

        // =====================================================================
        // 3) 支持通用结构体成员访问：a.b 或任意表达式.b
        // =====================================================================
        // 动态推断左侧表达式类型（如结构体类型等）
        Type leftType = ctx.getRegistry().getExpressionAnalyzer(expr.object())
                .analyze(ctx, mi, fn, locals, expr.object());

        if (leftType instanceof StructType st) {
            // 字段访问
            Type t = st.getFields().get(expr.member());
            if (t != null) return t;
            // 方法访问（返回方法类型，支持引用/分派）
            FunctionType ft = st.getMethods().get(expr.member());
            if (ft != null) return ft;
            // 均未找到，报错
            ctx.getErrors().add(new SemanticError(expr,
                    "结构体成员未定义: " + st + "." + expr.member()));
            ctx.log("错误: 结构体成员未定义 " + st + "." + expr.member());
            return BuiltinType.INT;
        }

        // =====================================================================
        // 4) 其它对象成员（如 xx.yy 且 xx 非结构体）暂不支持
        // =====================================================================
        ctx.getErrors().add(new SemanticError(expr,
                "不支持的成员访问对象类型: " + expr.object().getClass().getSimpleName()));
        ctx.log("错误: 不支持的成员访问对象类型 " + expr.object().getClass().getSimpleName());
        return BuiltinType.INT;
    }
}
