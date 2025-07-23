package org.jcnc.snow.compiler.semantic.core;

import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.FunctionType;
import org.jcnc.snow.compiler.semantic.type.Type;

import java.util.*;

/**
 * <b>BuiltinTypeRegistry - 语言全部内置类型/模块/函数注册中心</b>
 *
 * <p>
 * 该类统一注册编译器需要用到的所有基础类型、标准库模块与内核函数，供语义分析及类型检查阶段使用。
 * <ul>
 *   <li>所有基础类型（byte、short、int、long、float、double、string、boolean、void）</li>
 *   <li>标准库模块 <b>BuiltinUtils</b>（仅注册函数签名，具体实现由 Snow 语言源码实现）</li>
 *   <li>内核函数 <b>syscall</b>（供标准库内部实现调用）</li>
 * </ul>
 * </p>
 */
public final class BuiltinTypeRegistry {

    /**
     * <b>基础类型表</b>：类型名称 → Type 实例
     * <p>
     * 本 Map 静态初始化，注册所有 Snow 语言基础类型，供类型检查与类型推断使用。
     * </p>
     */
    public static final Map<String, Type> BUILTIN_TYPES;
    static {
        Map<String, Type> t = new HashMap<>();
        t.put("byte",   BuiltinType.BYTE);     // 字节型
        t.put("short",  BuiltinType.SHORT);    // 短整型
        t.put("int",    BuiltinType.INT);      // 整型
        t.put("long",   BuiltinType.LONG);     // 长整型
        t.put("float",  BuiltinType.FLOAT);    // 单精度浮点
        t.put("double", BuiltinType.DOUBLE);   // 双精度浮点
        t.put("string", BuiltinType.STRING);   // 字符串
        t.put("void",   BuiltinType.VOID);     // 无返回
        t.put("boolean", BuiltinType.BOOLEAN); // 布尔类型

        BUILTIN_TYPES = Collections.unmodifiableMap(t); // 不可变映射，防止被意外更改
    }

    /**
     * 私有构造方法，禁止实例化
     */
    private BuiltinTypeRegistry() { }

    /**
     * <b>初始化内置模块和函数声明</b>
     *
     * <p>
     * 语义分析阶段调用，将所有基础模块与函数声明注册到语义上下文中。
     * - 目前注册 BuiltinUtils 标准库模块（仅注册签名，不负责具体实现）。
     * - syscall 函数注册到 BuiltinUtils 内，供标准库内部调用。
     * </p>
     *
     * @param ctx 全局语义分析上下文，持有模块表
     */
    public static void init(Context ctx) {
        /* ---------- 注册标准库 os ---------- */
        ModuleInfo utils = new ModuleInfo("os");

        // syscall(string, int): void   —— 供标准库内部使用的调用接口
        utils.getFunctions().put(
                "syscall",
                new FunctionType(
                        Arrays.asList(BuiltinType.STRING, BuiltinType.INT),
                        BuiltinType.VOID
                )
        );

        // 注册 BuiltinUtils 到上下文的模块表（若已存在则不重复添加）
        ctx.getModules().putIfAbsent("os", utils);
    }
}
