package org.jcnc.snow.compiler.ir.builder.utils;

import org.jcnc.snow.compiler.ir.builder.core.InstructionFactory;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionBuilder;
import org.jcnc.snow.compiler.ir.core.IRValue;
import org.jcnc.snow.compiler.ir.instruction.CallInstruction;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.IndexExpressionNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 多维数组中间层下标访问辅助工具。
 * <p>
 * 用于处理多维数组或嵌套下标访问时，每一层的引用生成（即返回“引用”而不是最终值），
 * 便于链式下标读取与 IR 生成。
 */
public record IndexRefHelper(ExpressionBuilder builder) {

    /**
     * 递归构建多维下标引用寄存器。
     * <p>
     * 若可做常量折叠，直接返回常量寄存器；否则递归处理每层，最终生成 __index_r 指令，返回中间层的引用。
     *
     * @param node 多维下标 AST 节点
     * @return     存放下标引用的虚拟寄存器
     */
    public IRVirtualRegister build(IndexExpressionNode node) {
        // 1. 常量折叠：编译期求值优化
        TryFoldConst tf = new TryFoldConst(builder.ctx());
        Object arrConst = tf.apply(node.array());
        Object idxConst = tf.apply(node.index());

        if (arrConst instanceof List<?> list && idxConst instanceof Number num) {
            int i = num.intValue();
            if (i < 0 || i >= list.size()) {
                throw new IllegalStateException("数组下标越界: " + i + " (长度 " + list.size() + ")");
            }
            Object elem = list.get(i);
            IRVirtualRegister r = builder.ctx().newRegister();
            InstructionFactory.loadConstInto(builder.ctx(), r, new IRConstant(elem));
            return r;
        }

        // 2. 递归处理：array 部分为多层下标时继续递归，否则直接 build
        IRVirtualRegister arrReg = (node.array() instanceof IndexExpressionNode inner)
                ? build(inner)
                : builder.build(node.array());

        // 3. 求下标寄存器
        IRVirtualRegister idxReg = builder.build(node.index());
        IRVirtualRegister dest = builder.ctx().newRegister();

        // 4. 构造参数并生成 __index_r 指令（返回“引用”而非具体数值）
        List<IRValue> argv = new ArrayList<>();
        argv.add(arrReg);
        argv.add(idxReg);

        builder.ctx().addInstruction(new CallInstruction(dest, "__index_r", argv));
        return dest;
    }
}
