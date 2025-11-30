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
import java.util.Locale;
import java.util.Map;

/**
 * <p><b>new 表达式构建处理器。</b></p>
 *
 * <p>
 * 负责将 {@code new T(a, b, c)} 或结构体/类实例化相关的 AST 节点转换为 IR 构建流程。
 * 支持：
 * </p>
 * <ul>
 *   <li>为新实例分配虚拟寄存器并初始化默认结构</li>
 *   <li>依次编译构造参数表达式并写入对应字段位置</li>
 *   <li>根据字段类型选择合适的下标写入函数（__setindex_*）</li>
 *   <li>自动调用构造函数（例如 {@code T.__init__N}）</li>
 * </ul>
 */
public class NewHandler implements ExpressionHandler<NewExpressionNode> {

    /**
     * 处理 new 表达式并生成对应 IR 指令。
     *
     * <p>主要流程：</p>
     * <ol>
     *     <li>分配目标寄存器并构造空实例结构</li>
     *     <li>遍历并构建所有参数表达式，分别写入实例字段</li>
     *     <li>根据字段类型选择合适的赋值通道函数</li>
     *     <li>若类型有布局，则自动追加构造函数调用指令</li>
     * </ol>
     *
     * @param b    表达式构建器，用于生成 IR 指令与寄存器
     * @param node new 表达式 AST 节点，包含类型名与构造参数
     * @return 存放新建对象实例引用的虚拟寄存器
     */
    @Override
    public IRVirtualRegister handle(ExpressionBuilder b, NewExpressionNode node) {

        // 1. 创建实例寄存器，默认初始化为空列表（结构体字段容器）
        IRVirtualRegister dest = b.ctx().newRegister();
        b.ctx().getScope().setRegisterType(dest, node.typeName());
        InstructionFactory.loadConstInto(b.ctx(), dest, new IRConstant(java.util.List.of()));

        // 2. 遍历构造参数，依次求值并写入对应字段位置
        List<IRVirtualRegister> argRegs = new ArrayList<>();
        Map<String, Integer> layout = IRBuilderScope.getStructLayout(node.typeName());

        for (int i = 0; i < node.arguments().size(); i++) {

            // 2.1 解析字段类型
            String fieldType = resolveFieldTypeByIndex(
                    layout,
                    IRBuilderScope.getStructFieldTypes(node.typeName()),
                    i
            );

            // 为表达式求值设置字段类型上下文
            if (fieldType != null) {
                b.ctx().setVarType(fieldType);
            }

            // 求值构造参数
            IRVirtualRegister argReg = b.build(node.arguments().get(i));
            b.ctx().clearVarType();
            argRegs.add(argReg);

            // 2.2 加载字段下标
            IRVirtualRegister idxReg = b.ctx().newTempRegister();
            InstructionFactory.loadConstInto(b.ctx(), idxReg, new IRConstant(i));

            // 2.3 选择对应类型的 __setindex_* 函数并写入字段
            String setFn = selectSetIndexFunc(fieldType);
            b.ctx().addInstruction(new CallInstruction(null, setFn, List.of(dest, idxReg, argReg)));
        }

        // 3. 若类型为结构体，则自动调用构造函数 T.__init__N
        if (IRBuilderScope.getStructLayout(node.typeName()) != null) {
            String ctorName = node.typeName() + ".__init__" + argRegs.size();
            List<IRValue> ctorArgs = new ArrayList<>();
            ctorArgs.add(dest);
            ctorArgs.addAll(argRegs);
            b.ctx().addInstruction(new CallInstruction(null, ctorName, ctorArgs));
        }

        // 4. 返回实例寄存器
        return dest;
    }

    /**
     * 根据字段类型选择对应的“__setindex_*”写入函数。
     *
     * <p>用于根据基本类型选择最优的下标赋值通道。</p>
     *
     * @param fieldType 字段类型（可能为 null）
     * @return 对应的 __setindex_* 函数名
     */
    private String selectSetIndexFunc(String fieldType) {
        if (fieldType == null || fieldType.isBlank()) {
            return "__setindex_r";
        }
        return switch (fieldType.toLowerCase(Locale.ROOT)) {
            case "byte" -> "__setindex_b";
            case "short" -> "__setindex_s";
            case "int", "integer", "bool", "boolean" -> "__setindex_i";
            case "long" -> "__setindex_l";
            case "float" -> "__setindex_f";
            case "double" -> "__setindex_d";
            default -> "__setindex_r";
        };
    }

    /**
     * 根据字段索引反查字段类型。
     *
     * <p>
     * Layout 存储字段名称到下标的映射，本方法通过下标查找对应字段，
     * 再从字段类型映射中取出类型。
     * </p>
     *
     * @param layout     字段布局映射：字段名 → 下标
     * @param fieldTypes 字段类型映射：字段名 → 类型名
     * @param index      需要查询的字段下标
     * @return 对应字段的类型，若不存在则返回 null
     */
    private String resolveFieldTypeByIndex(Map<String, Integer> layout,
                                           Map<String, String> fieldTypes,
                                           int index) {
        if (layout == null || fieldTypes == null) {
            return null;
        }
        for (Map.Entry<String, Integer> e : layout.entrySet()) {
            if (e.getValue() == index) {
                return fieldTypes.get(e.getKey());
            }
        }
        return null;
    }
}