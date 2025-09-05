# Snow Compiler - Common 公共模块

> Snow 编译器的通用基础能力 —— 统一运行模式/调试输出与字符串转义工具

## 项目简介

**Common** 是 Snow 编译器项目的基础工具模块，提供两类核心能力：
1）统一的**运行/调试模式**切换与便捷的调试输出；2）在虚拟机（VM）执行阶段使用的**字符串反转义**工具。
该模块以极简 API 抽象暴露通用能力，供编译阶段与运行时组件复用。

## 核心功能

* **运行模式与调试输出**

    * 模式枚举：`Mode` —— `RUN`（运行）、`DEBUG`（调试）
    * 全局配置：`SnowConfig`
        * 运行模式：`public static Mode MODE`（默认 `RUN`）
        * 判定方法：`isDebug()` / `isRun()`
        * 调试输出（仅 `DEBUG` 模式生效）：
            * `print(String msg)` —— 行输出
            * `print(String fmt, Object... args)` —— `printf` 风格格式化输出

* **字符串转义（运行期反转义）**

    * 工具类：`StringEscape`
        * 反转义：`unescape(String src)`
            * 支持：`\n`、`\t`、`\r`、`\b`、`\f`、`\\`、`\"`、`\'`
            * 支持 Unicode：`\uXXXX`（十六进制 4 位）
            * 容错策略：
                * 非法 `\u` 十六进制序列：以原样 `\uXXXX` 输出
                * 字符串末尾单个反斜杠：按原样 `\` 输出
                * 未定义转义：输出其字面字符（如 `\x` → `x`）

## 模块结构

```
common/                         // 公共工具模块（包名：org.jcnc.snow.common）
  ├── Mode.java                 // 运行/调试模式枚举：RUN / DEBUG
  ├── SnowConfig.java           // 全局模式设置与调试输出工具（print / printf、isDebug / isRun）
  └── StringEscape.java         // 字符串反转义工具：unescape（含 \uXXXX 支持与容错）
```