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
 * 负责分析和类型推断所有函数调用表达式，包括模块函数调用、结构体方法调用以及普通函数调用。
 * <p>
 * 主要职责如下：
 * <ul>
 *   <li>解析调用目标，支持三种调用形式：</li>
 *   <ol>
 *     <li>模块函数调用（如 <code>module.func(...)</code>）</li>
 *     <li>结构体实例方法调用（如 <code>instance.method(...)</code>）</li>
 *     <li>普通函数调用（如 <code>func(...)</code>，仅限当前模块）</li>
 *   </ol>
 *   <li>依据调用目标类型，执行相应的符号查找和类型匹配：</li>
 *   <ul>
 *     <li>模块调用需确认模块已导入且函数存在</li>
 *     <li>结构体方法调用需先解析实例类型并查找方法</li>
 *     <li>普通函数调用只在当前模块内查找，若导入模块中存在同名函数，提示需显式指定模块名</li>
 *   </ul>
 *   <li>参数检查和类型推断，包括：</li>
 *   <ul>
 *     <li>参数数量校验</li>
 *     <li>参数类型兼容性（支持数值类型宽化和数值到字符串的隐式转换）</li>
 *     <li>特例支持 os.syscall 的可变参数规则</li>
 *   </ul>
 *   <li>权限和语义错误处理：</li>
 *   <ul>
 *     <li>禁止跨模块访问以 <code>"_"</code> 开头的私有函数</li>
 *     <li>记录未导入模块、未定义函数、参数不匹配等所有类型语义错误</li>
 *   </ul>
 *   <li>最终根据函数签名推断并返回返回值类型。若存在语义错误，返回 {@link BuiltinType#INT} 作为回退类型。</li>
 * </ul>
 * <p>
 * 设计约束：
 * <ul>
 *   <li>外部模块函数必须采用“模块.函数”全限定名调用</li>
 *   <li>禁止跨模块访问私有（下划线开头）函数</li>
 * </ul>
 */
public class CallExpressionAnalyzer implements ExpressionAnalyzer {

    /**
     * 分析函数调用表达式节点，推断返回类型并执行完整的语义检查。
     *
     * @param ctx    当前语义分析上下文
     * @param mi     当前模块信息
     * @param fn     当前作用域函数节点
     * @param locals 当前作用域局部符号表
     * @param node   待分析的函数调用表达式节点
     * @return 返回调用表达式的返回类型；如存在语义错误，返回 {@link BuiltinType#INT} 作为回退类型
     */
    @Override
    public Type analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        ExpressionNode node) {
        // 检查节点类型是否合法
        if (!(node instanceof CallExpressionNode call)) {
            ctx.getErrors().add(new SemanticError(node, "内部错误：非调用表达式"));
            return BuiltinType.INT;
        }

        ExpressionNode callee = call.callee();

        ModuleInfo targetModule = mi;   // 默认目标模块为当前模块
        String functionName;            // 被调用函数名称
        FunctionType funcType;          // 被调用函数类型

        // 1. 处理成员调用（模块.函数 或 结构体实例.方法 或 任意表达式.方法）
        if (callee instanceof MemberExpressionNode memberExpr) {
            ExpressionNode left = memberExpr.object();
            String member = memberExpr.member();

            if (left instanceof IdentifierNode idNode) {
                String leftName = idNode.name();

                // 1.1 作为模块调用，需确认模块已导入且存在目标函数
                if (ctx.getModules().containsKey(leftName) &&
                        (mi.getImports().contains(leftName) || mi.getName().equals(leftName))) {
                    targetModule = ctx.getModules().get(leftName);
                    functionName = member;
                    funcType = targetModule.getFunctions().get(functionName);
                } else {
                    // 1.2 作为结构体实例调用，解析实例类型并查找方法
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
                // 1.3 任意表达式.方法，如链式结构体方法调用
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
                            "无法在该对象上调用方法: " + leftType));
                    return BuiltinType.INT;
                }
            }
        }
        // 2. 普通函数调用，仅在当前模块查找
        else if (callee instanceof IdentifierNode idNode) {
            functionName = idNode.name();
            funcType = mi.getFunctions().get(functionName);

            // 如未找到，检查导入模块是否存在同名函数，提示用户必须写模块名
            if (funcType == null) {
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
                                    + candidates.getFirst() + "." + functionName + "(...)"));
                    return BuiltinType.INT;
                }
            }
        }
        // 3. 其它不支持的调用方式，直接报错
        else {
            ctx.getErrors().add(new SemanticError(callee,
                    "不支持的调用方式: " + callee));
            return BuiltinType.INT;
        }

        // 校验访问权限：禁止跨模块访问以“_”开头的私有函数
        if (targetModule != null &&
                functionName != null &&
                functionName.startsWith("_") &&
                !targetModule.getName().equals(mi.getName())) {
            ctx.getErrors().add(new SemanticError(callee,
                    "无法访问模块私有函数: " + targetModule.getName() + "." + functionName));
            return BuiltinType.INT;
        }

        // 校验函数是否存在
        if (funcType == null) {
            ctx.getErrors().add(new SemanticError(callee,
                    "函数未定义: " + functionName));
            return BuiltinType.INT;
        }

        // 分析所有实参类型
        List<Type> args = new ArrayList<>();
        for (ExpressionNode arg : call.arguments()) {
            args.add(ctx.getRegistry().getExpressionAnalyzer(arg)
                    .analyze(ctx, mi, fn, locals, arg));
        }

        // 特例支持：os.syscall 允许可变参数，首参要求为 string
        boolean isSyscallVarargs = (targetModule != null
                && "os".equals(targetModule.getName())
                && "syscall".equals(functionName));

        if (isSyscallVarargs) {
            if (args.isEmpty()) {
                ctx.getErrors().add(new SemanticError(call, "syscall 至少需要一个子命令字符串参数"));
            } else {
                // 检查第一个参数类型
                Type expected0 = funcType.paramTypes().getFirst();
                Type actual0 = args.getFirst();
                boolean ok0 = expected0.isCompatible(actual0)
                        || (expected0.isNumeric() && actual0.isNumeric()
                        && Type.widen(actual0, expected0) == expected0);
                if (!ok0 && expected0 == BuiltinType.STRING && actual0.isNumeric()) {
                    ok0 = true; // 支持数值到字符串自动转换
                }
                if (!ok0) {
                    ctx.getErrors().add(new SemanticError(call,
                            "参数类型不匹配 (位置 0): 期望 " + expected0 + ", 实际 " + actual0));
                }
                // 其余参数无需进一步检查类型
            }
        } else {
            // 普通函数参数数量与类型校验
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
                        ok = true; // 支持数值到字符串自动转换
                    }

                    if (!ok) {
                        ctx.getErrors().add(new SemanticError(call,
                                "参数类型不匹配 (位置 " + i + "): 期望 " + expected + ", 实际 " + actual));
                    }
                }
            }
        }

        // 返回最终的函数返回类型，若有错误已提前返回
        ctx.log("函数调用类型: 返回 " + funcType.returnType());
        return funcType.returnType();
    }
}
