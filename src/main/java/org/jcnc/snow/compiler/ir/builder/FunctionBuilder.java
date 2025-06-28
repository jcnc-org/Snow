package org.jcnc.snow.compiler.ir.builder;

import org.jcnc.snow.compiler.ir.core.IRFunction;
import org.jcnc.snow.compiler.ir.utils.ExpressionUtils;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.ParameterNode;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

/**
 * IR 函数构建器。
 * <p>
 * 负责将语法树中的 FunctionNode 节点转化为可执行的 IRFunction，
 * 包含参数声明、返回类型推断、函数体语句转换等步骤。
 */
public class FunctionBuilder {

    /**
     * 将 AST 中的 FunctionNode 构建为可执行的 IRFunction。
     * <p>
     * 构建过程包括：
     * <ol>
     *     <li>初始化 IRFunction 实例和上下文</li>
     *     <li>根据函数返回类型，设置默认类型后缀，便于表达式推断</li>
     *     <li>声明参数到作用域，并为每个参数分配虚拟寄存器</li>
     *     <li>遍历并转换函数体内的每条语句为 IR 指令</li>
     *     <li>函数构建完成后，清理默认类型后缀，防止影响其他函数</li>
     * </ol>
     *
     * @param functionNode 表示函数定义的语法树节点
     * @return 构建得到的 IRFunction 对象
     */
    public IRFunction build(FunctionNode functionNode) {

        // 0) 基本初始化：创建 IRFunction 实例与对应上下文
        IRFunction irFunction = new IRFunction(functionNode.name());
        IRContext  irContext  = new IRContext(irFunction);

        // 1) 把函数返回类型注入为默认类型后缀（供表达式类型推断）
        char _returnSuffix = switch (functionNode.returnType().toLowerCase()) {
            case "double" -> 'd';
            case "float"  -> 'f';
            case "long"   -> 'l';
            case "short"  -> 's';
            case "byte"   -> 'b';
            default       -> '\0';
        };
        ExpressionUtils.setDefaultSuffix(_returnSuffix);

        try {
            // 2) 声明形参：为每个参数分配虚拟寄存器并声明到作用域
            for (ParameterNode p : functionNode.parameters()) {
                IRVirtualRegister reg = irFunction.newRegister(); // 新寄存器
                irContext.getScope().declare(p.name(), p.type(), reg);      // 变量名→寄存器绑定
                irFunction.addParameter(reg);                     // 添加到函数参数列表
            }

            // 3) 生成函数体 IR：遍历每条语句，逐一转化
            StatementBuilder stmtBuilder = new StatementBuilder(irContext);
            for (StatementNode stmt : functionNode.body()) {
                stmtBuilder.build(stmt);
            }
        } finally {
            // 4) 清除默认后缀，避免影响后续函数的推断
            ExpressionUtils.clearDefaultSuffix();
        }

        // 返回构建好的 IRFunction
        return irFunction;
    }
}
