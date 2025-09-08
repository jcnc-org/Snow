package org.jcnc.snow.compiler.parser.factory;

import org.jcnc.snow.compiler.parser.statement.*;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code StatementParserFactory} 是一个语句解析器工厂类，
 * 用于根据关键字动态选择适当的 {@link StatementParser} 实现。
 * <p>
 * 本类通过静态注册的方式将各类语句解析器绑定到对应的关键字上，
 * 在语法分析阶段根据当前语句起始词快速获取对应解析逻辑。
 * </p>
 * <p>
 * 若传入的关键字未被显式注册，将回退使用默认的表达式语句解析器 {@link ExpressionStatementParser}。
 * </p>
 */
public class StatementParserFactory {

    /**
     * 注册表: 语句关键字 -> 对应语句解析器
     */
    private static final Map<String, StatementParser> registry = new HashMap<>();

    static {
        // 注册各类语句解析器
        registry.put("declare", new DeclarationStatementParser());
        registry.put("if", new IfStatementParser());
        registry.put("loop", new LoopStatementParser());
        registry.put("return", new ReturnStatementParser());
        registry.put("break", new BreakStatementParser());
        registry.put("continue", new ContinueStatementParser());

        // 默认处理器: 表达式语句
        registry.put("", new ExpressionStatementParser());
    }

    /**
     * 根据关键字查找对应的语句解析器。
     *
     * @param keyword 当前语句起始关键字（如 "if"、"loop" 等）
     * @return 匹配的 {@link StatementParser} 实例；若无匹配则返回默认解析器
     */
    public static StatementParser get(String keyword) {
        return registry.getOrDefault(keyword, registry.get(""));
    }
}
