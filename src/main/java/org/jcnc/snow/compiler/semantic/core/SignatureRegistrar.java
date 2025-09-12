package org.jcnc.snow.compiler.semantic.core;

import org.jcnc.snow.compiler.parser.ast.*;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.FunctionType;
import org.jcnc.snow.compiler.semantic.type.StructType;
import org.jcnc.snow.compiler.semantic.type.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@code SignatureRegistrar} 是语义分析前置阶段的注册器。
 * <p>
 * 它负责函数签名登记、结构体签名登记以及 import 模块引用校验，
 * 为后续的类型推断和语义分析建立完整的类型环境。
 *
 * <p>本类通过多阶段处理逻辑，确保跨模块引用和继承在类型推断时可用：
 * <ol>
 *   <li>校验模块的 import 是否存在，并预注册结构体的名称占位符。</li>
 *   <li>解析结构体字段、构造函数和方法的签名信息。</li>
 *   <li>处理结构体继承关系，并继承字段与方法。</li>
 *   <li>注册模块级函数签名。</li>
 * </ol>
 * <p>错误处理策略：
 * <ul>
 *   <li>若引用未知模块、类型或父类，均会记录为 {@link SemanticError} 错误。</li>
 *   <li>若字段、参数、返回类型无法解析，将默认降级为 {@code int} 或 {@code void} 类型以保证流程健壮性。</li>
 * </ul>
 *
 * @param ctx 语义分析上下文，记录所有模块、类型信息与错误列表
 */
public record SignatureRegistrar(Context ctx) {

    /**
     * 注册传入的所有模块的类型签名信息，包括 import 检查、结构体与函数签名建立。
     * <p>
     * 采用三阶段设计确保跨模块结构体引用/继承不会出错。
     *
     * @param modules 所有模块 AST 节点列表
     */
    public void register(Iterable<ModuleNode> modules) {
        // 第一阶段：验证模块 imports 与预注册结构体名称
        for (ModuleNode mod : modules) {
            ctx.setCurrentModule(mod.name());
            ModuleInfo mi = ctx.modules().get(mod.name());

            // 检查 import 的模块是否存在
            for (ImportNode imp : mod.imports()) {
                if (!ctx.modules().containsKey(imp.moduleName())) {
                    ctx.errors().add(new SemanticError(imp, "未知模块: " + imp.moduleName()));
                } else {
                    mi.getImports().add(imp.moduleName());
                }
            }

            // 为每个 struct 创建占位 StructType
            for (StructNode stn : mod.structs()) {
                if (!mi.getStructs().containsKey(stn.name())) {
                    mi.getStructs().put(stn.name(), new StructType(mod.name(), stn.name()));
                }
            }
        }

        // 第二阶段：解析 struct 字段、构造函数与方法签名
        for (ModuleNode mod : modules) {
            ctx.setCurrentModule(mod.name());
            ModuleInfo mi = ctx.modules().get(mod.name());

            for (StructNode stn : mod.structs()) {
                StructType st = mi.getStructs().get(stn.name());

                // 2.0 解析字段类型
                if (stn.fields() != null) {
                    for (DeclarationNode field : stn.fields()) {
                        Type ft = ctx.parseType(field.getType());
                        if (ft == null) {
                            ctx.errors().add(new SemanticError(field, "未知类型: " + field.getType()));
                            ft = BuiltinType.INT;
                        }
                        st.getFields().put(field.getName(), ft);
                    }
                }

                // 2.1 构造函数（支持重载）
                if (stn.inits() != null) {
                    for (FunctionNode initFn : stn.inits()) {
                        List<Type> ptypes = new ArrayList<>();
                        for (ParameterNode p : initFn.parameters()) {
                            Type t = ctx.parseType(p.type());
                            if (t == null) {
                                ctx.errors().add(new SemanticError(p, "未知类型: " + p.type()));
                                t = BuiltinType.INT;
                            }
                            ptypes.add(t);
                        }
                        st.addConstructor(new FunctionType(ptypes, BuiltinType.VOID));
                    }
                }

                // 2.2 方法（支持重载）
                if (stn.methods() != null) {
                    for (FunctionNode fn : stn.methods()) {
                        List<Type> ptypes = new ArrayList<>();
                        for (ParameterNode p : fn.parameters()) {
                            Type t = ctx.parseType(p.type());
                            if (t == null) {
                                ctx.errors().add(new SemanticError(p, "未知类型: " + p.type()));
                                t = BuiltinType.INT;
                            }
                            ptypes.add(t);
                        }
                        Type ret = Optional.ofNullable(ctx.parseType(fn.returnType()))
                                .orElse(BuiltinType.VOID);
                        st.addMethod(fn.name(), new FunctionType(ptypes, ret));
                    }
                }
            }
        }

        // 第三阶段：解析并建立 struct 的继承关系
        for (ModuleNode mod : modules) {
            ctx.setCurrentModule(mod.name());
            ModuleInfo mi = ctx.modules().get(mod.name());

            for (StructNode stn : mod.structs()) {
                if (stn.parent() == null) continue;
                StructType child = mi.getStructs().get(stn.name());
                StructType parent = resolveParentStruct(mi, stn.parent());

                if (parent == null) {
                    ctx.errors().add(new SemanticError(stn, "未知父类: " + stn.parent()));
                    continue;
                }

                // 建立继承
                child.setParent(parent);

                // 继承字段
                parent.getFields().forEach((k, v) -> child.getFields().putIfAbsent(k, v));

                // 继承方法（逐个重载）
                parent.getMethodOverloads().forEach((name, byArity) -> {
                    byArity.forEach((argc, ft) -> {
                        child.getMethodOverloads()
                                .computeIfAbsent(name, _k -> new java.util.HashMap<>())
                                .putIfAbsent(argc, ft);
                        child.getMethods().putIfAbsent(name, ft);
                    });
                });
            }
        }

        // 第四阶段：模块顶层函数
        for (ModuleNode mod : modules) {
            ctx.setCurrentModule(mod.name());
            ModuleInfo mi = ctx.modules().get(mod.name());

            for (FunctionNode fn : mod.functions()) {
                List<Type> params = new ArrayList<>();
                for (ParameterNode p : fn.parameters()) {
                    Type t = ctx.parseType(p.type());
                    if (t == null) {
                        ctx.errors().add(new SemanticError(p, "未知类型: " + p.type()));
                        t = BuiltinType.INT;
                    }
                    params.add(t);
                }
                Type ret = Optional.ofNullable(ctx.parseType(fn.returnType()))
                        .orElse(BuiltinType.VOID);
                mi.getFunctions().put(fn.name(), new FunctionType(params, ret));
            }
        }
    }

    /**
     * 解析父类结构体类型。
     * <p>支持限定名形式（如 {@code Module.Struct}），
     * 也支持在当前模块或导入模块中查找非限定名结构体。
     *
     * @param mi         当前模块信息
     * @param parentName 父类名称（可以是限定名或非限定名）
     * @return 对应的 StructType，找不到则返回 null
     */
    private StructType resolveParentStruct(ModuleInfo mi, String parentName) {
        // 限定名 Module.Struct
        int dot = parentName.indexOf('.');
        if (dot > 0 && dot < parentName.length() - 1) {
            String m = parentName.substring(0, dot);
            String s = parentName.substring(dot + 1);
            ModuleInfo pm = ctx.modules().get(m);
            if (pm != null) {
                return pm.getStructs().get(s);
            }
            return null;
        }

        // 当前模块中查找
        StructType local = mi.getStructs().get(parentName);
        if (local != null) return local;

        // 导入模块中查找
        for (String imported : mi.getImports()) {
            ModuleInfo pim = ctx.modules().get(imported);
            if (pim == null) continue;
            StructType st = pim.getStructs().get(parentName);
            if (st != null) return st;
        }

        // 均找不到
        return null;
    }
}
