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
 * {@code SignatureRegistrar}
 * <p>
 * 语义分析准备阶段：负责函数签名登记、结构体签名登记与 import 校验。
 *
 * <ul>
 *   <li>验证每个模块声明的 import 模块在全局模块表 {@link Context#modules()} 是否存在。</li>
 *   <li>将每个函数、结构体方法、构造函数的类型签名登记到 {@link ModuleInfo}，便于后续类型推断。</li>
 *   <li>支持 {@code extends} 单继承：子类会继承父类的字段与方法。</li>
 *   <li>若参数或返回类型无法解析，则报错并降级为 int 或 void，保证语义分析流程健壮。</li>
 * </ul>
 * <p>
 * 作为语义分析前置流程，为后续函数体和表达式分析提供类型环境。
 */
public record SignatureRegistrar(Context ctx) {

    /**
     * 遍历所有模块，注册函数/方法/结构体签名，校验 import 合法性。
     *
     * @param modules 需要分析的所有模块列表（AST 顶层节点）
     */
    public void register(Iterable<ModuleNode> modules) {
        for (ModuleNode mod : modules) {
            ctx.setCurrentModule(mod.name()); // 切换上下文到当前模块
            ModuleInfo mi = ctx.modules().get(mod.name());

            // ========== 1) 校验 imports ==========
            for (ImportNode imp : mod.imports()) {
                if (!ctx.modules().containsKey(imp.moduleName())) {
                    // 导入的模块在全局表中不存在，报错
                    ctx.errors().add(new SemanticError(imp, "未知模块: " + imp.moduleName()));
                } else {
                    // 添加到本模块导入集合
                    mi.getImports().add(imp.moduleName());
                }
            }

            // ========== 2) 结构体签名登记 ==========
            for (StructNode stn : mod.structs()) {
                // 构造结构体类型对象，唯一标识为 (模块名, 结构体名)
                StructType st = new StructType(mod.name(), stn.name());
                mi.getStructs().put(stn.name(), st);

                // --- 2.0 字段签名登记 ---
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

                // --- 2.1 多个构造函数 init（重载，按参数个数区分） ---
                if (stn.inits() != null) {
                    for (FunctionNode initFn : stn.inits()) {
                        List<Type> ptypes = new ArrayList<>();
                        for (ParameterNode p : initFn.parameters()) {
                            // 解析参数类型，不存在则报错降级为 int
                            Type t = ctx.parseType(p.type());
                            if (t == null) {
                                ctx.errors().add(new SemanticError(p, "未知类型: " + p.type()));
                                t = BuiltinType.INT;
                            }
                            ptypes.add(t);
                        }
                        // 构造函数返回类型固定为 void
                        st.addConstructor(new FunctionType(ptypes, BuiltinType.VOID));
                    }
                }

                // --- 2.2 方法签名 ---
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
                    st.getMethods().put(fn.name(), new FunctionType(ptypes, ret));
                }
            }

            // ========== 2.3 继承处理 ==========
            for (StructNode stn : mod.structs()) {
                if (stn.parent() != null) {
                    StructType child = mi.getStructs().get(stn.name());
                    StructType parent = resolveParentStruct(mi, stn.parent());

                    if (parent == null) {
                        // 父类不存在（既不在本模块，也不在导入模块 / 限定名错误），报语义错误
                        ctx.errors().add(new SemanticError(stn, "未知父类: " + stn.parent()));
                    } else {
                        // 建立继承链
                        child.setParent(parent);

                        // 继承字段
                        parent.getFields().forEach(
                                (k, v) -> child.getFields().putIfAbsent(k, v));
                        // 继承方法
                        parent.getMethods().forEach(
                                (k, v) -> child.getMethods().putIfAbsent(k, v));
                        // 构造函数不继承
                    }
                }
            }

            // ========== 3) 模块级函数签名登记 ==========
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
     * 解析父类结构体：
     * <ul>
     *   <li>支持限定名 {@code Module.Struct}。</li>
     *   <li>支持在本模块与 import 的模块中按未限定名查找。</li>
     * </ul>
     */
    private StructType resolveParentStruct(ModuleInfo mi, String parentName) {
        // 1. 限定名：Module.Struct
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

        // 2. 先在当前模块找
        StructType local = mi.getStructs().get(parentName);
        if (local != null) return local;

        // 3. 在导入模块中找
        for (String imported : mi.getImports()) {
            ModuleInfo pim = ctx.modules().get(imported);
            if (pim == null) continue;
            StructType st = pim.getStructs().get(parentName);
            if (st != null) return st;
        }

        // 未找到
        return null;
    }
}
