package org.jcnc.snow.compiler.semantic.core;

import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.FunctionType;
import org.jcnc.snow.compiler.semantic.type.Type;

import java.util.*;

/**
 * 语言全部内置类型 / 模块 / 函数的注册中心。
 *
 * <p>目前同时注册：</p>
 * <ul>
 *   <li>所有基础类型（byte、short、int …）</li>
 *   <li>标准库模块 <b>BuiltinUtils</b> —— 仅声明函数签名，真正实现写在 Snow 源码中</li>
 *   <li>运行时内核函数 <b>syscall</b> —— 供标准库内部实现调用</li>
 * </ul>
 */
public final class BuiltinTypeRegistry {

    /** 基础类型表：名称 → Type */
    public static final Map<String, Type> BUILTIN_TYPES;
    static {
        Map<String, Type> t = new HashMap<>();
        t.put("byte",   BuiltinType.BYTE);
        t.put("short",  BuiltinType.SHORT);
        t.put("int",    BuiltinType.INT);
        t.put("long",   BuiltinType.LONG);
        t.put("float",  BuiltinType.FLOAT);
        t.put("double", BuiltinType.DOUBLE);
        t.put("string", BuiltinType.STRING);
        t.put("void",   BuiltinType.VOID);
        BUILTIN_TYPES = Collections.unmodifiableMap(t);
    }

    private BuiltinTypeRegistry() { }

    /**
     * 供语义分析阶段调用：向上下文注入所有内置模块 / 函数声明。
     */
    public static void init(Context ctx) {
        /* ---------- BuiltinUtils ---------- */
        ModuleInfo utils = new ModuleInfo("BuiltinUtils");

        // print(string): void
        utils.getFunctions().put(
                "print",
                new FunctionType(
                        Collections.singletonList(BuiltinType.STRING),
                        BuiltinType.VOID
                )
        );

        // println(string): void
        utils.getFunctions().put(
                "println",
                new FunctionType(
                        Collections.singletonList(BuiltinType.STRING),
                        BuiltinType.VOID
                )
        );

        // syscall(string, string): void   —— 供 BuiltinUtils 内部使用
        utils.getFunctions().put(
                "syscall",
                new FunctionType(
                        Arrays.asList(BuiltinType.STRING, BuiltinType.STRING),
                        BuiltinType.VOID
                )
        );

        ctx.getModules().putIfAbsent("BuiltinUtils", utils);
    }
}
