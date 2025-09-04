package org.jcnc.snow.compiler.ir.builder.statement;

import org.jcnc.snow.compiler.ir.builder.core.IRContext;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionBuilder;
import org.jcnc.snow.compiler.ir.builder.statement.utils.ConstFolder;

import java.util.ArrayDeque;
import java.util.function.Consumer;

/**
 * 语句构建上下文。
 * <p>
 * 为所有语句处理器提供统一的构建环境，包括IR上下文、表达式构建器、
 * break/continue目标栈、递归语句构建入口等辅助工具和环境变量。
 * 通过该上下文，语句处理器可以方便访问和复用编译期间的各类状态与能力。
 * </p>
 */
public class StatementBuilderContext {

    /**
     * 当前IR上下文（保存符号表、指令流、标签等全局状态）
     */
    private final IRContext ctx;

    /**
     * 表达式构建器，用于语句内部表达式求值与IR生成
     */
    private final ExpressionBuilder expr;

    /**
     * break跳转目标栈，用于支持多层嵌套循环/块中的break语义
     */
    private final ArrayDeque<String> breakTargets;

    /**
     * continue跳转目标栈，用于支持多层嵌套循环/块中的continue语义
     */
    private final ArrayDeque<String> continueTargets;

    /**
     * 单条语句递归构建回调（由StatementBuilder注入）
     */
    private final Consumer<org.jcnc.snow.compiler.parser.ast.base.StatementNode> buildOne;

    /**
     * 批量语句递归构建回调（由StatementBuilder注入）
     */
    private final Consumer<Iterable<org.jcnc.snow.compiler.parser.ast.base.StatementNode>> buildMany;

    /**
     * 常量折叠工具（用于IR层常量传播与优化）
     */
    private final ConstFolder constFolder;

    /**
     * 构造新的语句构建上下文。
     *
     * @param ctx             IR上下文对象，保存全局编译环境
     * @param expr            表达式构建器，供语句处理器使用
     * @param breakTargets    break目标栈，支持嵌套控制流
     * @param continueTargets continue目标栈，支持嵌套控制流
     * @param buildOne        单条语句递归构建回调
     * @param buildMany       多条语句批量递归构建回调
     */
    public StatementBuilderContext(
            IRContext ctx,
            ExpressionBuilder expr,
            ArrayDeque<String> breakTargets,
            ArrayDeque<String> continueTargets,
            Consumer<org.jcnc.snow.compiler.parser.ast.base.StatementNode> buildOne,
            Consumer<Iterable<org.jcnc.snow.compiler.parser.ast.base.StatementNode>> buildMany
    ) {
        this.ctx = ctx;
        this.expr = expr;
        this.breakTargets = breakTargets;
        this.continueTargets = continueTargets;
        this.buildOne = buildOne;
        this.buildMany = buildMany;
        this.constFolder = new ConstFolder(this);
    }

    /**
     * 获取当前IR上下文。
     *
     * @return 全局IRContext对象
     */
    public IRContext ctx() {
        return ctx;
    }

    /**
     * 获取表达式构建器。
     *
     * @return ExpressionBuilder对象
     */
    public ExpressionBuilder expr() {
        return expr;
    }

    /**
     * 获取break目标栈（用于循环/块内break语义）。
     *
     * @return break目标栈
     */
    public ArrayDeque<String> breakTargets() {
        return breakTargets;
    }

    /**
     * 获取continue目标栈（用于循环/块内continue语义）。
     *
     * @return continue目标栈
     */
    public ArrayDeque<String> continueTargets() {
        return continueTargets;
    }

    /**
     * 递归构建单条语句（委托到顶层StatementBuilder）。
     *
     * @param s AST语句节点
     */
    public void build(org.jcnc.snow.compiler.parser.ast.base.StatementNode s) {
        buildOne.accept(s);
    }

    /**
     * 递归批量构建语句列表（委托到顶层StatementBuilder）。
     *
     * @param list AST语句节点集合
     */
    public void buildAll(Iterable<org.jcnc.snow.compiler.parser.ast.base.StatementNode> list) {
        buildMany.accept(list);
    }

    /**
     * 获取常量折叠工具，用于IR层常量传播和优化。
     *
     * @return ConstFolder对象
     */
    public ConstFolder constFolder() {
        return constFolder;
    }
}
