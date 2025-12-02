package org.jcnc.snow.compiler.semantic.analyzers.expression;

import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.IdentifierNode;
import org.jcnc.snow.compiler.parser.ast.MemberExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.semantic.analyzers.base.ExpressionAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.symbol.Symbol;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.FunctionType;
import org.jcnc.snow.compiler.semantic.type.StructType;
import org.jcnc.snow.compiler.semantic.type.Type;

/**
 * {@code MemberExpressionAnalyzer} 负责成员访问表达式（如 a.b、this.x、Module.member）的语义分析与类型推断。
 *
 * <p><b>支持的成员访问场景：</b>
 * <ul>
 *   <li>当前实例字段访问（this.x）：可等价视为本地或全局符号表查找 x</li>
 *   <li>跨模块成员访问（Module.member）：引用导入模块的常量、变量、函数等</li>
 *   <li>结构体实例成员或方法访问（a.b, a.method）：a 为结构体变量，b 为字段或方法</li>
 * </ul>
 *
 * <p>如遇类型未定义、成员不存在或访问非法，均会自动记录 {@link SemanticError} 并降级为 int 类型，确保分析流程不中断。</p>
 */
public class MemberExpressionAnalyzer implements ExpressionAnalyzer<MemberExpressionNode> {

    /**
     * 对成员访问表达式进行语义分析与类型推断。
     *
     * @param ctx    语义分析全局上下文
     * @param mi     当前模块信息
     * @param fn     所在函数节点
     * @param locals 局部符号表
     * @param expr   成员访问表达式节点
     * @return 推断得到的类型（如字段类型、方法类型、全局变量类型等）；如分析失败则降级为 int 类型
     */
    @Override
    public Type analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        MemberExpressionNode expr) {

        ctx.log("检查成员访问: " + expr);

        // 1. 处理 this.x 语法糖
        if (expr.object() instanceof IdentifierNode oid) {
            String objName = oid.name();

            if ("this".equals(objName)) {
                // 先查当前局部符号表
                if (locals != null) {
                    Symbol localSym = locals.resolve(expr.member());
                    if (localSym != null) {
                        return localSym.type();
                    }
                }
                // 再查当前模块全局符号表
                SymbolTable globals = mi != null ? mi.getGlobals() : null;
                if (globals != null) {
                    Symbol globalSym = globals.resolve(expr.member());
                    if (globalSym != null) {
                        return globalSym.type();
                    }
                }
                // 都查不到则报错
                ctx.getErrors().add(new SemanticError(expr, "未定义的字段或变量: " + expr.member()));
                ctx.log("错误: 未定义的字段或变量 this." + expr.member());
                return BuiltinType.INT;
            }
        }

        // 2. 处理 ModuleName.member 跨模块访问
        if (expr.object() instanceof IdentifierNode(String mod, NodeContext nodeContext)) {
            boolean moduleExists = ctx.getModules().containsKey(mod);
            boolean importedOrSelf = mi != null && (
                    mi.getName().equals(mod)
                            || mi.getImports().contains(mod)
                            || mi.getImports().stream().anyMatch(s ->
                            s != null && (s.startsWith(mod + ".")
                                    || (s.contains(".") && s.substring(0, s.indexOf('.')).equals(mod)))
                    )
            );
            if (moduleExists && importedOrSelf) {
                ModuleInfo target = ctx.getModules().get(mod);
                // 1) 查模块函数
                FunctionType ft = target.getFunctions().get(expr.member());
                if (ft != null) return ft;

                // 2) 查模块全局符号
                SymbolTable globals = target.getGlobals();
                if (globals != null) {
                    Symbol sym = globals.resolve(expr.member());
                    if (sym != null) {
                        return sym.type();
                    }
                }
                // 未找到
                ctx.getErrors().add(new SemanticError(expr, "模块成员未定义: " + mod + "." + expr.member()));
                ctx.log("错误: 模块成员未定义 " + mod + "." + expr.member());
                return BuiltinType.INT;
            }
            // 不是模块名，尝试变量（结构体实例）
            if (locals != null) {
                Symbol sym = locals.resolve(mod);
                if (sym != null && sym.type() instanceof StructType st) {
                    Type t = st.getFields().get(expr.member());
                    if (t != null) return t;
                    FunctionType ft = st.getMethods().get(expr.member());
                    if (ft != null) return ft;
                    ctx.getErrors().add(new SemanticError(expr, "结构体成员未定义: " + st + "." + expr.member()));
                    ctx.log("错误: 结构体成员未定义 " + st + "." + expr.member());
                    return BuiltinType.INT;
                }
            }
            ctx.getErrors().add(new SemanticError(expr, "未知或未导入模块: " + mod));
            ctx.log("错误: 未导入模块或未声明变量 " + mod);
            return BuiltinType.INT;
        }

        // 3. 一般结构体成员访问
        Type leftType = ctx.getRegistry().getExpressionAnalyzer(expr.object())
                .analyze(ctx, mi, fn, locals, expr.object());

        if (leftType instanceof StructType st) {
            Type t = st.getFields().get(expr.member());
            if (t != null) return t;
            FunctionType ft = st.getMethods().get(expr.member());
            if (ft != null) return ft;
            ctx.getErrors().add(new SemanticError(expr, "结构体成员未定义: " + st + "." + expr.member()));
            ctx.log("错误: 结构体成员未定义 " + st + "." + expr.member());
            return BuiltinType.INT;
        }

        // 4. 其它不支持的对象类型成员访问
        ctx.getErrors().add(new SemanticError(expr,
                "不支持的成员访问对象类型: " + expr.object().getClass().getSimpleName()));
        ctx.log("错误: 不支持的成员访问对象类型 " + expr.object().getClass().getSimpleName());
        return BuiltinType.INT;
    }
}
