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
import org.jcnc.snow.compiler.semantic.type.Type;

/**
 * {@code MemberExpressionAnalyzer} 用于分析模块成员访问表达式的类型和语义。
 *
 * <p>
 * 当前实现支持 <code>ModuleName.constOrVar</code> 形式的跨模块常量/全局变量访问，
 * 能根据目标模块的全局符号表，返回准确的类型信息，完全支持跨模块类型推断。
 * <br>
 * 对于非模块成员的访问（如对象.属性、多级 a.b.c），暂不支持，遇到时将报告语义错误。
 * </p>
 *
 * <p>
 * <b>核心特性：</b>
 * <ul>
 *   <li>校验模块是否存在、是否已导入（或自身）；</li>
 *   <li>跨模块访问目标模块的全局符号表，查找指定成员符号及其类型；</li>
 *   <li>若成员不存在，报告“模块成员未定义”语义错误；</li>
 *   <li>暂不支持更复杂的对象成员访问，遇到将报“不支持的成员访问对象类型”错误。</li>
 * </ul>
 * </p>
 */
public class MemberExpressionAnalyzer implements ExpressionAnalyzer<MemberExpressionNode> {

    /**
     * 语义分析模块成员访问表达式。
     *
     * @param ctx    全局语义分析上下文，持有所有模块及错误记录
     * @param mi     当前模块信息（用于判断导入关系）
     * @param fn     当前函数节点
     * @param locals 当前局部符号表
     * @param expr   当前要分析的成员表达式（如 ModuleA.a）
     * @return 成员表达式的类型；出错时类型降级为 int，并记录语义错误
     */
    @Override
    public Type analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        MemberExpressionNode expr) {

        ctx.log("检查成员访问: " + expr);

        // -------- 仅支持 ModuleName.member 形式 --------
        if (expr.object() instanceof IdentifierNode(String mod, NodeContext _)) {

            // 1. 校验模块存在且已导入或为本模块自身
            if (!ctx.getModules().containsKey(mod)
                    || (!mi.getImports().contains(mod) && !mi.getName().equals(mod))) {
                ctx.getErrors().add(new SemanticError(expr,
                        "未知或未导入模块: " + mod));
                ctx.log("错误: 未导入模块 " + mod);
                return BuiltinType.INT;
            }

            // 2. 查找目标模块的全局符号表，精确返回成员类型
            ModuleInfo target = ctx.getModules().get(mod);
            SymbolTable globals = target.getGlobals();
            if (globals != null) {
                Symbol sym = globals.resolve(expr.member());
                if (sym != null) {
                    return sym.type(); // 找到成员，返回其真实类型
                }
            }

            // 3. 成员不存在，记录语义错误并类型降级
            ctx.getErrors().add(new SemanticError(expr,
                    "模块成员未定义: " + mod + "." + expr.member()));
            ctx.log("错误: 模块成员未定义 " + mod + "." + expr.member());
            return BuiltinType.INT;
        }

        // -------- 其它对象成员（如 a.b.c）暂不支持 --------
        ctx.getErrors().add(new SemanticError(expr,
                "不支持的成员访问对象类型: "
                        + expr.object().getClass().getSimpleName()));
        ctx.log("错误: 不支持的成员访问对象类型 "
                + expr.object().getClass().getSimpleName());
        return BuiltinType.INT;
    }
}
