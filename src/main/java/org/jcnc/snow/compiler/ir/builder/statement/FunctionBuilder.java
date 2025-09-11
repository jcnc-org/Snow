package org.jcnc.snow.compiler.ir.builder.statement;

import org.jcnc.snow.compiler.ir.builder.core.IRContext;
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
 * 将语法树中的 {@link FunctionNode} 转换为可执行的 {@link IRFunction}，包含：
 * <ul>
 *   <li>在全局函数表登记函数名与返回类型；</li>
 *   <li>初始化 IR 容器与构建上下文（作用域、寄存器池等）；</li>
 *   <li>导入全局/跨模块常量，便于常量折叠与跨模块引用；</li>
 *   <li>依据返回类型为表达式设置数字字面量默认后缀；</li>
 *   <li>分配形参寄存器并注册到作用域与 IR 函数；</li>
 *   <li>将函数体语句逐条转为 IR 指令；</li>
 *   <li>在构建完成后清理默认字面量后缀，避免泄漏到其它函数。</li>
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
        // 1) 在全局函数表登记：名称 + 返回类型（返回类型可能为 null，例如构造函数 init）
        GlobalFunctionTable.register(functionNode.name(), functionNode.returnType());

        // 2) 初始化 IR 容器与上下文
        IRFunction irFunction = new IRFunction(functionNode.name());
        IRContext irContext = new IRContext(irFunction);

        // 3) 导入所有全局/跨模块常量到当前作用域（便于常量折叠和跨模块引用）
        GlobalConstTable.all().forEach((k, v) ->
                irContext.getScope().importExternalConst(k, v));

        // 4) 根据函数返回类型设置“数字字面量默认后缀”
        //    - 关键修复：对 returnType 进行空值/空白保护，缺省视为 "void"
        String rt = functionNode.returnType();
        String rtLower = (rt == null || rt.trim().isEmpty()) ? "void" : rt.trim().toLowerCase();

        // 根据返回类型决定默认字面量后缀
        // 仅在浮点/整型长短类型上设置；其它/void 情况不设置（使用 '\0' 表示不设置）
        char defaultSuffix = switch (rtLower) {
            case "double" -> 'd';
            case "float" -> 'f';
            case "long" -> 'l';
            case "short" -> 's';
            case "byte" -> 'b';
            default -> '\0';
        };
        ExpressionUtils.setDefaultSuffix(defaultSuffix);

        try {
            // 5) 处理形参：
            //    - 为每个形参分配一个新的虚拟寄存器（从 IRContext 统一分配，保证作用域一致）
            //    - 将 (参数名, 类型, 寄存器) 声明到当前作用域
            //    - 将寄存器加入 IRFunction 的参数列表，便于后续调用/生成
            for (ParameterNode p : functionNode.parameters()) {
                IRVirtualRegister reg = irContext.newRegister(); // 使用上下文统一分配
                irContext.getScope().declare(p.name(), p.type(), reg);
                irFunction.addParameter(reg);
            }

            // 6) 构建函数体：将每条语句节点转换为 IR 指令序列
            StatementBuilder stmtBuilder = new StatementBuilder(irContext);
            for (StatementNode stmt : functionNode.body()) {
                stmtBuilder.build(stmt);
            }
        } finally {
            // 7) 清理默认后缀，防止影响后续函数的字面量推断
            ExpressionUtils.clearDefaultSuffix();
        }

        // 8) 返回构建完成的 IRFunction
        return irFunction;
    }
}
