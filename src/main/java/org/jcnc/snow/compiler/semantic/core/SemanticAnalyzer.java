package org.jcnc.snow.compiler.semantic.core;

import org.jcnc.snow.compiler.parser.ast.ModuleNode;
import org.jcnc.snow.compiler.semantic.analyzers.AnalyzerRegistry;
import org.jcnc.snow.compiler.semantic.error.SemanticError;

import java.util.*;

/**
 * {@code SemanticAnalyzer} 是编译器语义分析阶段的顶层调度器。
 * <p>
 * 它负责统一协调模块注册、函数签名登记和函数体语义检查等子任务，构建并维护语义上下文 {@link Context}，
 * 并最终输出所有收集到的语义错误列表 {@link SemanticError}。
 * <p>
 * 语义分析流程分为三个阶段：
 * <ol>
 *   <li>模块注册：将所有用户模块的名称添加至全局模块表中，供后续导入检查与引用；</li>
 *   <li>函数签名注册：提取函数定义的签名（名称与类型），填入每个模块对应的 {@link ModuleInfo}；</li>
 *   <li>函数体检查：遍历每个函数体，对所有语句与表达式执行类型检查和语义验证。</li>
 * </ol>
 * <p>
 * 内部使用组件：
 * <ul>
 *   <li>{@link ModuleRegistry}：注册用户模块；</li>
 *   <li>{@link SignatureRegistrar}：提取函数签名；</li>
 *   <li>{@link FunctionChecker}：分析函数体内语句；</li>
 *   <li>{@link BuiltinTypeRegistry}：初始化内置模块和类型；</li>
 *   <li>{@link AnalyzerRegistrar}：注册语句和表达式分析器。</li>
 * </ul>
 */
public class SemanticAnalyzer {

    /** 全局语义分析上下文，包含模块信息、错误记录、分析器注册表等 */
    private final Context ctx;

    /** 分析器注册表，管理语法节点与分析器之间的映射关系 */
    private final AnalyzerRegistry registry = new AnalyzerRegistry();

    // 分析流程中用到的核心子组件
    private final ModuleRegistry moduleRegistry;
    private final SignatureRegistrar signatureRegistrar;
    private final FunctionChecker functionChecker;

    /**
     * 构造语义分析器并完成初始配置。
     *
     * @param verbose 是否启用日志输出
     */
    public SemanticAnalyzer(boolean verbose) {
        this.ctx = new Context(new HashMap<>(), new ArrayList<>(), verbose, registry);

        // 初始化内置模块及分析器注册表
        BuiltinTypeRegistry.init(ctx);
        AnalyzerRegistrar.registerAll(registry);

        // 构造核心组件
        this.moduleRegistry = new ModuleRegistry(ctx);
        this.signatureRegistrar = new SignatureRegistrar(ctx);
        this.functionChecker = new FunctionChecker(ctx);
    }

    /**
     * 执行完整语义分析流程。
     * <p>
     * 输入为用户的模块语法树集合，输出为分析阶段产生的语义错误列表。
     *
     * @param modules 所有用户模块（语法树）
     * @return 所有语义错误的列表，若分析无误则为空
     */
    public List<SemanticError> analyze(List<ModuleNode> modules) {
        ctx.log("开始语义分析");

        moduleRegistry.registerUserModules(modules);  // 注册模块名
        signatureRegistrar.register(modules);         // 提取函数签名
        functionChecker.check(modules);               // 分析函数体

        ctx.log("分析完成，错误总数: " + ctx.errors().size());
        return ctx.errors();
    }
}
