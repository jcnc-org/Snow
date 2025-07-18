package org.jcnc.snow.compiler.semantic.analyzers.expression;

import org.jcnc.snow.compiler.parser.ast.*;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.semantic.analyzers.base.ExpressionAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.FunctionType;
import org.jcnc.snow.compiler.semantic.type.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code CallExpressionAnalyzer} 是函数调用表达式的语义分析器。
 * <p>
 * 它负责处理类似 {@code callee(arg1, arg2, ...)} 形式的调用表达式，执行如下操作:
 * <ul>
 *   <li>识别调用目标（支持模块成员函数调用和当前模块函数调用）；</li>
 *   <li>根据被调用函数的参数签名检查实参数量和类型的兼容性；</li>
 *   <li>支持数值参数的宽化转换（如 int → double）；</li>
 *   <li>支持数值到字符串的隐式转换（自动视为调用 {@code to_string}）；</li>
 *   <li>在发生类型不匹配、未导入模块或函数未定义等情况下记录语义错误。</li>
 * </ul>
 */
public class CallExpressionAnalyzer implements ExpressionAnalyzer<CallExpressionNode> {

    /**
     * 分析函数调用表达式并推断其类型。
     *
     * @param ctx    当前语义分析上下文，提供日志、错误记录、模块访问等功能。
     * @param mi     当前模块信息，用于函数查找及模块依赖判断。
     * @param fn     当前分析的函数节点。
     * @param locals 局部符号表，用于变量查找。
     * @param call   待分析的函数调用表达式节点。
     * @return 表达式的返回类型。如果存在语义错误，默认返回 {@code BuiltinType.INT}。
     */
    @Override
    public Type analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        CallExpressionNode call) {
        ctx.log("检查函数调用: " + call.callee());

        ModuleInfo target = mi;  // 默认目标模块为当前模块
        String functionName;
        ExpressionNode callee = call.callee();

        // 支持模块调用形式: ModuleName.FunctionName(...)
        if (callee instanceof MemberExpressionNode(var obj, String member, NodeContext _)
                && obj instanceof IdentifierNode(String mod, NodeContext _)) {
            // 验证模块是否存在并已导入
            if (!ctx.getModules().containsKey(mod)
                    || (!mi.getImports().contains(mod) && !mi.getName().equals(mod))) {
                ctx.getErrors().add(new SemanticError(callee,
                        "未知或未导入模块: " + mod));
                ctx.log("错误: 未导入模块 " + mod);
                return BuiltinType.INT;
            }
            target = ctx.getModules().get(mod);
            functionName = member;

            // 简单函数名形式: func(...)
        } else if (callee instanceof IdentifierNode(String name, NodeContext _)) {
            functionName = name;

            // 不支持的 callee 形式
        } else {
            ctx.getErrors().add(new SemanticError(callee,
                    "不支持的调用方式: " + callee));
            ctx.log("错误: 不支持的调用方式 " + callee);
            return BuiltinType.INT;
        }

        // 查找目标函数签名
        FunctionType ft = target.getFunctions().get(functionName);
        if (ft == null) {
            ctx.getErrors().add(new SemanticError(callee,
                    "函数未定义: " + functionName));
            ctx.log("错误: 函数未定义 " + functionName);
            return BuiltinType.INT;
        }

        // 分析所有实参并获取类型
        List<Type> args = new ArrayList<>();
        for (ExpressionNode arg : call.arguments()) {
            args.add(ctx.getRegistry().getExpressionAnalyzer(arg)
                    .analyze(ctx, mi, fn, locals, arg));
        }

        // 参数数量检查
        if (args.size() != ft.paramTypes().size()) {
            ctx.getErrors().add(new SemanticError(call,
                    "参数数量不匹配: 期望 " + ft.paramTypes().size()
                            + " 个, 实际 " + args.size() + " 个"));
            ctx.log("错误: 参数数量不匹配: 期望 "
                    + ft.paramTypes().size() + ", 实际 " + args.size());

        } else {
            // 参数类型检查与转换支持
            for (int i = 0; i < args.size(); i++) {
                Type expected = ft.paramTypes().get(i);
                Type actual   = args.get(i);

                // 完全兼容或数值宽化转换
                boolean ok = expected.isCompatible(actual)
                        || (expected.isNumeric() && actual.isNumeric()
                        && Type.widen(actual, expected) == expected);

                // 支持将数值自动转换为字符串
                if (!ok && expected == BuiltinType.STRING && actual.isNumeric()) {
                    ctx.log(String.format(
                            "隐式将参数 %d 的数值类型 %s 转换为 string (to_string)",
                            i, actual
                    ));
                    ok = true;
                }

                // 类型不匹配，记录语义错误
                if (!ok) {
                    ctx.getErrors().add(new SemanticError(call,
                            String.format("参数类型不匹配 (位置 %d): 期望 %s, 实际 %s",
                                    i, expected, actual)));
                    ctx.log("错误: 参数类型不匹配 (位置 " + i + "): 期望 "
                            + expected + ", 实际 " + actual);
                }
            }
        }

        // 返回函数的返回类型作为整个调用表达式的类型
        ctx.log("函数调用类型: 返回 " + ft.returnType());
        return ft.returnType();
    }
}
