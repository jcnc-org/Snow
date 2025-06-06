package org.jcnc.snow.compiler.parser.factory;

import org.jcnc.snow.compiler.parser.base.TopLevelParser;
import org.jcnc.snow.compiler.parser.module.ModuleParser;
import org.jcnc.snow.compiler.parser.function.FunctionParser;
import org.jcnc.snow.compiler.parser.top.ScriptTopLevelParser;

import java.util.Map;
import java.util.HashMap;

public class TopLevelParserFactory {

    private static final Map<String, TopLevelParser> registry = new HashMap<>();
    private static final TopLevelParser DEFAULT = new ScriptTopLevelParser(); // ← 默认解析器

    static {
        // 顶层结构解析器
        registry.put("module",   new ModuleParser());
        registry.put("function", new FunctionParser());
        // 也可按需继续注册其它关键字
    }

    /**
     * 根据关键字获取解析器；若未注册，回退到脚本语句解析。
     */
    public static TopLevelParser get(String keyword) {
        return registry.getOrDefault(keyword, DEFAULT);
    }
}
