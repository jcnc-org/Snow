package org.jcnc.snow.compiler.semantic.analyzers.expression;

import org.jcnc.snow.compiler.parser.ast.CallExpressionNode;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.IdentifierNode;
import org.jcnc.snow.compiler.parser.ast.MemberExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
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

import java.util.ArrayList;
import java.util.List;

/**
 * {@code CallExpressionAnalyzer} 是函数调用表达式 ({@link CallExpressionNode}) 的语义分析器。
 *
 * <p>它负责处理所有形式的调用表达式（如 {@code callee(arg1, arg2, ...)}），并执行如下操作：
 * <ul>
 *   <li>识别调用目标：支持三种调用方式
 *       <ol>
 *           <li>模块函数调用： {@code module.func(...)} </li>
 *           <li>结构体实例方法调用： {@code instance.method(...)} </li>
 *           <li>普通函数调用（当前模块或导入模块）： {@code func(...)} </li>
 *       </ol>
 *   </li>
 *   <li>在函数解析时遵循如下规则：
 *       <ul>
 *           <li>若是模块调用，必须确认模块已导入。</li>
 *           <li>若是结构体实例调用，需先解析左侧表达式类型并确认方法存在。</li>
 *           <li>若是普通函数调用，优先在当前模块中查找，若未找到，则尝试唯一导入模块解析。</li>
 *       </ul>
 *   </li>
 *   <li>参数检查与类型推断：
 *       <ul>
 *           <li>检查实参与形参数量是否一致。</li>
 *           <li>检查类型兼容性，支持数值宽化转换 (int → double)。</li>
 *           <li>支持数值到字符串的隐式转换（自动视为调用 {@code to_string}）。</li>
 *       </ul>
 *   </li>
 *   <li>错误处理：
 *       <ul>
 *           <li>函数/方法未定义时记录 {@link SemanticError}。</li>
 *           <li>访问未导入的模块时报错。</li>
 *           <li>跨模块访问私有函数（以 "_" 开头）时报错。</li>
 *           <li>参数数量或类型不匹配时报错。</li>
 *       </ul>
 *   </li>
 *   <li>最终返回函数的返回类型；若分析过程中存在错误，返回 {@link BuiltinType#INT} 作为默认回退类型。</li>
 * </ul>
 * <p>
 * 规则：
 * - 外部模块函数必须显式写 `模块.函数`，不再支持未限定名。
 * - 跨模块访问以下划线开头的私有函数禁止。
 */
public class CallExpressionAnalyzer implements ExpressionAnalyzer<CallExpressionNode> {

    /**
     * 分析函数调用表达式，推断返回类型并执行语义检查。
     *
     * @param ctx    当前语义分析上下文，提供日志、错误记录、模块访问等功能。
     * @param mi     当前模块信息，用于函数查找及模块依赖判断。
     * @param fn     当前正在分析的函数节点（函数作用域）。
     * @param locals 局部符号表，用于变量和结构体实例查找。
     * @param call   待分析的函数调用表达式节点。
     * @return 调用表达式的返回类型；若存在语义错误，返回 {@link BuiltinType#INT}。
     */
    @Override
    public Type analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        CallExpressionNode call) {
        ctx.log("检查函数/方法调用: " + call.callee());

        ExpressionNode callee = call.callee();   // 被调用的表达式（函数名或成员访问）
        ModuleInfo targetModule = mi;            // 初始假设目标模块为当前模块
        String functionName;                     // 被调用函数的名称
        FunctionType funcType;                   // 被调用函数的类型签名

        // ========== 支持三种调用形式 ==========
        // 1. 模块.函数(...)
        // 2. 结构体实例.方法(...)
        // 3. 普通函数(...)

        if (callee instanceof MemberExpressionNode memberExpr) {
            // -------- 情况1 & 情况2: 成员调用表达式 --------
            ExpressionNode left = memberExpr.object();
            String member = memberExpr.member();

            if (left instanceof IdentifierNode idNode) {
                // 左侧是标识符（可能是模块名或结构体变量）
                String leftName = idNode.name();

                // 1. 模块.函数
                if (ctx.getModules().containsKey(leftName) &&
                        (mi.getImports().contains(leftName) || mi.getName().equals(leftName))) {
                    targetModule = ctx.getModules().get(leftName);
                    functionName = member;
                    funcType = targetModule.getFunctions().get(functionName);
                } else {
                    // 2. 结构体实例.方法（重载）
                    Symbol sym = locals.resolve(leftName);
                    if (sym != null && sym.type() instanceof StructType structType) {
                        funcType = structType.getMethod(member, call.arguments().size());
                        functionName = member;
                        if (funcType == null) {
                            ctx.getErrors().add(new SemanticError(callee,
                                    "结构体方法未定义: " + structType + "." + member));
                            return BuiltinType.INT;
                        }
                    } else {
                        ctx.getErrors().add(new SemanticError(callee,
                                "未知或未导入模块: " + leftName));
                        return BuiltinType.INT;
                    }
                }
            } else {
                // 任意表达式.方法（重载）
                Type leftType = ctx.getRegistry()
                        .getExpressionAnalyzer(left)
                        .analyze(ctx, mi, fn, locals, left);

                if (leftType instanceof StructType structType) {
                    funcType = structType.getMethod(member, call.arguments().size());
                    functionName = member;
                    if (funcType == null) {
                        ctx.getErrors().add(new SemanticError(callee,
                                "结构体方法未定义: " + structType + "." + member));
                        return BuiltinType.INT;
                    }
                } else {
                    ctx.getErrors().add(new SemanticError(callee,
                            "不支持的成员调用对象类型: " + leftType));
                    return BuiltinType.INT;
                }
            }
        }
        // 普通函数调用
        else if (callee instanceof IdentifierNode idNode) {
            functionName = idNode.name();
            funcType = mi.getFunctions().get(functionName); // 仅在当前模块查找

            if (funcType == null) {
                // 如果导入模块里存在该函数，提示必须写 模块.函数
                List<String> candidates = new ArrayList<>();
                for (String imp : mi.getImports()) {
                    ModuleInfo mod = ctx.getModules().get(imp);
                    if (mod != null && mod.getFunctions().containsKey(functionName)) {
                        candidates.add(mod.getName());
                    }
                }
                if (!candidates.isEmpty()) {
                    ctx.getErrors().add(new SemanticError(idNode,
                            "外部模块函数调用必须写为 模块.函数，例如: "
                                    + candidates.get(0) + "." + functionName + "(...)"));
                    return BuiltinType.INT;
                }
            }
        }
        // 其它情况
        else {
            ctx.getErrors().add(new SemanticError(callee,
                    "不支持的调用方式: " + callee));
            return BuiltinType.INT;
        }

        // -------- 访问控制检查 --------
        if (functionName != null && funcType != null &&
                functionName.startsWith("_") &&
                !targetModule.getName().equals(mi.getName())) {
            ctx.getErrors().add(new SemanticError(callee,
                    "无法访问模块私有函数: " + targetModule.getName() + "." + functionName));
            return BuiltinType.INT;
        }

        // -------- 函数是否存在 --------
        if (funcType == null) {
            ctx.getErrors().add(new SemanticError(callee,
                    "函数未定义: " + functionName));
            return BuiltinType.INT;
        }

        // -------- 分析实参类型 --------
        List<Type> args = new ArrayList<>();
        for (ExpressionNode arg : call.arguments()) {
            args.add(ctx.getRegistry().getExpressionAnalyzer(arg)
                    .analyze(ctx, mi, fn, locals, arg));
        }

        if (args.size() != funcType.paramTypes().size()) {
            ctx.getErrors().add(new SemanticError(call,
                    "参数数量不匹配: 期望 " + funcType.paramTypes().size()
                            + ", 实际 " + args.size()));
        } else {
            for (int i = 0; i < args.size(); i++) {
                Type expected = funcType.paramTypes().get(i);
                Type actual = args.get(i);

                boolean ok = expected.isCompatible(actual)
                        || (expected.isNumeric() && actual.isNumeric()
                        && Type.widen(actual, expected) == expected);

                if (!ok && expected == BuiltinType.STRING && actual.isNumeric()) {
                    ok = true; // 自动数值→string
                }

                if (!ok) {
                    ctx.getErrors().add(new SemanticError(call,
                            "参数类型不匹配 (位置 " + i + "): 期望 " + expected + ", 实际 " + actual));
                }
            }
        }

        // -------- 返回类型 --------
        ctx.log("函数调用类型: 返回 " + funcType.returnType());
        return funcType.returnType();
    }
}
