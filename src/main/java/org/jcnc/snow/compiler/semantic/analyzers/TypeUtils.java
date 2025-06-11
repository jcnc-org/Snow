package org.jcnc.snow.compiler.semantic.analyzers;

import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.Type;

/**
 * 实用类型辅助工具类，提供与类型相关的静态方法。
 * <p>
 * 本类为静态工具类，无法被实例化，仅用于类型判断等功能。
 * </p>
 */
public final class TypeUtils {
    /**
     * 私有构造方法，防止实例化工具类。
     */
    private TypeUtils() {
        // 工具类不允许被实例化
    }

    /**
     * 判断给定类型是否为“逻辑类型”。
     * <p>
     * 当前的实现仅判断类型是否不是布尔类型（BOOLEAN）。
     * 如果类型不是 BOOLEAN，则认为是“逻辑类型”。
     * </p>
     *
     * @param t 需要检查的类型对象
     * @return 如果 t 不是 {@link BuiltinType#BOOLEAN}，则返回 {@code true}，否则返回 {@code false}
     */
    public static boolean isLogic(Type t) {
        return t != BuiltinType.BOOLEAN;
    }
}
