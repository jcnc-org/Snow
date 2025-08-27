package org.jcnc.snow.compiler.ir.builder;

import org.jcnc.snow.compiler.ir.common.GlobalConstTable;
import org.jcnc.snow.compiler.ir.common.GlobalFunctionTable;
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
 *
 * <ul>
 *   <li>支持自动导入全局/跨模块常量，使跨模块常量引用（如 ModuleA.a）在 IR 阶段可用。</li>
 *   <li>将函数形参声明为虚拟寄存器，并注册到作用域，便于后续指令生成。</li>
 *   <li>根据返回类型设置表达式默认字面量类型，保证 IR 层类型一致性。</li>
 *   <li>遍历并转换函数体语句为 IR 指令。</li>
 * </ul>
 */
public class FunctionBuilder {

    /**
     * 将 AST 中的 FunctionNode 构建为可执行的 IRFunction。
     * <p>
     * 构建过程包括:
     * <ol>
     *     <li>在全局函数表注册函数签名，便于后续模块/语义分析阶段查询。</li>
     *     <li>初始化 IRFunction 实例和 IRContext 上下文对象（包含作用域与寄存器信息）。</li>
     *     <li>自动导入全局常量（包括跨模块 const 变量）到当前作用域，
     *         使成员访问如 ModuleA.a 可直接折叠为常量。</li>
     *     <li>根据函数返回类型，设置表达式推断的默认字面量类型后缀
     *         （如 double→d, float→f），避免类型不一致。</li>
     *     <li>遍历声明形参，每个参数分配虚拟寄存器，并注册到作用域。</li>
     *     <li>依次转换函数体中的每条语句为 IR 指令。</li>
     *     <li>函数体转换完成后，清理默认类型后缀，防止影响后续函数构建。</li>
     * </ol>
     *
     * @param functionNode 表示函数定义的语法树节点
     * @return 构建得到的 IRFunction 对象
     */
    public IRFunction build(FunctionNode functionNode) {

        // 1. 在全局函数表注册函数名与返回类型
        // 方便其他阶段/模块调用、类型检查。
        GlobalFunctionTable.register(functionNode.name(), functionNode.returnType());

        // 2. 初始化 IRFunction 实例与上下文对象
        // IRFunction: 表示该函数的中间代码容器
        // IRContext: 负责作用域、寄存器分配等编译上下文管理
        IRFunction irFunction = new IRFunction(functionNode.name());
        IRContext  irContext  = new IRContext(irFunction);

        // 3. 自动导入所有全局/跨模块常量到当前作用域
        // 支持如 ModuleA.a 这样的常量访问/折叠（参见 ExpressionBuilder）
        GlobalConstTable.all().forEach((k, v) ->
                irContext.getScope().importExternalConst(k, v));

        // 4. 根据函数返回类型设置默认类型后缀
        // 例如返回类型为 double 时, 字面量表达式自动用 d 后缀。
        char _returnSuffix = switch (functionNode.returnType().toLowerCase()) {
            case "double" -> 'd';
            case "float"  -> 'f';
            case "long"   -> 'l';
            case "short"  -> 's';
            case "byte"   -> 'b';
            default       -> '\0'; // 其它类型不设默认后缀
        };
        ExpressionUtils.setDefaultSuffix(_returnSuffix);

        try {
            // 5. 遍历函数参数列表
            //   - 为每个参数分配一个新的虚拟寄存器
            //   - 注册参数名、类型、寄存器到当前作用域
            //   - 添加参数寄存器到 IRFunction（用于后续调用与指令生成）
            for (ParameterNode p : functionNode.parameters()) {
                IRVirtualRegister reg = irFunction.newRegister();
                irContext.getScope().declare(p.name(), p.type(), reg);
                irFunction.addParameter(reg);
            }

            // 6. 遍历函数体语句节点，转换为 IR 指令
            //    StatementBuilder 负责将每条语句递归转换为 IR
            StatementBuilder stmtBuilder = new StatementBuilder(irContext);
            for (StatementNode stmt : functionNode.body()) {
                stmtBuilder.build(stmt);
            }
        } finally {
            // 7. 清理默认类型后缀，防止影响后续其他函数的类型推断
            ExpressionUtils.clearDefaultSuffix();
        }

        // 8. 返回构建完成的 IRFunction
        return irFunction;
    }
}
