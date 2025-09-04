package org.jcnc.snow.compiler.ir.builder.handlers;

import org.jcnc.snow.compiler.ir.builder.expression.ExpressionBuilder;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionHandler;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.IdentifierNode;
import org.jcnc.snow.compiler.parser.ast.MemberExpressionNode;

/**
 * 标识符表达式处理器。
 * <p>
 * 负责将变量名、标识符访问转换为 IR 虚拟寄存器查找指令。
 * 支持局部变量查找、成员字段自动查找（即 this.x 语法糖），
 * 并在无法解析时抛出未定义标识符异常。
 */
public class IdentifierHandler implements ExpressionHandler<IdentifierNode> {
    /**
     * 处理标识符表达式，返回其对应的虚拟寄存器。
     * 优先查找局部变量；若不存在则查找 this 对象上的字段；均不存在时抛出异常。
     *
     * @param b  表达式构建器
     * @param id 标识符 AST 节点
     * @return 标识符所对应的虚拟寄存器
     * @throws IllegalStateException 如果变量/字段均未定义，抛出异常
     */
    @Override
    public IRVirtualRegister handle(ExpressionBuilder b, IdentifierNode id) {
        // 1. 优先尝试查找当前作用域下的局部变量/参数
        IRVirtualRegister reg = b.ctx().getScope().lookup(id.name());
        if (reg != null) return reg;

        // 2. 若为成员访问（类成员），则转换为 this.<field>
        IRVirtualRegister thisReg = b.ctx().getScope().lookup("this");
        String thisType = b.ctx().getScope().lookupType("this");
        if (thisReg != null && thisType != null) {
            // 构造一个等价的 this.<field> 成员表达式，并交给 MemberHandler 处理
            MemberExpressionNode asField = new MemberExpressionNode(
                    new IdentifierNode("this", id.context()), id.name(), id.context());
            return new MemberHandler().handle(b, asField);
        }

        // 3. 未找到则抛出异常
        throw new IllegalStateException("未定义标识符: " + id.name());
    }
}
