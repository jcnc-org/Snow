package org.jcnc.snow.compiler.ir.builder;

import org.jcnc.snow.compiler.ir.common.GlobalConstTable;
import org.jcnc.snow.compiler.ir.core.IRFunction;
import org.jcnc.snow.compiler.ir.core.IRProgram;
import org.jcnc.snow.compiler.parser.ast.*;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.Node;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

import java.util.*;

/**
 * IRProgramBuilder 负责将 AST 顶层节点（如模块、函数、语句等）转换为可执行的 {@link IRProgram}。
 *
 * <p>
 * 主要工作内容包括：
 * </p>
 * <ul>
 *   <li>预扫描所有模块，将 <code>declare const</code> 常量登记到全局常量表，支持跨模块常量折叠。</li>
 *   <li>预扫描并注册所有 struct 的字段布局（字段→下标），供 IR 阶段对象字段读写使用。</li>
 *   <li>对模块内的普通函数加上模块前缀（ModuleName.func），保证命名唯一，并将本模块全局声明注入到函数体前部。</li>
 *   <li>将 struct 的 init 与 methods 降级为普通 IR 函数并注册为 <code>StructName.__init__</code>、<code>StructName.method</code>；同时在其参数列表首位插入隐式 <code>this: StructName</code>。</li>
 *   <li>将独立顶层语句自动包装为特殊的 "_start" 函数（脚本模式支持）。</li>
 * </ul>
 *
 * <p>
 * 该类为不可变工具类，仅包含静态行为，不持有状态。
 * </p>
 */
public final class IRProgramBuilder {

    /**
     * 将解析生成的 AST 根节点列表转换为 IRProgram。
     *
     * @param roots 含 ModuleNode、FunctionNode 或 StatementNode 的顶层 AST 根节点列表
     * @return 包含所有转换后 IRFunction 的 IRProgram 对象
     * @throws IllegalStateException 遇到不支持的顶层节点类型时抛出
     *
     *                               <p>
     *                               主流程为：
     *                               <ol>
     *                                   <li>首先登记全局常量（便于后续常量折叠优化）。</li>
     *                                   <li>再注册所有 struct 的字段布局（为成员读写和 this.xx 做准备）。</li>
     *                                   <li>随后依次遍历各顶层节点，根据类型进行分发处理：</li>
     *                                   <ul>
     *                                       <li>ModuleNode: 先降级并注册 struct 方法/构造，再处理普通函数。</li>
     *                                       <li>FunctionNode: 直接构建为 IRFunction。</li>
     *                                       <li>StatementNode: 作为脚本模式顶层语句包装进 "_start" 函数。</li>
     *                                       <li>其他类型: 抛出异常。</li>
     *                                   </ul>
     *                               </ol>
     *                               </p>
     */
    public IRProgram buildProgram(List<Node> roots) {
        // 1. 先登记全局常量，便于后续常量折叠
        preloadGlobals(roots);
        // 2. 注册所有结构体的字段布局（为成员访问做准备）
        preloadStructLayouts(roots);

        // 创建 IR 程序对象
        IRProgram irProgram = new IRProgram();
        // 3. 遍历并处理所有顶层节点
        for (Node node : roots) {
            switch (node) {
                case ModuleNode moduleNode -> {
                    // 3.1 先降级并注册本模块所有 struct 的构造/方法（struct 方法降级）
                    if (moduleNode.structs() != null) {
                        for (StructNode structNode : moduleNode.structs()) {
                            lowerAndRegisterStruct(structNode, irProgram);
                        }
                    }
                    // 3.2 再处理模块里的普通函数，模块内函数名全限定，注入全局声明
                    if (moduleNode.functions() != null) {
                        for (FunctionNode f : moduleNode.functions()) {
                            irProgram.add(buildFunctionWithGlobals(moduleNode, f));
                        }
                    }
                }
                case FunctionNode functionNode ->
                    // 3.3 处理顶层函数节点：直接构建为 IRFunction 并加入
                        irProgram.add(buildFunction(functionNode));
                case StatementNode statementNode ->
                    // 3.4 处理脚本式顶层语句：封装成 "_start" 函数后构建并添加
                        irProgram.add(buildFunction(wrapTopLevel(statementNode)));
                default ->
                    // 3.5 遇到未知类型节点，抛出异常
                        throw new IllegalStateException("Unsupported top-level node: " + node);
            }
        }
        return irProgram;
    }

    // ===================== 预扫描：注册结构体字段布局 =====================

    /**
     * 遍历所有模块与结构体，按字段声明顺序构建“字段名 -> 槽位索引”的映射，
     * 并注册到 {@link IRBuilderScope#registerStructLayout(String, Map)} 中。
     * 这样在构建任意函数/方法时，成员访问（如 this.name）就能查询到正确的字段索引。
     *
     * @param roots AST 顶层节点列表，包含模块/结构体信息
     */
    private void preloadStructLayouts(List<Node> roots) {
        for (Node n : roots) {
            // 只处理模块节点
            if (!(n instanceof ModuleNode mod)) continue;
            if (mod.structs() == null) continue;

            // 遍历每个 struct，收集字段布局
            for (StructNode s : mod.structs()) {
                List<DeclarationNode> fields = s.fields();
                if (fields == null) continue;
                Map<String, Integer> layout = new LinkedHashMap<>();
                int idx = 0;
                for (DeclarationNode d : fields) {
                    // 字段声明顺序决定槽位顺序（如0,1,2,...）
                    layout.put(d.getName(), idx++);
                }
                // 注册字段布局。结构体名通常为简单名（如 Person），类型系统如支持 Module.Struct 在 IRBuilderScope 里兼容处理
                IRBuilderScope.registerStructLayout(s.name(), layout);
            }
        }
    }

    // ===================== Struct 降级：方法/构造 → 普通函数 =====================

    /**
     * 将一个 Struct 的构造函数（init）和方法（methods）降级为普通 Function，并注册进 IRProgram：
     * <ul>
     *     <li>构造函数：StructName.__init__(this:StructName, ...)</li>
     *     <li>方法：StructName.method(this:StructName, ...)</li>
     * </ul>
     * <p>降级规则：</p>
     * <ol>
     *     <li>构造/方法函数名前缀加上结构体名（便于唯一定位）。</li>
     *     <li>参数列表最前添加隐式 this:StructName 参数。</li>
     * </ol>
     *
     * @param structNode 当前结构体节点
     * @param out        注册到的 IRProgram
     */
    private void lowerAndRegisterStruct(StructNode structNode, IRProgram out) {
        String structName = structNode.name();

        // 1) 降级处理构造函数（如有）
        if (structNode.init() != null) {
            FunctionNode loweredInit = lowerStructCallable(
                    structNode.init(),
                    structName + ".__init__",
                    structName
            );
            out.add(buildFunction(loweredInit));
        }

        // 2) 降级处理所有普通方法
        if (structNode.methods() != null) {
            for (FunctionNode m : structNode.methods()) {
                FunctionNode loweredMethod = lowerStructCallable(
                        m,
                        structName + "." + m.name(),
                        structName
                );
                out.add(buildFunction(loweredMethod));
            }
        }
    }

    /**
     * 生成一个带隐式 this:StructName 参数的函数节点副本，并重命名为 loweredName。
     *
     * @param original    原始 FunctionNode
     * @param loweredName 降级后的新函数名（如 StructName.method）
     * @param structName  结构体名，用于 this 参数类型
     * @return 重新包装后的函数节点
     */
    private FunctionNode lowerStructCallable(FunctionNode original, String loweredName, String structName) {
        // 在参数列表首位插入隐式 this 参数（类型为结构体名）
        List<ParameterNode> newParams = new ArrayList<>(original.parameters().size() + 1);
        newParams.add(new ParameterNode("this", structName, original.context()));
        newParams.addAll(original.parameters());

        return new FunctionNode(
                loweredName,
                newParams,
                original.returnType(),
                original.body(),
                original.context()
        );
    }

    // ===================== 全局常量收集 =====================

    /**
     * 扫描所有模块节点，将其中声明的 const 全局变量（即编译期常量）
     * 以 "模块名.常量名" 形式注册到全局常量表。
     * <p>
     * 支持跨模块常量折叠，便于 IR 生成时做常量传播与优化。
     * </p>
     *
     * @param roots AST 顶层节点列表
     */
    private void preloadGlobals(List<Node> roots) {
        for (Node n : roots) {
            if (n instanceof ModuleNode mod) {
                String moduleName = mod.name();
                if (mod.globals() == null) continue;
                for (DeclarationNode decl : mod.globals()) {
                    // 只处理带初始值的 const 常量（编译期常量），忽略 run-time/无初始值
                    if (!decl.isConst() || decl.getInitializer().isEmpty()) continue;
                    ExpressionNode init = decl.getInitializer().get();
                    Object value = evalLiteral(init);
                    if (value != null) {
                        GlobalConstTable.register(moduleName + "." + decl.getName(), value);
                    }
                }
            }
        }
    }

    /**
     * 字面量提取与类型折叠工具。
     * <p>
     * 用于将表达式节点还原为 Java 原生类型（int、long、double、String等），仅支持直接字面量。
     * 不支持复杂表达式、非常量等情况，无法静态折叠则返回 null。
     *
     * @param expr 要计算的表达式节点（要求是字面量）
     * @return 提取到的原生常量值；若不支持则返回 null
     */
    private Object evalLiteral(ExpressionNode expr) {
        return switch (expr) {
            case NumberLiteralNode num -> {
                // 数字字面量：支持下划线、类型后缀（如 123_456L）
                String raw = num.value();
                String s = raw.replace("_", "");
                char last = Character.toLowerCase(s.charAt(s.length() - 1));
                // 处理类型后缀：如 123l/123d/123f
                String core = switch (last) {
                    case 'b', 's', 'l', 'f', 'd' -> s.substring(0, s.length() - 1);
                    default -> s;
                };
                try {
                    // 支持浮点数、科学计数法
                    if (core.contains(".") || core.contains("e") || core.contains("E")) {
                        yield Double.parseDouble(core);
                    }
                    long lv = Long.parseLong(core);
                    yield switch (last) {
                        case 'b' -> (byte) lv;
                        case 's' -> (short) lv;
                        case 'l' -> lv;
                        default -> (int) lv;
                    };
                } catch (NumberFormatException ignore) {
                    yield null;
                }
            }
            case StringLiteralNode str -> str.value(); // 字符串字面量直接返回
            case BoolLiteralNode b -> b.getValue() ? 1 : 0; // 布尔常量转为 1/0
            default -> null; // 其他情况不支持常量折叠
        };
    }

    // ===================== IRFunction 构建辅助 =====================

    /**
     * 构建带有模块全局声明“注入”的函数，并将函数名加上模块前缀，保证模块内函数名唯一。
     * <p>
     * 如果模块有全局声明，则这些声明会被插入到函数体前部（<b>会过滤掉与参数同名的全局声明</b>，防止变量遮蔽）。
     * </p>
     *
     * @param moduleNode   所属模块节点
     * @param functionNode 待构建的函数节点
     * @return 包含全局声明的 IRFunction
     */
    private IRFunction buildFunctionWithGlobals(ModuleNode moduleNode, FunctionNode functionNode) {
        // 1. 拼接模块名和函数名，生成全限定名
        String qualifiedName = moduleNode.name() + "." + functionNode.name();
        // 2. 若无全局声明，直接重命名构建
        if (moduleNode.globals() == null || moduleNode.globals().isEmpty()) {
            return buildFunction(renameFunction(functionNode, qualifiedName));
        }

        // 3. 过滤掉与参数重名的全局声明（优先参数作用域，避免变量遮蔽）
        Set<String> paramNames = new HashSet<>();
        for (ParameterNode p : functionNode.parameters()) {
            paramNames.add(p.name());
        }
        List<StatementNode> filteredGlobals = new ArrayList<>();
        for (DeclarationNode g : moduleNode.globals()) {
            // 避免全局声明和参数重名，优先参数
            if (!paramNames.contains(g.getName())) {
                filteredGlobals.add(g);
            }
        }

        // 4. 若无可插入的全局声明，直接重命名构建
        if (filteredGlobals.isEmpty()) {
            return buildFunction(renameFunction(functionNode, qualifiedName));
        }

        // 5. 合并全局声明与函数体，前插全局声明
        List<StatementNode> newBody = new ArrayList<>(filteredGlobals.size() + functionNode.body().size());
        newBody.addAll(filteredGlobals);
        newBody.addAll(functionNode.body());
        FunctionNode wrapped = new FunctionNode(
                qualifiedName,
                functionNode.parameters(),
                functionNode.returnType(),
                newBody,
                functionNode.context()
        );
        return buildFunction(wrapped);
    }

    /**
     * 生成一个重命名的 FunctionNode（只修改函数名，其他属性保持不变）。
     *
     * @param fn      原始函数节点
     * @param newName 新的函数名（全限定名）
     * @return 重命名后的函数节点
     */
    private FunctionNode renameFunction(FunctionNode fn, String newName) {
        return new FunctionNode(
                newName,
                fn.parameters(),
                fn.returnType(),
                fn.body(),
                fn.context()
        );
    }

    /**
     * 构建 IRFunction。
     *
     * @param functionNode 待构建的 FunctionNode
     * @return 构建后的 IRFunction
     *
     * <p>本方法仅作中转，直接委托给 FunctionBuilder。</p>
     */
    private IRFunction buildFunction(FunctionNode functionNode) {
        return new FunctionBuilder().build(functionNode);
    }

    /**
     * 将顶层语句节点封装成特殊的 "_start" 函数。
     * 主要用于脚本模式支持，使得顶层语句也可以被 IR 执行引擎统一处理。
     *
     * @param stmt 顶层语句节点
     * @return 封装后的 FunctionNode
     */
    private FunctionNode wrapTopLevel(StatementNode stmt) {
        return new FunctionNode(
                "_start",
                List.of(),
                "void",
                List.of(stmt),
                new NodeContext(-1, -1, "")
        );
    }
}
