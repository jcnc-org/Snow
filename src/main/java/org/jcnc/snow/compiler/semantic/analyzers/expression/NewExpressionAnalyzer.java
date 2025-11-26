package org.jcnc.snow.compiler.semantic.analyzers.expression;

import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.NewExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.semantic.analyzers.base.ExpressionAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.FunctionType;
import org.jcnc.snow.compiler.semantic.type.StructType;
import org.jcnc.snow.compiler.semantic.type.Type;
import org.jcnc.snow.compiler.semantic.utils.NumericConstantUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code NewExpressionAnalyzer}
 * <p>
 * 负责“对象创建表达式”<code>new T(args...)</code>的语义分析与类型推断。
 * <ul>
 *   <li>分析 new 关键字创建结构体对象的表达式。</li>
 *   <li>校验目标类型是否为结构体，参数数量与类型是否匹配构造函数。</li>
 *   <li>出错时会收集详细的语义错误信息。</li>
 * </ul>
 *
 * <p>
 * <b>表达式类型：</b> 返回被 new 的结构体类型 <code>T</code>，如失败则降级为 int。
 * </p>
 */
public class NewExpressionAnalyzer implements ExpressionAnalyzer<NewExpressionNode> {

    /**
     * 语义分析：推断 new 表达式的类型、参数检查、构造函数合法性校验。
     *
     * @param ctx    全局语义分析上下文
     * @param mi     当前模块信息
     * @param fn     当前所在函数节点
     * @param locals 局部符号表（当前作用域变量/形参）
     * @param expr   new 表达式节点
     * @return 分析推断得到的类型；如出错则降级为 int 类型
     */
    @Override
    public Type analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        NewExpressionNode expr) {

        final String typeName = expr.typeName();

        // 1. 解析目标类型（通过语义上下文统一入口，支持别名、跨模块等情况）
        Type parsed = ctx.parseType(typeName);
        if (parsed == null) {
            // 类型不存在，报错并降级
            ctx.errors().add(new SemanticError(expr, "未知类型: " + typeName));
            return BuiltinType.INT; // 兜底，避免连锁报错
        }
        if (!(parsed instanceof StructType st)) {
            // 非结构体类型不能用 new
            ctx.errors().add(new SemanticError(expr, "只有结构体类型才能使用 new: " + parsed));
            return BuiltinType.INT;
        }

        // 2. 分析所有实参的类型
        List<Type> argTypes = new ArrayList<>();
        for (ExpressionNode a : expr.arguments()) {
            Type at = ctx.getRegistry()
                    .getExpressionAnalyzer(a)
                    .analyze(ctx, mi, fn, locals, a);
            if (at == null) {
                // 兜底处理，防止后续 NPE
                at = BuiltinType.INT;
            }
            argTypes.add(at);
        }

        // 3. 选择并检查结构体构造函数（init）——支持重载：按“参数个数”匹配
        FunctionType ctor = st.getConstructor(argTypes.size());

        if (ctor == null) {
            // 若该结构体完全未声明任何 init，且调用为 0 实参，则允许隐式默认构造（保留旧行为）
            if (st.getConstructors().isEmpty() && argTypes.isEmpty()) {
                return st;
            }
            // 否则报错：未找到对应形参个数的构造函数
            ctx.errors().add(new SemanticError(expr,
                    "未找到参数个数为 " + argTypes.size() + " 的构造函数"));
            return st;
        }

        // 3.1. 构造函数参数类型兼容性检查（包括数值类型宽化）
        List<Type> expectedParams = ctor.paramTypes();
        for (int i = 0; i < expectedParams.size(); i++) {
            Type expected = expectedParams.get(i); // 构造函数声明的参数类型
            Type actual = argTypes.get(i);       // 实际传入的实参类型

            boolean compatible = expected.isCompatible(actual); // 直接类型兼容
            boolean widenOK = expected.isNumeric()
                    && actual.isNumeric()
                    && expected.equals(Type.widen(actual, expected)); // 支持数值类型自动宽化
            boolean narrowingConst = NumericConstantUtils.canNarrowToIntegral(expected, actual, expr.arguments().get(i));

            if (!compatible && !widenOK && !narrowingConst) {
                // 实参类型不兼容也无法宽化，报错
                ctx.errors().add(new SemanticError(expr,
                        String.format("构造函数参数类型不匹配 (位置 %d): 期望 %s, 实际 %s",
                                i, expected, actual)));
            }
        }

        // 4. new 表达式的类型就是结构体类型
        return st;
    }
}
