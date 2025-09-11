package org.jcnc.snow.compiler.parser.factory;

import org.jcnc.snow.compiler.parser.base.TopLevelParser;
import org.jcnc.snow.compiler.parser.function.FunctionParser;
import org.jcnc.snow.compiler.parser.module.ModuleParser;
import org.jcnc.snow.compiler.parser.top.ScriptTopLevelParser;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code TopLevelParserFactory} 用于根据源码中顶层关键字取得对应的解析器。
 * <p>
 * 若关键字未注册，则回退到脚本模式解析器 {@link ScriptTopLevelParser}。
 */
public class TopLevelParserFactory {

    /**
     * 关键字 → 解析器注册表
     */
    private static final Map<String, TopLevelParser> registry = new HashMap<>();

    /**
     * 缺省解析器: 脚本模式（单条语句可执行）
     */
    private static final TopLevelParser DEFAULT = new ScriptTopLevelParser();

    static {
        // 在此注册所有受支持的顶层结构关键字
        registry.put("module", new ModuleParser());
        registry.put("function", new FunctionParser());
        // 若未来新增顶层结构，可继续在此处注册
    }

    /**
     * 依据关键字返回解析器；若未注册则返回脚本解析器。
     */
    public static TopLevelParser get(String keyword) {
        return registry.getOrDefault(keyword, DEFAULT);
    }

    /**
     * 判断某关键字是否已显式注册为顶层结构，
     * 供同步恢复逻辑使用，避免死循环。
     */
    public static boolean isRegistered(String keyword) {
        return registry.containsKey(keyword);
    }
}
