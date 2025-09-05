package org.jcnc.snow.compiler.ir.builder.handlers;

import org.jcnc.snow.compiler.ir.builder.core.InstructionFactory;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionBuilder;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionHandler;
import org.jcnc.snow.compiler.ir.builder.utils.IndexRefHelper;
import org.jcnc.snow.compiler.ir.builder.utils.TryFoldConst;
import org.jcnc.snow.compiler.ir.core.IRValue;
import org.jcnc.snow.compiler.ir.instruction.CallInstruction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.IdentifierNode;
import org.jcnc.snow.compiler.parser.ast.IndexExpressionNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 下标访问（IndexExpressionNode）处理器。
 * <p>
 * 负责处理数组、列表、字符串等的下标访问、支持常量折叠、支持多维数组引用优化。
 * 支持不同类型（如 int、float、string、byte 等）的下标通道自动选择。
 */
public class IndexHandler implements ExpressionHandler<IndexExpressionNode> {
    /**
     * 处理下标表达式，生成 IR 指令，返回目标寄存器。
     *
     * @param b    表达式构建器
     * @param node 下标访问 AST 节点
     * @return 存放下标访问结果的虚拟寄存器
     */
    @Override
    public IRVirtualRegister handle(ExpressionBuilder b, IndexExpressionNode node) {
        // 1. 尝试对 array 和 index 执行常量折叠（编译期直接求值）
        TryFoldConst tf = new TryFoldConst(b.ctx());
        Object arrConst = tf.apply(node.array());
        Object idxConst = tf.apply(node.index());

        // 2. 若为可折叠的编译期常量数组下标，直接生成常量加载指令
        if (arrConst instanceof java.util.List<?> list && idxConst instanceof Number num) {
            int i = num.intValue();
            if (i < 0 || i >= list.size()) {
                throw new IllegalStateException("数组下标越界: " + i + " (长度 " + list.size() + ")");
            }
            Object elem = list.get(i);
            IRVirtualRegister r = b.ctx().newRegister();
            InstructionFactory.loadConstInto(b.ctx(), r,
                    new org.jcnc.snow.compiler.ir.value.IRConstant(elem));
            return r;
        }

        // 3. 支持多维数组：如果 array 是嵌套的 IndexExpression，则用 IndexRefHelper 构造“引用”
        IRVirtualRegister arrReg = (node.array() instanceof IndexExpressionNode inner)
                ? new IndexRefHelper(b).build(inner)
                : b.build(node.array());

        // 4. 下标表达式本身寄存器
        IRVirtualRegister idxReg = b.build(node.index());
        IRVirtualRegister dest = b.ctx().newRegister();

        // 5. 构造参数列表：array寄存器、下标寄存器
        List<IRValue> argv = new ArrayList<>();
        argv.add(arrReg);
        argv.add(idxReg);

        // 6. 分派调用不同类型的下标访问（int/float/string/引用类型等）
        if (node.array() instanceof IndexExpressionNode) {
            // 多维中间层，下标引用指令
            b.ctx().addInstruction(new CallInstruction(dest, "__index_r", argv));
        } else {
            // 一般类型，按声明类型选择专用通道
            String func = "__index_i"; // 默认 int 通道
            if (node.array() instanceof IdentifierNode id) {
                String declType = b.ctx().getScope().lookupType(id.name());
                if (declType != null) {
                    String base = declType.toLowerCase();
                    int p = base.indexOf('[');
                    if (p > 0) base = base.substring(0, p);
                    // 类型分派：基础类型选择专用 IR 通道
                    switch (base) {
                        case "byte" -> func = "__index_b";
                        case "short" -> func = "__index_s";
                        case "int" -> func = "__index_i";
                        case "long" -> func = "__index_l";
                        case "float" -> func = "__index_f";
                        case "double" -> func = "__index_d";
                        case "boolean" -> func = "__index_i"; // bool 用 int 通道
                        case "string" -> func = "__index_r";
                        default -> func = "__index_r"; // 其它/引用类型走通用通道
                    }
                }
            }
            b.ctx().addInstruction(new CallInstruction(dest, func, argv));
        }
        return dest;
    }
}
