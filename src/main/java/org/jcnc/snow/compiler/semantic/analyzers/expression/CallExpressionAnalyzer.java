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
import org.jcnc.snow.compiler.semantic.type.*;
import org.jcnc.snow.compiler.semantic.utils.NumericConstantUtils;

import java.util.ArrayList;
import java.util.Arrays;
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
 *   </ul>
 *   <li>权限和语义错误处理：</li>
 *   <ul>
 *     <li>禁止跨模块访问以 <code>"_"</code> 开头的私有函数</li>
 *     <li>记录未导入模块、未定义函数、参数不匹配等所有类型语义错误</li>
 *   </ul>
 *   <li>最终根据函数签名推断并返回返回值类型。若存在语义错误，返回 {@link BuiltinType#INT} 作为回退类型。</li>
 * </ul>
 * 支持内置 syscall 特例：裸 syscall 调用和参数类型宽松校验。
 * <p>
 * 主要职责：
 * <ul>
 *   <li>查找目标函数定义（模块/结构体/本地）</li>
 *   <li>参数数量和类型检查，支持自动宽化与 syscall 特例</li>
 *   <li>错误收集和健壮返回类型，确保分析不中断</li>
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
            ctx.getErrors().add(new SemanticError(node, "不是调用表达式: " + node));
            return BuiltinType.INT;
        }

        ExpressionNode callee = call.callee();
        List<Type> args = new ArrayList<>();
        ModuleInfo targetModule;        // 默认目标模块为当前模块
        String functionName;            // 被调用函数名称
        FunctionType funcType;          // 被调用函数类型


        // 1. 解析调用目标 
        if (callee instanceof MemberExpressionNode member) {
            // 形如 module.func(...) 或 obj.method(...)
            ExpressionNode left = member.object();
            String memberName = member.member();

            if (left instanceof IdentifierNode idNode) {
                // 1.1 左侧是标识符（可能是模块名）
                String leftName = idNode.name();

                if (ctx.getModules().containsKey(leftName) &&
                        (mi.getImports().contains(leftName) || mi.getName().equals(leftName))) {
                    // 模块调用
                    targetModule = ctx.getModules().get(leftName);
                    functionName = memberName;
                    funcType = targetModule.getFunctions().get(functionName);

                    if (funcType == null && functionName.startsWith("_")) {
                        ctx.getErrors().add(new SemanticError(member,
                                "禁止访问私有函数: " + leftName + "." + functionName));
                        return BuiltinType.INT;
                    }
                    if (funcType == null) {
                        ctx.getErrors().add(new SemanticError(member,
                                "模块函数未定义: " + leftName + "." + functionName));
                        return BuiltinType.INT;
                    }
                } else {
                    // 1.2 左侧不是模块，尝试解析为结构体方法调用
                    Symbol sym = locals.resolve(leftName);
                    if (sym != null && sym.type() instanceof StructType structType) {
                        functionName = memberName;
                        funcType = structType.getMethod(functionName, call.arguments().size());
                        if (funcType == null) {
                            ctx.getErrors().add(new SemanticError(callee,
                                    "结构体方法未定义: " + structType + "." + functionName));
                            return BuiltinType.INT;
                        }
                    } else {
                        ctx.getErrors().add(new SemanticError(callee,
                                "未知或未导入模块: " + leftName));
                        return BuiltinType.INT;
                    }
                }
            } else {
                // 1.3 左侧是任意表达式（如链式调用）
                Type leftType = ctx.getRegistry()
                        .getExpressionAnalyzer(left)
                        .analyze(ctx, mi, fn, locals, left);
                if (!(leftType instanceof StructType structType)) {
                    ctx.getErrors().add(new SemanticError(left,
                            "点号左侧必须是结构体实例: " + left));
                    return BuiltinType.INT;
                }
                functionName = memberName;
                funcType = structType.getMethod(functionName, call.arguments().size());
                if (funcType == null) {
                    ctx.getErrors().add(new SemanticError(member,
                            "结构体方法未定义: " + structType + "." + functionName));
                    return BuiltinType.INT;
                }
            }
        } else if (callee instanceof IdentifierNode idNode) {
            // 2. 普通函数调用 
            functionName = idNode.name();
            funcType = mi.getFunctions().get(functionName);

            // 内置特例：裸 syscall
            if (funcType == null && "syscall".equals(functionName)) {
                funcType = new FunctionType(
                        Arrays.asList(BuiltinType.STRING, new ArrayType(BuiltinType.ANY)),
                        BuiltinType.ANY
                );
            }

            // 如果找不到，检查导入模块里是否存在同名函数
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
        } else {
            // 3. 其它调用方式不支持 
            ctx.getErrors().add(new SemanticError(callee,
                    "不支持的调用方式: " + callee));
            return BuiltinType.INT;
        }

        // 若仍未找到函数定义
        if (funcType == null) {
            ctx.getErrors().add(new SemanticError(callee,
                    "函数未定义: " + (functionName == null ? "<unknown>" : functionName)));
            return BuiltinType.INT;
        }

        // 4. 收集实参类型 
        for (ExpressionNode arg : call.arguments()) {
            args.add(ctx.getRegistry().getExpressionAnalyzer(arg)
                    .analyze(ctx, mi, fn, locals, arg));
        }

        // 5. 参数检查 
        boolean isSyscallVarargs =
                "syscall".equals(functionName);

        if (isSyscallVarargs) {
            // syscall 特例：允许任意参数数量，但至少 1 个
            if (args.isEmpty()) {
                ctx.getErrors().add(new SemanticError(call, "syscall 至少需要一个子命令字符串参数"));
            } else {
                // 检查第一个参数类型必须是 string/可转 string
                Type expected0 = funcType.paramTypes().getFirst();
                Type actual0 = args.getFirst();
                boolean ok0 = expected0.isCompatible(actual0)
                        || (expected0.isNumeric() && actual0.isNumeric()
                        && Type.widen(actual0, expected0) == expected0)
                        || NumericConstantUtils.canNarrowToIntegral(expected0, actual0, call.arguments().getFirst());
                if (!ok0 && expected0 == BuiltinType.STRING && actual0.isNumeric()) {
                    ok0 = true; // 支持数字自动转字符串
                }
                if (!ok0) {
                    ctx.getErrors().add(new SemanticError(call,
                            "参数类型不匹配 (位置 0): 期望 " + expected0 + ", 实际 " + actual0));
                }
                // 其余参数不做严格检查
            }
        } else {
            // 普通函数严格检查参数数量与类型
            if (args.size() != funcType.paramTypes().size()) {
                ctx.getErrors().add(new SemanticError(call,
                        "参数数量不匹配: 期望 " + funcType.paramTypes().size()
                                + " 个，实际 " + args.size() + " 个"));
                return funcType.returnType();
            }
            for (int i = 0; i < args.size(); i++) {
                Type expected = funcType.paramTypes().get(i);
                Type actual = args.get(i);

                boolean ok = expected.isCompatible(actual)
                        || (expected.isNumeric() && actual.isNumeric()
                        && Type.widen(actual, expected) == expected)
                        || NumericConstantUtils.canNarrowToIntegral(expected, actual, call.arguments().get(i))
                        || (expected == BuiltinType.STRING && actual.isNumeric());

                if (!ok) {
                    ctx.getErrors().add(new SemanticError(call,
                            "参数类型不匹配 (位置 " + i + "): 期望 " + expected + ", 实际 " + actual));
                }
            }
        }

        // 6. 返回函数的返回类型
        ctx.log("函数调用类型: 返回 " + funcType.returnType());
        return funcType.returnType();
    }
}
