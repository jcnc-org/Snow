package org.jcnc.snow.compiler.semantic.analyzers.statement;

import org.jcnc.snow.compiler.parser.ast.DeclarationNode;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.semantic.analyzers.base.StatementAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.symbol.*;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.Type;

/**
 * {@code DeclarationAnalyzer} 是变量声明语句的语义分析器。
 * <p>
 * 它负责处理类似 {@code int x = 10;} 的声明语句，具体分析内容包括: 
 * <ul>
 *   <li>类型解析: 将声明中的类型字符串转换为语义层的 {@link Type} 对象；</li>
 *   <li>符号定义: 将变量注册到当前作用域的 {@link SymbolTable} 中；</li>
 *   <li>重复定义检查: 防止同一作用域下的变量名冲突；</li>
 *   <li>初始化表达式类型校验: 检查类型兼容性，支持数值类型宽化（如 int → float）。</li>
 * </ul>
 * 若出现类型未识别、重复声明或类型不兼容等问题，将向语义错误列表添加对应错误信息。
 */
public class DeclarationAnalyzer implements StatementAnalyzer<DeclarationNode> {

    /**
     * 对变量声明语句执行语义分析。
     *
     * @param ctx    当前语义分析上下文对象，提供类型解析、错误记录、日志输出等功能。
     * @param mi     当前模块信息，支持跨模块引用检查（本分析器未直接使用）。
     * @param fn     当前函数节点，表示当前所在作用域的函数（用于初始化分析上下文）。
     * @param locals 当前作用域的符号表，用于注册和查找变量。
     * @param decl   要分析的变量声明节点 {@link DeclarationNode}。
     */
    @Override
    public void analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        DeclarationNode decl) {

        // 1. 解析声明类型
        Type varType = ctx.parseType(decl.getType());
        if (varType == null) {
            ctx.getErrors().add(new SemanticError(decl,
                    "未知类型: " + decl.getType()));
            ctx.log("错误: 未知类型 " + decl.getType()
                    + " 在声明 " + decl.getName());
            varType = BuiltinType.INT;  // 容错处理: 默认降级为 int
        }
        ctx.log("声明变量: " + decl.getName()
                + " 类型: " + varType);

        // 2. 将变量注册到当前作用域符号表中，检查重复定义
        if (!locals.define(new Symbol(
                decl.getName(), varType, SymbolKind.VARIABLE
        ))) {
            ctx.getErrors().add(new SemanticError(decl,
                    "变量重复声明: " + decl.getName()));
            ctx.log("错误: 变量重复声明 " + decl.getName());
        }

        // 3. 检查初始化表达式（如果存在）
        Type finalVarType = varType;  // 用于 lambda 捕获
        decl.getInitializer().ifPresent(init -> {
            Type initType = ctx.getRegistry().getExpressionAnalyzer(init)
                    .analyze(ctx, mi, fn, locals, init);

            // 检查类型是否兼容，或是否允许数值宽化转换
            if (!finalVarType.isCompatible(initType)) {
                boolean canWiden = finalVarType.isNumeric()
                        && initType.isNumeric()
                        && Type.widen(initType, finalVarType) == finalVarType;
                if (!canWiden) {
                    ctx.getErrors().add(new SemanticError(decl,
                            "初始化类型不匹配: 期望 " + finalVarType
                                    + ", 实际 " + initType));
                    ctx.log("错误: 初始化类型不匹配 "
                            + decl.getName());
                }
            }
        });
    }
}
