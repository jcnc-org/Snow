package org.jcnc.snow.compiler.ir.builder.handlers;

import org.jcnc.snow.compiler.ir.builder.expression.ExpressionBuilder;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionHandler;
import org.jcnc.snow.compiler.ir.instruction.LoadConstInstruction;
import org.jcnc.snow.compiler.ir.utils.ExpressionUtils;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.ArrayLiteralNode;
import org.jcnc.snow.compiler.parser.ast.BoolLiteralNode;
import org.jcnc.snow.compiler.parser.ast.NumberLiteralNode;
import org.jcnc.snow.compiler.parser.ast.StringLiteralNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code ArrayLiteralHandler}
 * <p>
 * 处理数组字面量表达式（ArrayLiteralNode）的 IR 构建逻辑，将所有元素递归转换为 IRConstant，并生成加载常量数组的指令。
 * <p>
 * 目前仅支持全部为常量（数字、字符串、布尔值、嵌套数组）的数组元素。
 * 若出现非常量元素，则会抛出异常。
 */
public class ArrayLiteralHandler implements ExpressionHandler<ArrayLiteralNode> {

    /**
     * 构建数组字面量表达式，对应为编译期常量数组。
     *
     * @param b   表达式构建器
     * @param arr 数组字面量 AST 节点
     * @return 存放数组常量的虚拟寄存器
     */
    @Override
    public IRVirtualRegister handle(ExpressionBuilder b, ArrayLiteralNode arr) {
        // 1. 构造常量数组
        IRConstant c = buildArrayConstant(b, arr);
        // 2. 分配目标寄存器
        IRVirtualRegister r = b.ctx().newRegister();
        // 3. 加载常量指令
        b.ctx().addInstruction(new LoadConstInstruction(r, c));
        return r;
    }

    /**
     * 递归构建数组常量（仅允许常量表达式元素）。
     *
     * @param b   表达式构建器
     * @param arr 数组字面量 AST 节点
     * @return IRConstant：包含所有元素的 Java List
     * @throws IllegalStateException 若数组元素包含非常量表达式，抛出异常
     */
    private IRConstant buildArrayConstant(ExpressionBuilder b, ArrayLiteralNode arr) {
        List<Object> list = new ArrayList<>();
        for (ExpressionNode e : arr.elements()) {
            switch (e) {
                case NumberLiteralNode n ->
                    // 数字字面量
                        list.add(ExpressionUtils.buildNumberConstant(b.ctx(), n.value()).value());
                case StringLiteralNode s ->
                    // 字符串字面量
                        list.add(s.value());
                case BoolLiteralNode bl ->
                    // 布尔字面量：以 1/0 形式保存
                        list.add(bl.getValue() ? 1 : 0);
                case ArrayLiteralNode inner ->
                    // 嵌套数组：递归处理
                        list.add(buildArrayConstant(b, inner).value());
                default ->
                    // 不支持的元素类型
                        throw new IllegalStateException(
                                "file:///" + e.context().file() + ":" + e.context().line() + ":" + e.context().column() +
                                        ": 暂不支持含非常量元素的数组字面量: " + e.getClass().getSimpleName()
                        );
            }
        }
        // 返回不可变 List 封装的 IRConstant
        return new IRConstant(List.copyOf(list));
    }
}
