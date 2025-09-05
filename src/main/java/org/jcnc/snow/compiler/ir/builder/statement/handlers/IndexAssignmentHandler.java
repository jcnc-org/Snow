package org.jcnc.snow.compiler.ir.builder.statement.handlers;

import org.jcnc.snow.compiler.ir.builder.expression.ExpressionBuilder;
import org.jcnc.snow.compiler.ir.builder.statement.IStatementHandler;
import org.jcnc.snow.compiler.ir.builder.statement.StatementBuilderContext;
import org.jcnc.snow.compiler.ir.builder.utils.IndexRefHelper;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.IdentifierNode;
import org.jcnc.snow.compiler.parser.ast.IndexAssignmentNode;
import org.jcnc.snow.compiler.parser.ast.IndexExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

/**
 * 下标赋值语句处理器（数组/集合元素赋值）。
 * <p>
 * 负责将AST中的下标赋值节点（如 a[1] = v 或 a[i][j] = v）编译为IR层的数组/集合元素赋值指令。
 * 支持多维数组链式赋值、自动类型分派和常量绑定自动清理等功能。
 * </p>
 */
public class IndexAssignmentHandler implements IStatementHandler {

    /**
     * 判断是否可以处理给定的语句节点。
     *
     * @param stmt AST语句节点
     * @return 若为IndexAssignmentNode类型则返回true，否则返回false
     */
    @Override
    public boolean canHandle(StatementNode stmt) {
        return stmt instanceof IndexAssignmentNode;
    }

    /**
     * 处理下标赋值节点，生成数组/集合元素赋值的IR指令。
     * <p>
     * 处理流程：
     * <ol>
     *     <li>递归获取多维数组/集合的目标寄存器</li>
     *     <li>生成下标和值的表达式寄存器</li>
     *     <li>根据目标变量类型选择具体的内置赋值函数</li>
     *     <li>生成CALL指令</li>
     *     <li>清理常量绑定，保证数据一致性</li>
     * </ol>
     * </p>
     *
     * @param stmt AST语句节点（已保证为IndexAssignmentNode类型）
     * @param c    语句构建上下文
     */
    @Override
    public void handle(StatementNode stmt, StatementBuilderContext c) {
        IndexAssignmentNode idxAssign = (IndexAssignmentNode) stmt;
        IndexExpressionNode target = idxAssign.target();

        // 1. 目标数组寄存器：多维时用 buildIndexRef 拿引用
        ExpressionBuilder expr = c.expr();
        IRVirtualRegister arrReg = (target.array() instanceof IndexExpressionNode inner)
                ? new IndexRefHelper(expr).build(inner)
                : expr.build(target.array());

        // 2. 下标与右值
        IRVirtualRegister idxReg = expr.build(target.index());
        IRVirtualRegister valReg = expr.build(idxAssign.value());

        // 3. 根据元素类型分派内置函数（自动选择字节/整型/浮点/引用类型等专用set指令）
        String func = "__setindex_r";
        ExpressionNode base = target.array();
        while (base instanceof IndexExpressionNode innerIdx) base = innerIdx.array();
        if (base instanceof IdentifierNode id) {
            String arrType = c.ctx().getScope().lookupType(id.name());
            if (arrType != null) {
                String elemType = arrType.endsWith("[]") ? arrType.substring(0, arrType.length() - 2) : arrType;
                switch (elemType) {
                    case "byte" -> func = "__setindex_b";
                    case "short" -> func = "__setindex_s";
                    case "int" -> func = "__setindex_i";
                    case "long" -> func = "__setindex_l";
                    case "float" -> func = "__setindex_f";
                    case "double" -> func = "__setindex_d";
                    case "boolean" -> func = "__setindex_i";
                    case "string" -> func = "__setindex_r";
                    default -> func = "__setindex_r";
                }
            }
        }

        // 4. 生成CALL指令
        java.util.List<org.jcnc.snow.compiler.ir.core.IRValue> argv =
                java.util.List.of(arrReg, idxReg, valReg);
        c.ctx().addInstruction(new org.jcnc.snow.compiler.ir.instruction.CallInstruction(null, func, argv));

        // 5. 清理常量绑定（被赋值的变量不再为常量，保持语义一致）
        try {
            if (base instanceof IdentifierNode id2) {
                c.ctx().getScope().clearConstValue(id2.name());
            }
        } catch (Throwable ignored) {
            // 忽略所有异常，确保不会影响主流程
        }
    }
}
