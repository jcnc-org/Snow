package org.jcnc.snow.compiler.ir.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局函数返回类型表。
 * <p>
 * 此工具类用于在编译前端构建每个函数 IR（中间表示）时登记函数的返回类型，
 * 以便后端在生成 {@code CALL} 指令时判断是否需要保存返回值。
 * </p>
 *
 * 使用说明
 * <ul>
 *   <li>在函数 IR 构建阶段，调用 {@link #register(String, String)} 方法登记函数名与返回类型。</li>
 *   <li>在生成调用指令阶段，通过 {@link #getReturnType(String)} 查询函数的返回类型。</li>
 *   <li>返回类型统一为小写；若调用 {@code register} 时传入的返回类型为 {@code null}，则登记为 {@code "void"}。</li>
 * </ul>
 */
public final class GlobalFunctionTable {

    /**
     * 存储全局函数返回类型映射表。
     * <ul>
     *   <li>Key: 函数名（不含模块限定）</li>
     *   <li>Value: 返回类型，统一转换为小写字符串；若无返回值则为 {@code "void"}</li>
     * </ul>
     */
    private static final Map<String, String> RETURN_TYPES = new ConcurrentHashMap<>();

    /**
     * 私有构造函数，防止实例化。
     */
    private GlobalFunctionTable() {
        // 工具类，禁止实例化
    }

    /**
     * 登记或更新指定函数的返回类型。
     *
     * <p>若传入的 {@code returnType} 为 {@code null}，则登记为 {@code "void"}。
     * 否则将去除前后空白并转换为小写后登记。</p>
     *
     * @param name       函数名（不含模块限定）
     * @param returnType 函数返回类型字符串，如 {@code "int"}、{@code "String"}。
     *                   如果为 {@code null}，则登记类型为 {@code "void"}。
     * @throws IllegalArgumentException 如果 {@code name} 为空或 {@code null}
     */
    public static void register(String name, String returnType) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("函数名不能为空或 null");
        }
        RETURN_TYPES.put(
                name,
                returnType == null
                        ? "void"
                        : returnType.trim().toLowerCase()
        );
    }

    /**
     * 查询指定函数的返回类型。
     *
     * <p>返回类型为登记时的值（小写），如果函数未登记过，则返回 {@code null}。</p>
     *
     * @param name 函数名（不含模块限定）
     * @return 已登记的返回类型（小写），或 {@code null} 表示未知
     * @throws IllegalArgumentException 如果 {@code name} 为空或 {@code null}
     */
    public static String getReturnType(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("函数名不能为空或 null");
        }
        return RETURN_TYPES.get(name);
    }
}
