package org.jcnc.snow.compiler.semantic.analyzers.expression;

import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.IdentifierNode;
import org.jcnc.snow.compiler.parser.ast.MemberExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.semantic.analyzers.base.ExpressionAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.Type;

/**
 * {@code MemberExpressionAnalyzer} – 成员访问表达式的语义分析器。
 *
 * <p>
 * 当前实现仅支持 <code>ModuleName.constOrVar</code> 形式的跨模块常量 /
 * 全局变量访问，不支持对象成员（如 a.b.c）。
 * </p>
 *
 * <ul>
 *   <li>示例：ModuleA.a 或 ModuleB.CONST_X</li>
 *   <li>仅检查模块是否存在且已导入，类型降级为 int，后续由 IR 阶段常量折叠</li>
 *   <li>暂不支持复杂的对象/类型成员访问</li>
 * </ul>
 */
public class MemberExpressionAnalyzer implements ExpressionAnalyzer<MemberExpressionNode> {

    /**
     * 对成员访问表达式执行语义分析。
     *
     * @param ctx     全局语义上下文，包含模块表、错误收集等
     * @param mi      当前模块信息（含本模块名、已导入模块）
     * @param fn      当前所在函数节点
     * @param locals  当前作用域符号表
     * @param expr    当前要分析的成员表达式（如 ModuleA.a）
     * @return 分析推断得到的类型（目前恒定降级为 int），如有错误也会记录
     */
    @Override
    public Type analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        MemberExpressionNode expr) {

        ctx.log("检查成员访问: " + expr);

        /* ---------- 仅支持 ModuleName.member 形式 ---------- */
        if (expr.object() instanceof IdentifierNode(String mod, NodeContext _)) {

            // 1. 检查模块是否存在、且已在当前模块 import 或为本模块
            if (!ctx.getModules().containsKey(mod)
                    || (!mi.getImports().contains(mod) && !mi.getName().equals(mod))) {
                ctx.getErrors().add(new SemanticError(expr,
                        "未知或未导入模块: " + mod));
                ctx.log("错误: 未导入模块 " + mod);
                return BuiltinType.INT;          // 语义降级：默认 int
            }

            // 2. 目前不做类型精确推断。后续可解析目标模块 globals 获取精确类型。
            //    这里只做语法级校验，类型降级为 INT，由后续阶段折叠。
            return BuiltinType.INT;
        }

        /* ---------- 其它对象成员（如 a.b.c）暂不支持 ---------- */
        ctx.getErrors().add(new SemanticError(expr,
                "不支持的成员访问对象类型: "
                        + expr.object().getClass().getSimpleName()));
        ctx.log("错误: 不支持的成员访问对象类型 "
                + expr.object().getClass().getSimpleName());
        return BuiltinType.INT;                  // 语义降级
    }
}
