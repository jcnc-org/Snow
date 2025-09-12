package org.jcnc.snow.compiler.semantic.core;

import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.FunctionType;
import org.jcnc.snow.compiler.semantic.type.Type;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <b>语言内置类型、标准库模块与内核函数的注册中心。</b>
 *
 * <p>
 * 本类统一负责注册和管理 Snow 语言编译器中的所有内置基础类型、标准库模块以及内核级函数（如 syscall）。
 * 主要职责：
 * <ul>
 *   <li>定义并集中管理所有语言内置基础类型（如 int、string 等），
 *       方便类型系统与语义分析统一引用。</li>
 *   <li>注册标准库模块及其内核函数到语义分析上下文，确保全局唯一性与一致性。</li>
 *   <li>设计为工具类，禁止被实例化，仅通过静态成员进行访问。</li>
 * </ul>
 * </p>
 *
 * <p><b>使用说明：</b>
 * <ul>
 *   <li>通过 {@link #BUILTIN_TYPES} 获取所有内置类型的只读映射。</li>
 *   <li>通过 {@link #init(Context)} 注册标准库模块及其内核函数到语义分析上下文。</li>
 * </ul>
 * </p>
 */
public final class BuiltinTypeRegistry {

    /**
     * <b>内置基础类型表：</b>
     * <p>
     * 类型名到 {@link Type} 实例的全局只读映射。<br>
     * 例如："int" -> BuiltinType.INT。
     * </p>
     * <p>
     * 用于语义分析阶段，支持类型查找和检查。
     * </p>
     */
    public static final Map<String, Type> BUILTIN_TYPES;

    // === 静态初始化内置类型表 ===
    static {
        Map<String, Type> t = new HashMap<>();
        // 整数类型
        t.put("byte", BuiltinType.BYTE);      // 单字节无符号整数
        t.put("short", BuiltinType.SHORT);    // 短整型
        t.put("int", BuiltinType.INT);        // 标准整型
        t.put("long", BuiltinType.LONG);      // 长整型
        // 浮点类型
        t.put("float", BuiltinType.FLOAT);    // 单精度浮点数
        t.put("double", BuiltinType.DOUBLE);  // 双精度浮点数
        // 字符串类型
        t.put("string", BuiltinType.STRING);  // 字符串类型
        // 布尔类型
        t.put("boolean", BuiltinType.BOOLEAN);// 布尔类型
        // 空类型
        t.put("void", BuiltinType.VOID);      // 无返回值类型
        // 任意类型（万能类型，类似 dynamic 或 any）
        t.put("any", BuiltinType.ANY);        // 任意类型
        // 构建只读映射，防止外部修改
        BUILTIN_TYPES = Collections.unmodifiableMap(t);
    }

    /**
     * <b>工具类构造函数，私有化，禁止实例化。</b>
     * <p>
     * 该类仅作为静态工具类存在，不能被外部创建实例。
     * </p>
     */
    private BuiltinTypeRegistry() {
        // 禁止实例化
    }

    /**
     * <b>向语义分析上下文注册所有标准库模块及其内核函数。</b>
     *
     * <p>
     * 当前注册内容包括：
     * <ul>
     *   <li><b>os 模块</b>
     *     <ul>
     *       <li><b>syscall(string, any): void</b> —— 用于标准库内部与底层系统通信的内核函数。</li>
     *     </ul>
     *   </li>
     * </ul>
     * </p>
     *
     * @param ctx 语义分析上下文，用于存储全局模块与符号表。
     */
    public static void init(Context ctx) {
        // 注册标准库模块 os
        ModuleInfo utils = new ModuleInfo("os");

        // 注册 syscall(string, any): void 内核函数
        // 参数类型：string, any
        // 返回类型：void
        FunctionType syscallFn = new FunctionType(
                Arrays.asList(BuiltinType.STRING, BuiltinType.ANY),
                BuiltinType.VOID
        );
        // 添加到 os 模块函数表
        utils.getFunctions().put("syscall", syscallFn);

        //写入上下文中的模块表，已存在则不覆盖
        ctx.getModules().putIfAbsent("os", utils);
    }
}
