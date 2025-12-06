package org.jcnc.snow.compiler.ir.builder.statement;

import org.jcnc.snow.compiler.ir.builder.core.IRContext;
import org.jcnc.snow.compiler.ir.common.GlobalConstTable;
import org.jcnc.snow.compiler.ir.common.GlobalFunctionTable;
import org.jcnc.snow.compiler.ir.common.GlobalVariableTable.GlobalVariable;
import org.jcnc.snow.compiler.ir.core.IRFunction;
import org.jcnc.snow.compiler.ir.utils.ExpressionUtils;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.ParameterNode;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * IR 函数构建器。
 *
 * <p>负责将语法树中的 {@link FunctionNode} 转换为可执行的 {@link IRFunction}，
 * 是前端 IR 生成阶段的核心组件之一。构建内容包括函数签名登记、作用域初始化、
 * 全局常量与全局变量导入、形参寄存器分配、函数体语句构建等。
 *
 * <p>主要职责：
 * <ul>
 *     <li>在全局函数表中登记函数签名，便于跨函数/跨模块链接与语义分析；</li>
 *     <li>初始化 IRFunction 与 IRContext（作用域、虚拟寄存器池等）；</li>
 *     <li>将模块级全局变量的寄存器提前注入到作用域，使各函数共享同一寄存器槽位；</li>
 *     <li>导入跨模块/全局常量，便于常量折叠与跨模块常量引用；</li>
 *     <li>根据返回类型设置数字字面量的默认推断后缀（如 double→d），避免类型冲突；</li>
 *     <li>为形参分配统一的虚拟寄存器，并登记到当前作用域；</li>
 *     <li>将函数体的每条 AST 语句转换为 IR 指令序列；</li>
 *     <li>构建完成后清理默认字面量后缀，避免泄漏影响下一次构建。</li>
 * </ul>
 *
 * <p>本构建器支持接收模块级全局变量，以保证跨函数构建过程中共享这些变量的寄存器槽位。
 */
public class FunctionBuilder {

    /**
     * 当前模块的全局变量集合（可能为空）。用于为全函数提供共享寄存器槽位。
     */
    private final Collection<GlobalVariable> moduleGlobals;

    /**
     * 创建一个默认的函数构建器，不包含任何模块级全局变量。
     */
    public FunctionBuilder() {
        this(Collections.emptyList());
    }

    /**
     * 创建一个包含模块级全局变量的函数构建器。
     *
     * @param moduleGlobals 当前模块的全局变量集合，可为 null
     */
    public FunctionBuilder(Collection<GlobalVariable> moduleGlobals) {
        this.moduleGlobals = moduleGlobals == null ? Collections.emptyList() : moduleGlobals;
    }

    /**
     * 将 AST 中的 {@link FunctionNode} 构建为可执行的 {@link IRFunction}。
     *
     * <p>构建过程各阶段如下：
     * <ol>
     *     <li><b>全局函数表登记</b>：记录函数名、返回类型、形参类型列表。</li>
     *     <li><b>初始化上下文</b>：创建 IRFunction 与 IRContext（作用域 + 虚拟寄存器池）。</li>
     *     <li><b>注入模块级全局变量寄存器</b>：确保所有函数共享全局变量的寄存器槽位。</li>
     *     <li><b>导入全局常量</b>：包括跨模块常量，使表达式可直接折叠。</li>
     *     <li><b>设置数字字面量默认后缀</b>：根据返回类型推断字面量类型（double→d、float→f、long→l）。</li>
     *     <li><b>构建形参</b>：
     *         <ul>
     *             <li>为每个形参分配一个虚拟寄存器；</li>
     *             <li>登记到当前作用域；</li>
     *             <li>注册到 IRFunction 的形参列表。</li>
     *         </ul>
     *     </li>
     *     <li><b>构建函数体</b>：使用 StatementBuilder 将每条语句转换为 IR 指令。</li>
     *     <li><b>清理默认后缀</b>：避免影响其它函数的字面量推断。</li>
     * </ol>
     *
     * @param functionNode 表示函数定义的 AST 节点
     * @return 构建完成的 IRFunction 对象
     */
    public IRFunction build(FunctionNode functionNode) {

        // 1. 在全局函数表登记函数签名（名称 + 返回类型 + 参数类型）
        List<String> paramTypes = functionNode.parameters() == null
                ? List.of()
                : functionNode.parameters()
                .stream()
                .map(ParameterNode::type)
                .toList();

        GlobalFunctionTable.register(
                functionNode.name(),
                functionNode.returnType(),
                paramTypes
        );

        // 2. 初始化 IRFunction 与 IR 构建上下文
        IRFunction irFunction = new IRFunction(functionNode.name());
        IRContext irContext = new IRContext(irFunction);

        // 0. 注入模块级全局变量的共享寄存器槽位
        //    这样不同函数在引用全局变量时可保持统一寄存器地址
        if (!moduleGlobals.isEmpty()) {
            for (GlobalVariable g : moduleGlobals) {
                irContext.getScope().declare(
                        g.simpleName(),  // 全局变量的短名（不含模块名前缀）
                        g.type(),        // 类型
                        g.register()     // 已分配的共享寄存器
                );
            }
        }

        // 3. 导入全局常量（包括跨模块 const）
        GlobalConstTable.all().forEach((k, v) ->
                irContext.getScope().importExternalConst(k, v)
        );

        // 4. 设置数字字面量默认后缀（double/float/long）
        String rt = functionNode.returnType();
        String rtLower = (rt == null || rt.trim().isEmpty())
                ? "void"
                : rt.trim().toLowerCase();

        char defaultSuffix = switch (rtLower) {
            case "double" -> 'd';
            case "float" -> 'f';
            case "long" -> 'l';
            default -> '\0';  // void 或其它类型不设置后缀
        };
        ExpressionUtils.setDefaultSuffix(defaultSuffix);

        try {
            // 5. 处理形参：
            //    - 为每个形参分配一个新的虚拟寄存器（从 IRContext 统一分配，保证作用域一致）
            //    - 将 (参数名, 类型, 寄存器) 声明到当前作用域
            //    - 将寄存器加入 IRFunction 的参数列表，便于后续调用/生成
            if (functionNode.parameters() != null) {
                for (ParameterNode p : functionNode.parameters()) {
                    IRVirtualRegister reg = irContext.newRegister();
                    irContext.getScope().declare(p.name(), p.type(), reg);
                    irFunction.addParameter(reg);
                }
            }

            // 6. 构建函数体（逐条 AST 语句 → IR 指令）
            StatementBuilder stmtBuilder = new StatementBuilder(irContext);
            for (StatementNode stmt : functionNode.body()) {
                stmtBuilder.build(stmt);
            }

        } finally {
            // 7. 清理数字字面量默认后缀设置
            ExpressionUtils.clearDefaultSuffix();
        }

        // 8. 返回构建成功的 IRFunction
        return irFunction;
    }
}