package org.jcnc.snow.compiler.ir.builder.handlers;

import org.jcnc.snow.compiler.ir.builder.core.IRBuilderScope;
import org.jcnc.snow.compiler.ir.builder.core.InstructionFactory;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionBuilder;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionHandler;
import org.jcnc.snow.compiler.ir.core.IRValue;
import org.jcnc.snow.compiler.ir.instruction.CallInstruction;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.NewExpressionNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 数组字面量表达式处理器。
 * <p>
 * 负责将 <code>[a, b, c]</code> 数组字面量节点编译为 IR 常量表达式
 * <p>
 * 转换流程:
 * <ol>
 *   <li>递归遍历每个元素，确保全为编译期常量</li>
 *   <li>生成对应的 IRConstant 及 LoadConstInstruction 指令</li>
 *   <li>如遇嵌套数组，递归构建多维常量结构</li>
 * </ol>
 */

public class NewHandler implements ExpressionHandler<NewExpressionNode> {
    /**
     * 处理 new 表达式，返回实例对象寄存器。
     *
     * @param b    表达式构建器
     * @param node new 表达式 AST 节点
     * @return 存放新建对象实例的虚拟寄存器
     */
    @Override
    public IRVirtualRegister handle(ExpressionBuilder b, NewExpressionNode node) {
        // 1. 分配一个寄存器用于存放新对象，并初始化为空列表
        IRVirtualRegister dest = b.ctx().newRegister();
        InstructionFactory.loadConstInto(b.ctx(), dest, new IRConstant(java.util.List.of()));

        // 2. 遍历参数表达式，依次求值写入新对象的下标（模拟数组/结构体的字段）
        List<IRVirtualRegister> argRegs = new ArrayList<>();
        for (int i = 0; i < node.arguments().size(); i++) {
            // 2.1. 求每个参数的值
            IRVirtualRegister argReg = b.build(node.arguments().get(i));
            argRegs.add(argReg);

            // 2.2. 下标寄存器（参数顺序映射）
            IRVirtualRegister idxReg = b.ctx().newTempRegister();
            InstructionFactory.loadConstInto(b.ctx(), idxReg, new IRConstant(i));

            // 2.3. 调用通用下标赋值方法（将参数写入新对象的对应字段/位置）
            b.ctx().addInstruction(new CallInstruction(null, "__setindex_r", List.of(dest, idxReg, argReg)));
        }

        // 3. 若该类型存在结构体布局，则自动调用构造函数（__init__N）
        if (IRBuilderScope.getStructLayout(node.typeName()) != null) {
            String ctorName = node.typeName() + ".__init__" + argRegs.size();
            List<IRValue> ctorArgs = new ArrayList<>();
            ctorArgs.add(dest);           // 第一个参数为实例自身
            ctorArgs.addAll(argRegs);     // 后续为构造参数
            b.ctx().addInstruction(new CallInstruction(null, ctorName, ctorArgs));
        }

        // 4. 返回结果寄存器
        return dest;
    }
}
