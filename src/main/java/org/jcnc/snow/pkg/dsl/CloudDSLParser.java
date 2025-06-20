package org.jcnc.snow.pkg.dsl;

import org.jcnc.snow.pkg.model.Project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CloudDSLParser —  .cloud 配置文件解析器
 * <p>
 * 作用：
 * - 读取 Snow 构建工具自定义的 .cloud 文件
 * - 将内容转换为内存中的 {@link Project} 模型
 * <p>
 * 解析规则（：
 * <p>
 * 1. 顶级区块（project、properties、repositories、dependencies、build）
 * 以 “sectionName {” 开始，以 “}” 结束。
 * <p>
 * 2. 区块内部识别 “key = value” 形式的赋值。
 * <p>
 * 3. build 区块允许嵌套；内部键通过 “.” 展平，
 * 例如 compile.enabled = true。
 * <p>
 */
public final class CloudDSLParser {

    /* ---------- 正则表达式 ---------- */

    /**
     * 匹配 “sectionName {” 形式的行
     */
    private static final Pattern SECTION_HEADER = Pattern.compile("^(\\w+)\\s*\\{\\s*$");

    /**
     * 匹配 “key = value” 行。
     * value 允许空格，并忽略行尾 “# …” 注释。
     */
    private static final Pattern KEY_VALUE = Pattern.compile("^(\\w+)\\s*=\\s*([^#]+?)\\s*(?:#.*)?$");

    /**
     * 匹配仅包含 “}” 的行（可有前后空白）
     */
    private static final Pattern BLOCK_END = Pattern.compile("^}\\s*$");

    /**
     * 私有构造，禁止实例化
     */
    private CloudDSLParser() {
    }

    /**
     * 解析指定 .cloud 文件并生成 {@link Project} 对象。
     *
     * @param path 文件路径
     * @return 解析后的 Project
     * @throws IOException           文件读取失败
     * @throws IllegalStateException 语法错误（如括号不匹配、未知语句）
     */
    public static Project parse(Path path) throws IOException {

        Deque<String> sectionStack = new ArrayDeque<>();       // 记录当前区块层级
        Map<String, String> flatMap = new LinkedHashMap<>();   // 扁平化 key → value

        List<String> lines = Files.readAllLines(path);

        int lineNo = 0;
        for (String raw : lines) {
            lineNo++;
            String line = raw.trim();

            /* 1. 跳过空行和注释 */
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            /* 2. 区块起始：sectionName { */
            Matcher sec = SECTION_HEADER.matcher(line);
            if (sec.matches()) {
                sectionStack.push(sec.group(1));
                continue;
            }

            /* 3. 区块结束：} */
            if (BLOCK_END.matcher(line).matches()) {
                if (sectionStack.isEmpty()) {
                    throw new IllegalStateException("第 " + lineNo + " 行出现未配对的 '}'");
                }
                sectionStack.pop();
                continue;
            }

            /* 4. 键值对：key = value */
            Matcher kv = KEY_VALUE.matcher(line);
            if (kv.matches()) {
                String key = kv.group(1).trim();
                String value = kv.group(2).trim();

                /* 4.1 计算前缀（栈倒序，即从外到内） */
                String prefix = String.join(".", (Iterable<String>) sectionStack::descendingIterator);
                if (!prefix.isEmpty()) {
                    key = prefix + "." + key;
                }

                flatMap.put(key, value);
                continue;
            }

            /* 5. 无法识别的行 */
            throw new IllegalStateException("无法解析第 " + lineNo + " 行: " + raw);
        }

        /* 6. 检查括号是否全部闭合 */
        if (!sectionStack.isEmpty()) {
            throw new IllegalStateException("文件结束但区块未闭合：" + sectionStack);
        }

        /* 7. 构造 Project 模型 */
        return Project.fromFlatMap(flatMap);
    }
}
