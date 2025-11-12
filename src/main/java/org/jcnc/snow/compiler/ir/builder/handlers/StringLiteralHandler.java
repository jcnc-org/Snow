package org.jcnc.snow.compiler.ir.builder.handlers;

import org.jcnc.snow.compiler.ir.builder.expression.ExpressionBuilder;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionHandler;
import org.jcnc.snow.compiler.ir.instruction.LoadConstInstruction;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.StringLiteralNode;

/**
 * 字符串字面量处理器。
 * <p>
 * 该类负责将字符串常量（如 "abc"）从语法树节点转换为 IR 层的常量加载指令 {@link LoadConstInstruction}。
 * <p>
 * 生成的指令会将字符串值加载到新分配的 {@link IRVirtualRegister} 中，
 * 并在作用域中标注其类型为 "string"。
 */
public class StringLiteralHandler implements ExpressionHandler<StringLiteralNode> {

    /**
     * 处理字符串字面量表达式，生成 IR 常量加载指令。
     * <p>
     * 该方法会：
     * <ol>
     *   <li>创建 {@link IRConstant} 以封装字符串值；</li>
     *   <li>分配新的虚拟寄存器用于存放结果；</li>
     *   <li>生成并添加 {@link LoadConstInstruction} 指令；</li>
     *   <li>在作用域中设置寄存器类型为 {@code string}。</li>
     * </ol>
     *
     * @param b 表达式构建器，用于访问当前 {@link org.jcnc.snow.compiler.ir.builder.core.IRContext}
     * @param n 字符串字面量 AST 节点
     * @return 存放字符串常量的虚拟寄存器
     */
    @Override
    public IRVirtualRegister handle(ExpressionBuilder b, StringLiteralNode n) {
        // 1. 创建字符串常量
        IRConstant c = new IRConstant(n.value());
        // 2. 分配新的虚拟寄存器
        IRVirtualRegister r = b.ctx().newRegister();
        // 3. 生成并添加常量加载指令
        b.ctx().addInstruction(new LoadConstInstruction(r, c));
        // 4. 标注寄存器类型
        b.ctx().getScope().setRegisterType(r, "string");
        return r;
    }
}
