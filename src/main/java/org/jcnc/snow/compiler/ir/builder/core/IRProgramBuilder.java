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
 * {@code IRProgramBuilder} 负责将 AST 顶层节点（如模块、函数、语句等）
 * 构建并转换为可执行的 {@link IRProgram}。
 * <p>
 * 主要职责包括：
 * <ul>
 *   <li>扫描所有模块，将 <code>declare const</code> 编译期常量注册到全局常量表，实现跨模块常量折叠。</li>
 *   <li>注册所有 struct 的字段布局，为成员访问提供支持，兼容继承。</li>
 *   <li>为 struct 的 init 方法和成员方法降级为普通函数，并注册为唯一函数名。</li>
 *   <li>为模块内函数名添加前缀，保证命名唯一，并按需注入全局声明。</li>
 *   <li>将独立顶层语句封装为特殊 "_start" 函数，支持脚本式语法入口。</li>
 * </ul>
 * <p>
 * 本类为工具类，所有行为静态无状态（除注入追踪），禁止外部持有状态。
 * </p>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *   <li>请勿直接修改全局表，均通过本类预处理方法。</li>
 *   <li>异常处理严格，遇到不支持节点类型会抛出 {@link IllegalStateException}。</li>
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
     * 重命名函数节点，只修改名称，其他属性保持不变。
     *
     * @param fn      原始函数节点
     * @param newName 新函数名
     * @return 新函数节点
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
     * 判断是否需要为模块注入全局初始化声明（每模块仅注入一次，优先 main）。
     *
     * @param moduleName 模块名
     * @param fnName     函数名
     * @return 是否需要注入
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