package org.jcnc.snow.compiler.ir.builder.expression;

import org.jcnc.snow.compiler.ir.builder.core.IRContext;
import org.jcnc.snow.compiler.ir.builder.handlers.*;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;

import java.util.HashMap;
import java.util.Map;

/**
 * 表达式构建器主入口。
 * <p>
 * 根据表达式 AST 节点的具体类型，分派到对应的 ExpressionHandler 处理器，实现表达式到 IR 层的转换。
 * 支持注册不同类型的表达式处理器，便于扩展和维护。
 */
public class ExpressionBuilder {
    /**
     * 当前 IR 构建上下文，贯穿整个编译期 IR 生成过程
     */
    private final IRContext ctx;

    /**
     * ExpressionHandler 注册表。
     * key: ExpressionNode 子类类型，
     * value: 对应的表达式处理器。
     * <p>
     * 使用泛型约束，避免原始类型，保证类型安全。
     */
    private final Map<Class<? extends ExpressionNode>, ExpressionHandler<? extends ExpressionNode>> handlers = new HashMap<>();

    /**
     * 构造函数，注入 IRContext。
     *
     * @param ctx IR 构建上下文对象
     */
    public ExpressionBuilder(IRContext ctx) {
        this.ctx = ctx;
        registerHandlers();
    }

    /**
     * 获取当前 IRContext 上下文。
     *
     * @return IRContext 对象
     */
    public IRContext ctx() {
        return ctx;
    }

    /**
     * 注册所有内置的表达式处理器。
     * 如需扩展新的表达式类型，只需在此处添加即可。
     */
    private void registerHandlers() {
        handlers.put(org.jcnc.snow.compiler.parser.ast.NumberLiteralNode.class, new NumberLiteralHandler());
        handlers.put(org.jcnc.snow.compiler.parser.ast.StringLiteralNode.class, new StringLiteralHandler());
        handlers.put(org.jcnc.snow.compiler.parser.ast.BoolLiteralNode.class, new BoolLiteralHandler());
        handlers.put(org.jcnc.snow.compiler.parser.ast.IdentifierNode.class, new IdentifierHandler());
        handlers.put(org.jcnc.snow.compiler.parser.ast.MemberExpressionNode.class, new MemberHandler());
        handlers.put(org.jcnc.snow.compiler.parser.ast.BinaryExpressionNode.class, new BinaryHandler());
        handlers.put(org.jcnc.snow.compiler.parser.ast.CallExpressionNode.class, new CallHandler());
        handlers.put(org.jcnc.snow.compiler.parser.ast.UnaryExpressionNode.class, new UnaryHandler());
        handlers.put(org.jcnc.snow.compiler.parser.ast.IndexExpressionNode.class, new IndexHandler());
        handlers.put(org.jcnc.snow.compiler.parser.ast.ArrayLiteralNode.class, new ArrayLiteralHandler());
        handlers.put(org.jcnc.snow.compiler.parser.ast.NewExpressionNode.class, new NewHandler());
    }

    /**
     * 根据表达式类型分派到对应处理器进行构建，返回结果寄存器（IRVirtualRegister）。
     *
     * @param expr 需要构建的表达式 AST 节点
     * @return 存放表达式结果的虚拟寄存器
     * @throws IllegalStateException 若表达式类型未注册对应处理器，抛出异常
     */
    @SuppressWarnings("unchecked")
    public IRVirtualRegister build(ExpressionNode expr) {
        // 类型安全地获取对应 Handler，并进行类型转换
        ExpressionHandler<ExpressionNode> handler =
                (ExpressionHandler<ExpressionNode>) handlers.get(expr.getClass());
        if (handler == null) {
            throw new IllegalStateException("不支持的表达式类型: " + expr.getClass().getSimpleName());
        }
        return handler.handle(this, expr);
    }

    /**
     * 将表达式构建并写入指定的虚拟寄存器（用于支持目标寄存器复用等优化）。
     *
     * @param expr 需要构建的表达式 AST 节点
     * @param dest 目标虚拟寄存器（存放结果）
     * @throws IllegalStateException 若表达式类型未注册对应处理器，抛出异常
     */
    @SuppressWarnings("unchecked")
    public void buildInto(ExpressionNode expr, IRVirtualRegister dest) {
        // 类型安全地获取对应 Handler，并进行类型转换
        ExpressionHandler<ExpressionNode> handler =
                (ExpressionHandler<ExpressionNode>) handlers.get(expr.getClass());
        if (handler == null) {
            throw new IllegalStateException("不支持的表达式类型: " + expr.getClass().getSimpleName());
        }
        handler.handleInto(this, expr, dest);
    }
}
