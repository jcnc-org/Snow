package org.jcnc.snow.pkg.dsl;

import org.jcnc.snow.pkg.model.Project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CloudDSL 配置文件解析器。
 * <p>
 * 负责将自定义的 .cloud 构建配置文件解析为 {@link Project} 模型。
 * </p>
 *
 * <ul>
 *   <li>顶级区块（如 project、properties、repositories、dependencies、build）以 <code>sectionName &#123;</code> 开始，以 <code>&#125;</code> 结束</li>
 *   <li>区块内部只识别 <code>key = value</code> 赋值，行尾可有 <code># 注释</code></li>
 *   <li>build 区块支持嵌套，内部键通过 <code>.</code> 展平，例如 <code>compile.enabled = true</code></li>
 *   <li><strong>新增: </strong>对 <code>"value"</code> 或 <code>'value'</code> 形式的字面量自动去引号，调用方得到的均是不含引号的裸字符串</li>
 * </ul>
 *
 * <pre>
 * 示例 .cloud 文件片段:
 * project {
 *   group = com.example
 *   artifact = "demo-app"
 *   version = 1.0.0
 * }
 * </pre>
 */
public final class CloudDSLParser {

    /** 匹配 sectionName { 的行 */
    private static final Pattern SECTION_HEADER = Pattern.compile("^(\\w+)\\s*\\{\\s*$");

    /**
     * 匹配 key = value 行，忽略行尾注释。
     * 使用非贪婪匹配 <code>.*?</code>，确保 <code>value</code> 内部允许出现空格或 =。
     */
    private static final Pattern KEY_VALUE = Pattern.compile("^(\\w+)\\s*=\\s*(.*?)\\s*(?:#.*)?$");

    /** 匹配仅为 } 的行 */
    private static final Pattern BLOCK_END = Pattern.compile("^}\\s*$");

    /** 工具类禁止实例化 */
    private CloudDSLParser() {}

    /**
     * 解析指定 .cloud 文件为 {@link Project} 对象。
     * <ul>
     *   <li>遇到语法错误（括号不配对、无法识别的行）时抛出异常</li>
     *   <li>支持嵌套区块和注释</li>
     *   <li>对字面量自动去除成对单/双引号</li>
     * </ul>
     *
     * @param path .cloud 文件路径
     * @return 解析得到的 Project 实例
     * @throws IOException           文件读取失败
     * @throws IllegalStateException 文件内容格式非法或语法错误
     */
    public static Project parse(Path path) throws IOException {

        Deque<String> sectionStack = new ArrayDeque<>();       // 当前区块栈
        Map<String, String> flatMap = new LinkedHashMap<>();   // 扁平化后的 key → value

        List<String> lines = Files.readAllLines(path);

        int lineNo = 0;
        for (String raw : lines) {
            lineNo++;
            String line = raw.trim();

            // 跳过空行和注释
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            // 区块起始
            Matcher sec = SECTION_HEADER.matcher(line);
            if (sec.matches()) {
                sectionStack.push(sec.group(1));
                continue;
            }

            // 区块结束
            if (BLOCK_END.matcher(line).matches()) {
                if (sectionStack.isEmpty()) {
                    throw new IllegalStateException("第 " + lineNo + " 行出现未配对的 '}'");
                }
                sectionStack.pop();
                continue;
            }

            // 键值对
            Matcher kv = KEY_VALUE.matcher(line);
            if (kv.matches()) {
                String key = kv.group(1).trim();
                String value = kv.group(2).trim();
                value = unquote(value);                    // 去除首尾成对引号

                // 计算区块前缀
                String prefix = String.join(".", (Iterable<String>) sectionStack::descendingIterator);
                if (!prefix.isEmpty()) {
                    key = prefix + "." + key;
                }

                flatMap.put(key, value);
                continue;
            }

            // 无法识别的行
            throw new IllegalStateException("无法解析第 " + lineNo + " 行: " + raw);
        }

        // 检查区块是否全部闭合
        if (!sectionStack.isEmpty()) {
            throw new IllegalStateException("文件结束但区块未闭合: " + sectionStack);
        }

        // 构建 Project 模型
        return Project.fromFlatMap(flatMap);
    }

    /**
     * 如果字符串首尾包裹成对单引号或双引号，则去掉引号后返回；否则直接返回原字符串。
     */
    private static String unquote(String s) {
        if (s == null || s.length() < 2) return s;
        char first = s.charAt(0);
        char last  = s.charAt(s.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
