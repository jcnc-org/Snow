package org.jcnc.snow.compiler.ir.builder.handlers;

import org.jcnc.snow.compiler.ir.builder.expression.ExpressionBuilder;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionHandler;
import org.jcnc.snow.compiler.ir.instruction.LoadConstInstruction;
import org.jcnc.snow.compiler.ir.utils.ExpressionUtils;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.NumberLiteralNode;

/**
 * 数字字面量处理器。
 * <p>
 * 该类负责将数字字面量（整数或浮点数）从语法树节点转换为对应的
 * IR 常量加载指令 {@link LoadConstInstruction}。
 * <p>
 * 它会根据数值的具体类型（byte、short、int、long、float、double）
 * 自动推断寄存器类型，并将结果存储在新的 {@link IRVirtualRegister} 中。
 */
public class NumberLiteralHandler implements ExpressionHandler<NumberLiteralNode> {

    /**
     * 处理数字字面量表达式节点，生成相应的 IR 常量加载指令。
     * <p>
     * 该方法会：
     * <ol>
     *   <li>使用 {@link ExpressionUtils#buildNumberConstant} 自动判断数值类型并生成 {@link IRConstant}；</li>
     *   <li>为结果分配新的 {@link IRVirtualRegister}；</li>
     *   <li>向上下文添加 {@link LoadConstInstruction} 指令；</li>
     *   <li>根据常量值类型设置寄存器类型信息。</li>
     * </ol>
     *
     * @param b 表达式构建器，提供当前 {@link org.jcnc.snow.compiler.ir.builder.core.IRContext} 等上下文信息
     * @param n 数字字面量 AST 节点
     * @return 存放常量值的虚拟寄存器
     */
    @Override
    public IRVirtualRegister handle(ExpressionBuilder b, NumberLiteralNode n) {
        // 1. 自动判断数字类型并生成常量
        IRConstant c = ExpressionUtils.buildNumberConstant(b.ctx(), n.value());

        // 2. 分配新的虚拟寄存器
        IRVirtualRegister r = b.ctx().newRegister();

        // 3. 添加常量加载指令
        b.ctx().addInstruction(new LoadConstInstruction(r, c));

        // 4. 根据常量类型设置寄存器类型
        Object value = c.value();
        if (value instanceof Byte) {
            b.ctx().getScope().setRegisterType(r, "byte");
        } else if (value instanceof Short) {
            b.ctx().getScope().setRegisterType(r, "short");
        } else if (value instanceof Integer) {
            b.ctx().getScope().setRegisterType(r, "int");
        } else if (value instanceof Long) {
            b.ctx().getScope().setRegisterType(r, "long");
        } else if (value instanceof Float) {
            b.ctx().getScope().setRegisterType(r, "float");
        } else if (value instanceof Double) {
            b.ctx().getScope().setRegisterType(r, "double");
        }

        return r;
    }
}
