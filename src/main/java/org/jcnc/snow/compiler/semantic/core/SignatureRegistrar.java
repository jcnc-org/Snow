package org.jcnc.snow.compiler.semantic.core;

import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.ImportNode;
import org.jcnc.snow.compiler.parser.ast.ModuleNode;
import org.jcnc.snow.compiler.parser.ast.ParameterNode;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.FunctionType;
import org.jcnc.snow.compiler.semantic.type.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@code SignatureRegistrar} 负责函数签名登记与导入语义检查。
 * <p>
 * 在语义分析初期阶段，它遍历每个模块，完成以下任务: 
 * <ul>
 *   <li>验证所有 {@link ImportNode} 导入的模块是否存在于全局模块表 {@link Context#modules()} 中；</li>
 *   <li>将每个 {@link FunctionNode} 的函数签名（参数类型和返回类型）注册到对应 {@link ModuleInfo} 中；</li>
 *   <li>在参数或返回类型无法识别时，记录 {@link SemanticError}，并进行容错降级。</li>
 * </ul>
 * 本组件作为语义分析的准备阶段，为后续函数体检查提供函数类型上下文。
 *
 * @param ctx 全局语义分析上下文，提供模块、类型、错误管理等功能
 */
public record SignatureRegistrar(Context ctx) {

    /**
     * 构造函数签名注册器。
     *
     * @param ctx 当前语义分析上下文
     */
    public SignatureRegistrar {
    }

    /**
     * 遍历模块并注册函数签名，同时校验导入模块的合法性。
     *
     * @param mods 所有模块的语法树节点集合
     */
    public void register(Iterable<ModuleNode> mods) {
        for (ModuleNode mod : mods) {
            ModuleInfo mi = ctx.modules().get(mod.name());

            // ---------- 1. 模块导入检查 ----------
            for (ImportNode imp : mod.imports()) {
                if (!ctx.modules().containsKey(imp.moduleName())) {
                    ctx.errors().add(new SemanticError(
                            imp,
                            "未知模块: " + imp.moduleName()
                    ));
                } else {
                    mi.getImports().add(imp.moduleName());
                }
            }

            // ---------- 2. 函数签名注册 ----------
            for (FunctionNode fn : mod.functions()) {
                List<Type> params = new ArrayList<>();

                // 参数类型解析
                for (ParameterNode p : fn.parameters()) {
                    Type t = Optional.ofNullable(ctx.parseType(p.type()))
                            .orElseGet(() -> {
                                ctx.errors().add(new SemanticError(
                                        p,
                                        "未知类型: " + p.type()
                                ));
                                return BuiltinType.INT; // 容错降级
                            });
                    params.add(t);
                }

                // 返回类型解析（默认降级为 void）
                Type ret = Optional.ofNullable(ctx.parseType(fn.returnType()))
                        .orElse(BuiltinType.VOID);

                // 注册函数签名
                mi.getFunctions().put(fn.name(), new FunctionType(params, ret));
            }
        }
    }
}
