package org.jcnc.snow.compiler.ir.builder.core;

import org.jcnc.snow.common.NumberLiteralHelper;
import org.jcnc.snow.compiler.ir.builder.statement.FunctionBuilder;
import org.jcnc.snow.compiler.ir.common.GlobalConstTable;
import org.jcnc.snow.compiler.ir.common.GlobalFunctionTable;
import org.jcnc.snow.compiler.ir.common.GlobalVariableTable;
import org.jcnc.snow.compiler.ir.common.GlobalVariableTable.GlobalVariable;
import org.jcnc.snow.compiler.ir.core.IRFunction;
import org.jcnc.snow.compiler.ir.core.IRProgram;
import org.jcnc.snow.compiler.parser.ast.*;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.Node;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

import java.util.*;

/**
 * IRProgramBuilder 负责将 AST 的顶层结构（模块、函数、语句等）构建为可执行的 IRProgram。
 *
 * <p>核心流程包括：</p>
 * <ul>
 *   <li>预扫描所有模块，加载编译期常量至全局常量表，实现跨模块常量折叠。</li>
 *   <li>注册所有 struct 的字段布局，并处理继承关系，为成员访问与布局查询提供支持。</li>
 *   <li>预注册所有 struct 的构造函数及成员方法的签名，用于构建阶段的类型查询。</li>
 *   <li>将 struct 的方法降级为普通函数（重命名并注入隐式 this 参数）并加入 IR 程序。</li>
 *   <li>为模块内函数自动添加模块名前缀并按需注入全局声明。</li>
 *   <li>将顶层语句包装为特殊入口函数 "_start" 便于脚本式执行。</li>
 * </ul>
 *
 * <p>本类为无状态工具类（除全局注入追踪），外部不应持有其实例。</p>
 *
 * <p><b>注意：</b></p>
 * <ul>
 *   <li>禁止外部直接操作全局表，应通过本类的预处理流程完成。</li>
 *   <li>如遇无法处理的顶层节点类型，将抛出 IllegalStateException。</li>
 * </ul>
 */
public final class IRProgramBuilder {

    /**
     * 已经注入全局初始化的模块名集合，避免重复注入。
     */
    private final Set<String> injectedModuleGlobals = new HashSet<>();

    /**
     * 根据 AST 根节点列表构建 IRProgram。
     *
     * <p>
     * 主流程如下：
     * <ol>
     *     <li>预先登记全局常量，便于后续常量折叠。</li>
     *     <li>注册所有 struct 的字段布局。</li>
     *     <li>预注册 struct 构造/方法签名，支持同结构体方法间引用。</li>
     *     <li>依次遍历顶层节点，构建函数与特殊入口。</li>
     * </ol>
     * </p>
     *
     * @param roots AST 顶层节点列表，类型为 ModuleNode / FunctionNode / StatementNode
     * @return 构建完成的 IRProgram 对象
     * @throws IllegalStateException 若遇到不支持的顶层节点类型
     */
    public IRProgram buildProgram(List<Node> roots) {
        // 1. 预登记全局常量
        preloadGlobals(roots);
        // 2. 注册所有结构体字段布局
        preloadStructLayouts(roots);
        // 3. 预注册 struct 的构造与方法签名
        preloadStructCallables(roots);

        IRProgram irProgram = new IRProgram();
        // 4. 遍历所有顶层节点，分类型处理
        for (Node node : roots) {
            switch (node) {
                case ModuleNode moduleNode -> {
                    Collection<GlobalVariable> moduleGlobals =
                            GlobalVariableTable.ofModule(moduleNode.name());
                    boolean moduleHasEntry = moduleHasEntryFunction(moduleNode);
                    // 降级并注册本模块所有 struct 构造/方法
                    if (moduleNode.structs() != null) {
                        for (StructNode structNode : moduleNode.structs()) {
                            lowerAndRegisterStruct(structNode, irProgram, moduleNode.name(), moduleGlobals);
                        }
                    }
                    // 处理模块内的普通函数
                    if (moduleNode.functions() != null) {
                        for (FunctionNode f : moduleNode.functions()) {
                            irProgram.add(buildFunctionWithGlobals(moduleNode, f, moduleGlobals, moduleHasEntry));
                        }
                    }
                }
                case FunctionNode functionNode -> irProgram.add(buildFunction(functionNode, Collections.emptyList()));
                case StatementNode statementNode ->
                        irProgram.add(buildFunction(wrapTopLevel(statementNode), Collections.emptyList()));
                default -> throw new IllegalStateException("Unsupported top-level node: " + node);
            }
        }
        return irProgram;
    }

    /**
     * 注册每个结构体的字段布局，处理继承关系，将字段名映射到槽位下标。
     *
     * <p>规则说明：</p>
     * <ol>
     *   <li>如有父类，先复制父类布局和类型表，索引续接。</li>
     *   <li>子类字段与父类重名时跳过，防止覆盖。</li>
     *   <li>最终注册到全局布局表，并登记继承关系。</li>
     * </ol>
     *
     * @param roots AST 顶层节点列表
     */
    private void preloadStructLayouts(List<Node> roots) {
        for (Node n : roots) {
            if (!(n instanceof ModuleNode mod)) continue;
            if (mod.structs() == null) continue;

            for (StructNode s : mod.structs()) {
                List<DeclarationNode> fields = s.fields();
                Map<String, Integer> layout = new LinkedHashMap<>();
                Map<String, String> fieldTypes = new LinkedHashMap<>();
                int idx = 0;

                // 处理父类布局及字段类型
                String parentName = s.parent();
                if (parentName != null && !parentName.isBlank()) {
                    IRBuilderScope.registerStructParent(s.name(), parentName);

                    Map<String, Integer> parentLayout = IRBuilderScope.getStructLayout(parentName);
                    if (parentLayout != null && !parentLayout.isEmpty()) {
                        layout.putAll(parentLayout);
                        idx = parentLayout.size();
                        Map<String, String> parentTypes = IRBuilderScope.getStructFieldTypes(parentName);
                        if (parentTypes != null) {
                            fieldTypes.putAll(parentTypes);
                        }
                    }
                }

                // 注册子类字段，避免重名
                if (fields != null) {
                    for (DeclarationNode d : fields) {
                        String name = d.getName();
                        if (!layout.containsKey(name)) {
                            layout.put(name, idx++);
                        }
                        if (!fieldTypes.containsKey(name)) {
                            fieldTypes.put(name, d.getType());
                        }
                    }
                }

                IRBuilderScope.registerStructLayout(s.name(), layout);
                IRBuilderScope.registerStructFieldTypes(s.name(), fieldTypes);
            }
        }
    }

    /**
     * 预注册 struct 的所有构造函数与方法签名。
     * 便于在构建函数体时可直接查找其他方法返回值和参数信息。
     *
     * @param roots AST 顶层节点列表
     */
    private void preloadStructCallables(List<Node> roots) {
        for (Node n : roots) {
            if (!(n instanceof ModuleNode mod) || mod.structs() == null) continue;

            for (StructNode s : mod.structs()) {
                // 处理构造函数
                if (s.inits() != null) {
                    for (FunctionNode init : s.inits()) {
                        List<String> paramTypes = new ArrayList<>(init.parameters().size() + 1);
                        paramTypes.add(s.name());
                        init.parameters().forEach(p -> paramTypes.add(p.type()));
                        String loweredName = s.name() + ".__init__" + init.parameters().size();
                        GlobalFunctionTable.register(loweredName, init.returnType(), paramTypes);
                    }
                }
                // 处理普通方法
                if (s.methods() != null) {
                    for (FunctionNode m : s.methods()) {
                        List<String> paramTypes = new ArrayList<>(m.parameters().size() + 1);
                        paramTypes.add(s.name());
                        m.parameters().forEach(p -> paramTypes.add(p.type()));
                        int argc = m.parameters().size() + 1;
                        String loweredName = s.name() + "." + m.name() + "_" + argc;
                        GlobalFunctionTable.register(loweredName, m.returnType(), paramTypes);
                    }
                }
            }
        }
    }

    /**
     * 将 struct 的所有构造函数（inits）和方法（methods）降级为普通函数，并注册进 IRProgram。
     * <p>
     * 降级规则：
     * <ol>
     *     <li>函数名加结构体前缀，保证唯一。</li>
     *     <li>参数列表首位插入隐式 this:StructName 参数。</li>
     *     <li>方法名追加参数数后缀，防止重载冲突。</li>
     * </ol>
     *
     * @param structNode    当前结构体节点
     * @param out           注册到的 IRProgram
     * @param moduleName    模块名
     * @param moduleGlobals 模块全局变量集合
     */
    private void lowerAndRegisterStruct(StructNode structNode,
                                        IRProgram out,
                                        String moduleName,
                                        Collection<GlobalVariable> moduleGlobals) {
        String structName = structNode.name();

        // 处理构造函数
        if (structNode.inits() != null) {
            for (FunctionNode initFn : structNode.inits()) {
                String loweredName = structName + ".__init__" + initFn.parameters().size();
                FunctionNode loweredInit = lowerStructCallable(
                        initFn,
                        loweredName,
                        structName
                );
                out.add(buildFunction(loweredInit, moduleGlobals));
            }
        }

        // 处理普通方法
        if (structNode.methods() != null) {
            for (FunctionNode m : structNode.methods()) {
                int argCount = m.parameters().size() + 1; // +1 为 this
                String loweredName = structName + "." + m.name() + "_" + argCount;
                FunctionNode loweredMethod = lowerStructCallable(
                        m,
                        loweredName,
                        structName
                );
                out.add(buildFunction(loweredMethod, moduleGlobals));
            }
        }
    }

    /**
     * 生成带隐式 this:StructName 参数的函数节点副本，并重命名。
     *
     * @param original    原始 FunctionNode
     * @param loweredName 降级后函数名
     * @param structName  结构体名
     * @return 新的 FunctionNode 实例
     */
    private FunctionNode lowerStructCallable(FunctionNode original, String loweredName, String structName) {
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

    /**
     * 预扫描所有模块节点，将声明的 const 全局变量注册为编译期常量，形式为 "模块名.常量名"。
     * 支持跨模块常量折叠，便于后续 IR 优化。
     *
     * @param roots AST 顶层节点列表
     */
    private void preloadGlobals(List<Node> roots) {
        for (Node n : roots) {
            if (n instanceof ModuleNode mod) {
                String moduleName = mod.name();
                if (mod.globals() == null) continue;
                for (DeclarationNode decl : mod.globals()) {
                    // 只处理编译期常量
                    if (decl.isConst()) {
                        if (decl.getInitializer().isEmpty()) continue;
                        ExpressionNode init = decl.getInitializer().get();
                        Object value = evalLiteral(init);
                        if (value != null) {
                            GlobalConstTable.register(moduleName + "." + decl.getName(), value);
                        }
                    } else {
                        GlobalVariableTable.register(moduleName + "." + decl.getName(), decl.getType());
                    }
                }
            }
        }
    }

    /**
     * 字面量表达式计算工具。
     * 支持数值、字符串、布尔、简单负号表达式折叠，不支持复杂表达式。
     *
     * @param expr 表达式节点
     * @return Java 原生常量值，不支持则返回 null
     */
    private Object evalLiteral(ExpressionNode expr) {
        return switch (expr) {
            case NumberLiteralNode num -> {
                String raw = num.value();
                NumberLiteralHelper.NormalizedLiteral normalized = NumberLiteralHelper.normalize(raw, true);
                String digits = normalized.text();
                String core = normalized.digits();
                char suffix = NumberLiteralHelper.extractTypeSuffix(raw);
                try {
                    if (NumberLiteralHelper.looksLikeFloat(digits)) {
                        yield Double.parseDouble(digits);
                    }
                    long lv = NumberLiteralHelper.parseLongLiteral(core, normalized.radix());
                    yield switch (suffix) {
                        case 'b' -> (byte) lv;
                        case 's' -> (short) lv;
                        case 'l' -> lv;
                        default -> (int) lv;
                    };
                } catch (NumberFormatException ignore) {
                    yield null;
                }
            }
            case StringLiteralNode str -> str.value();
            case BoolLiteralNode b -> b.getValue() ? 1 : 0;
            case UnaryExpressionNode un -> {
                if ("-".equals(un.operator())) {
                    Object inner = evalLiteral(un.operand());
                    if (inner instanceof Integer i) yield -i;
                    if (inner instanceof Long l) yield -l;
                    if (inner instanceof Short s) yield (short) -s;
                    if (inner instanceof Byte by) yield (byte) -by;
                    if (inner instanceof Double d) yield -d;
                    if (inner instanceof Float f) yield -f;
                }
                yield null;
            }
            default -> null;
        };
    }

    /**
     * 构建带有全局声明注入的函数，函数名加模块前缀，保证唯一。
     * 如需注入全局声明，会优先过滤与参数同名的声明，防止遮蔽。
     *
     * @param moduleNode    所属模块节点
     * @param functionNode  函数节点
     * @param moduleGlobals 模块全局变量集合
     * @return 构建的 IRFunction
     */
    private IRFunction buildFunctionWithGlobals(ModuleNode moduleNode,
                                                FunctionNode functionNode,
                                                Collection<GlobalVariable> moduleGlobals,
                                                boolean moduleHasEntry) {
        String qualifiedName = moduleNode.name() + "." + functionNode.name();
        boolean injectGlobals = shouldInjectGlobals(moduleNode.name(), functionNode.name(), moduleHasEntry);

        List<StatementNode> newBody = functionNode.body();
        if (moduleNode.globals() != null && !moduleNode.globals().isEmpty()) {
            Set<String> paramNames = new HashSet<>();
            for (ParameterNode p : functionNode.parameters()) {
                paramNames.add(p.name());
            }
            List<StatementNode> constGlobals = new ArrayList<>();
            List<StatementNode> mutableGlobals = new ArrayList<>();
            for (DeclarationNode g : moduleNode.globals()) {
                if (paramNames.contains(g.getName())) continue;
                if (g.isConst()) constGlobals.add(g);
                else mutableGlobals.add(g);
            }

            List<StatementNode> prefix = new ArrayList<>();
            if (!constGlobals.isEmpty()) {
                prefix.addAll(constGlobals);
            }
            if (injectGlobals && !mutableGlobals.isEmpty()) {
                prefix.addAll(mutableGlobals);
            }

            if (!prefix.isEmpty()) {
                newBody = new ArrayList<>(prefix.size() + functionNode.body().size());
                newBody.addAll(prefix);
                newBody.addAll(functionNode.body());
            }
        }
        FunctionNode wrapped = new FunctionNode(
                qualifiedName,
                functionNode.parameters(),
                functionNode.returnType(),
                newBody,
                functionNode.context()
        );
        return buildFunction(wrapped, moduleGlobals);
    }

    /**
     * 创建一个新的函数节点，其内容与原函数完全一致，仅更改函数名。
     * 原节点的参数、返回类型、函数体及上下文信息均保持不变。
     *
     * @param fn      原始的 FunctionNode
     * @param newName 新函数名
     * @return 一个名称被替换后的新 FunctionNode
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
     * 判断某个函数在构建时是否应注入模块级全局变量声明。
     * <p>规则：</p>
     * <ul>
     *   <li>每个模块的全局变量只会被注入一次。</li>
     *   <li>若模块存在入口函数（main），则只对该入口函数注入。</li>
     *   <li>若模块不存在入口函数，则对出现的第一个函数注入。</li>
     * </ul>
     *
     * @param moduleName 模块名
     * @param fnName     函数名
     * @param moduleHasEntry 模块是否包含入口函数
     * @return 是否应对该函数注入全局变量
     */
    private boolean shouldInjectGlobals(String moduleName, String fnName, boolean moduleHasEntry) {
        if (moduleName == null || moduleName.isBlank()) return false;
        if (injectedModuleGlobals.contains(moduleName)) {
            return false;
        }
        boolean isEntry = isEntryFunction(fnName);
        if (moduleHasEntry) {
            if (isEntry) {
                injectedModuleGlobals.add(moduleName);
                return true;
            }
            return false;
        }
        // 模块没有入口函数，随第一个函数注入
        injectedModuleGlobals.add(moduleName);
        return true;
    }

    /**
     * 判断给定模块是否包含入口函数。
     * 入口函数定义为名为 "main" 或以 ".main" 结尾的函数。
     *
     * @param moduleNode 模块 AST 节点
     * @return 若存在入口函数则返回 true，否则返回 false
     */
    private static boolean moduleHasEntryFunction(ModuleNode moduleNode) {
        if (moduleNode == null || moduleNode.functions() == null) {
            return false;
        }
        for (FunctionNode fn : moduleNode.functions()) {
            if (isEntryFunction(fn.name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断指定名称是否代表入口函数。
     * 入口函数规则：名称为 "main"，或以 ".main" 结尾（模块前缀形式）。
     *
     * @param fnName 函数名
     * @return 是否为入口函数
     */
    private static boolean isEntryFunction(String fnName) {
        return "main".equals(fnName) || (fnName != null && fnName.endsWith(".main"));
    }

    /**
     * 构建 IRFunction 实例。
     * 直接委托 {@link FunctionBuilder#build(FunctionNode)}。
     *
     * @param functionNode  函数节点
     * @param moduleGlobals 模块全局变量集合
     * @return 构建的 IRFunction
     */
    private IRFunction buildFunction(FunctionNode functionNode, Collection<GlobalVariable> moduleGlobals) {
        return new FunctionBuilder(moduleGlobals).build(functionNode);
    }

    /**
     * 将顶层语句节点封装为 "_start" 函数，便于统一执行。
     *
     * @param stmt 顶层语句节点
     * @return 封装后的函数节点
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