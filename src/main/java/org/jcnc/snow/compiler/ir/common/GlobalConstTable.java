package org.jcnc.snow.compiler.ir.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局常量表，用于跨模块编译期常量查询和折叠。
 *
 * <p>
 * 主要功能：
 * <ul>
 *   <li>在 IRProgramBuilder 预扫描阶段，将所有模块级 <code>const</code> 常量
 *       （如 ModuleA.a）注册到全局常量表，支持跨模块访问。</li>
 *   <li>后续任何阶段均可通过 {@link #get(String)} 查询已注册常量，实现编译期常量折叠。</li>
 *   <li>保证线程安全，支持并发注册和访问。</li>
 * </ul>
 * <p>
 * 常量的 key 格式为“模块名.常量名”，如 "ModuleA.a"，以便唯一标识。
 *
 * <p>
 * 典型用法：
 * <pre>
 *   GlobalConstTable.register("ModuleA.a", 10);    // 注册常量
 *   Object val = GlobalConstTable.get("ModuleA.a"); // 查询常量
 * </pre>
 */
public final class GlobalConstTable {

    /** 存储全局常量: “ModuleName.constName” → 常量值。线程安全。 */
    private static final Map<String, Object> CONSTS = new ConcurrentHashMap<>();

    /**
     * 工具类构造器，防止实例化。
     */
    private GlobalConstTable() { /* utility class */ }

    /**
     * 注册一个全局常量到表中（只在首次注册时生效，避免被覆盖）。
     *
     * @param qualifiedName 常量的全限定名（如 "ModuleA.a"）
     * @param value         常量的字面值（如 10、字符串、布尔等）
     * @throws IllegalArgumentException 名称为 null 或空串时抛出
     */
    public static void register(String qualifiedName, Object value) {
        if (qualifiedName == null || qualifiedName.isBlank()) {
            throw new IllegalArgumentException("常量名不能为空");
        }
        CONSTS.putIfAbsent(qualifiedName, value);
    }

    /**
     * 获取指定全局常量的值。
     *
     * @param qualifiedName 常量的全限定名（如 "ModuleA.a"）
     * @return 查到的常量值，如果未注册则返回 null
     */
    public static Object get(String qualifiedName) {
        return CONSTS.get(qualifiedName);
    }

    /**
     * 返回全部已注册常量的不可变视图（快照）。
     * <p>注意：只读，不可修改。</p>
     *
     * @return key=常量名，value=常量值的不可变 Map
     */
    public static Map<String, Object> all() {
        return Map.copyOf(CONSTS);
    }
}
