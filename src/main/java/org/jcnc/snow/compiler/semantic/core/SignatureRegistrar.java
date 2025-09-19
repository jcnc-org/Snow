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
 * {@code SignatureRegistrar} 是语义分析前置阶段的类型签名注册器。
 *
 * <p><b>职责</b>：
 * <ul>
 *   <li>登记所有模块的 import、结构体与函数签名</li>
 *   <li>校验跨模块引用与结构体继承</li>
 *   <li>为后续类型推断与语义分析建立完整的类型环境</li>
 * </ul>
 *
 * <p><b>实现流程</b>：
 * <ol>
 *   <li>校验 import，预注册结构体名称（占位）</li>
 *   <li>注册结构体字段、构造函数、方法签名</li>
 *   <li>处理结构体继承，继承父类字段和方法</li>
 *   <li>注册模块级顶层函数</li>
 * </ol>
 *
 * <p><b>错误处理</b>：
 * <ul>
 *   <li>未知模块、类型、父类均记录为 {@link SemanticError}</li>
 *   <li>类型不可解析则默认降级为 {@code int} 或 {@code void}，确保分析流程健壮</li>
 * </ul>
 */
public record SignatureRegistrar(Context ctx) {

    /**
     * 注册传入所有模块的类型签名信息（多阶段），确保跨模块结构体与继承正确处理。
     *
     * @param modules 所有模块 AST 节点列表
     */
    public void register(Iterable<ModuleNode> modules) {
        // 第一阶段：校验 import 与结构体名占位
        for (ModuleNode mod : modules) {
            ctx.setCurrentModule(mod.name());
            ModuleInfo mi = ctx.modules().get(mod.name());

            // 校验 import
            for (ImportNode imp : mod.imports()) {
                String raw = imp.moduleName();
                String resolved = resolveImportModuleName(raw);
                if (ctx.modules().containsKey(resolved)) {
                    mi.getImports().add(resolved);
                } else {
                    ctx.errors().add(new SemanticError(imp, "未知模块: " + raw));
                }
            }
            // 占位注册所有结构体名
            for (StructNode stn : mod.structs()) {
                mi.getStructs().putIfAbsent(stn.name(), new StructType(mod.name(), stn.name()));
            }
        }

        // 第二阶段：结构体字段、构造函数、方法签名注册
        for (ModuleNode mod : modules) {
            ctx.setCurrentModule(mod.name());
            ModuleInfo mi = ctx.modules().get(mod.name());

            for (StructNode stn : mod.structs()) {
                StructType st = mi.getStructs().get(stn.name());
                // 字段
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
                // 构造函数
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
                // 方法
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

        // 第三阶段：结构体继承
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
                // 建立继承关系
                child.setParent(parent);
                // 字段继承
                parent.getFields().forEach((k, v) -> child.getFields().putIfAbsent(k, v));
                // 方法继承
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

        // 第四阶段：模块顶层函数注册
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
     * 支持限定名（如 {@code Module.Struct}）及当前/导入模块内查找。
     */
    private StructType resolveParentStruct(ModuleInfo mi, String parentName) {
        int dot = parentName.indexOf('.');
        if (dot > 0 && dot < parentName.length() - 1) {
            String m = parentName.substring(0, dot);
            String s = parentName.substring(dot + 1);
            ModuleInfo pm = ctx.modules().get(m);
            if (pm != null) return pm.getStructs().get(s);
            return null;
        }
        // 当前模块查找
        StructType local = mi.getStructs().get(parentName);
        if (local != null) return local;
        // 导入模块查找
        for (String imported : mi.getImports()) {
            ModuleInfo pim = ctx.modules().get(imported);
            if (pim == null) continue;
            StructType st = pim.getStructs().get(parentName);
            if (st != null) return st;
        }
        return null;
    }

    /**
     * 将 import 路径式名称规范化为模块名。
     * 例：import: os.file 实际映射到 file（末段名作为本地别名）
     */
    private String resolveImportModuleName(String raw) {
        if (raw == null) return null;
        int dot = raw.lastIndexOf('.');
        return dot > 0 ? raw.substring(dot + 1) : raw;
    }
}
