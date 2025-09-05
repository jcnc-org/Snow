# Snow Compiler - Lexer 模块

> Snow 编译器的词法分析（Lexing）模块 —— 把源代码字符流切分为语义化的 Token 序列，为后续语法分析/IR 构建打好基础

## 项目简介

**Lexer（词法分析器）** 负责把源代码转换为一串结构化的 **Token**。  
本模块采用“**扫描器链（Chain of Scanners）+ 状态机（FSM）**”的实现思路：由统一的 `LexerEngine` 驱动多种 `TokenScanner`
子类（数字/字符串/注释/运算符/符号等），配合可扩展的 `TokenFactory`（关键字与类型识别）与 `LexerContext`（字符流与位置信息管理），形成
**高内聚、易扩展、便于调试**的词法层。

## 核心功能

* **统一的 Token 模型**
    * 基础抽象：`Token`（类型/词素/原文/行列）、`TokenType`（全部词法类别枚举）
    * 工厂识别：`TokenFactory`（根据词素自动识别 `TYPE/KEYWORD/IDENTIFIER/BOOL_LITERAL` 等）
        * 关键字：
          `module/function/params/returns/body/end/if/then/else/loop/declare/return/import/init/cond/step/globals/break/continue/const/new/extends`
        * 内置类型：`int/string/float/boolean/void/double/long/short/byte`
        * 标识符判定：`[A-Za-z_][A-Za-z0-9_]*`
* **扫描器体系（FSM/模板方法）**
    * 接口与基类：`TokenScanner`、`AbstractTokenScanner`（封装通用扫描/读流工具）
    * 具体扫描器：
        * `WhitespaceTokenScanner` —— 跳过空白（不含换行）
        * `NewlineTokenScanner` —— 把 `\n` 作为 `NEWLINE` 记号
        * `CommentTokenScanner` —— `//` 单行、`/* ... */` 多行注释（FSM）
        * `NumberTokenScanner` —— 十进制整数/小数，支持**单字符类型后缀**：`b/s/l/f`（大小写均可），如 `2.0f`、`255B`
        * `IdentifierTokenScanner` —— 识别标识符并交由 `TokenFactory` 判定是否关键字/类型/布尔字面量
        * `StringTokenScanner` —— 双引号字符串，支持转义 `\" \\ \n \t` 等（FSM，保留原文）
        * `OperatorTokenScanner` —— `= == != > >= < <= && || %` 等
        * `SymbolTokenScanner` —— `: , . + - * / ( ) [ ]`
        * `UnknownTokenScanner` —— 兜底，遇到非法字符抛出 `LexicalException`
* **引擎与校验**
    * `LexerEngine`：驱动扫描器链；捕获 `LexicalException`→记录为 `LexicalError`；末尾补 `EOF`
    * **健壮性处理**：`skipInvalidLexeme()` 遇到 `1abc` 等错误片段时**一次性吞掉残余**，避免产生连锁误报
    * **轻量规则校验**（`validateTokens()`）：
        * `declare` 后必须紧跟**合法标识符**（允许可选 `const`）
        * `declare` 后**禁止出现第二个多余的标识符**
* **上下文与定位**
    * `LexerContext`：统一换行为 `\n`；1-based 行/列；`advance/peek/peekNext/peekAhead(n)/match(ch)`；跟踪 `lastCol`
* **错误与报告**
    * `LexicalException`（抛出于扫描阶段，禁止堆栈，专注单行错误描述）
    * `LexicalError`（文件/行/列/消息，统一格式：`file:///abs/path:line:col: message`）
    * `LexerEngine#report(errors)`：汇总输出
* **打印与调试**
    * `TokenPrinter`：表格化打印 `line/col/type/lexeme`，对换行/制表做可视化转义；遇 `NEWLINE` 额外空行

## 词法细则（Token 一览）

* **标识与关键字**
    * `IDENTIFIER`：`[A-Za-z_][A-Za-z0-9_]*`
    * `KEYWORD`：见上“关键字”
    * `TYPE`：见上“内置类型”
* **字面量**
    * `BOOL_LITERAL`：`true`/`false`
    * `STRING_LITERAL`：双引号字符串，支持转义 `\" \\ \n \t` 等
    * `NUMBER_LITERAL`：十进制整数/小数，允许后缀 `b/s/l/f`（大小写）
* **符号/运算符**
    * 标点/括号：`COLON(:), COMMA(,), DOT(.), LPAREN(()), RPAREN()), LBRACKET([), RBRACKET(])`
    * 算术：`PLUS(+), MINUS(-), MULTIPLY(*), DIVIDE(/), MODULO(%)`
    * 比较/逻辑：
      `EQUALS(=), DOUBLE_EQUALS(==), NOT(!), NOT_EQUALS(!=), GREATER_THAN(>), GREATER_EQUAL(>=), LESS_THAN(<), LESS_EQUAL(<=), AND(&&), OR(||)`
* **其它**
    * `NEWLINE`：换行
    * `COMMENT`：`//...` 或 `/* ... */`
    * `EOF`：文件结束
    * `UNKNOWN`：无法识别的字符序列（立即报错）

## 模块结构

```
lexer/
  ├── base/
  │   └── TokenScanner.java          // 扫描器接口：canHandle/handle
  │
  ├── core/
  │   ├── LexerContext.java          // 字符流与位置信息；advance/peek/match/peekAhead
  │   ├── LexerEngine.java           // 引擎：扫描器链→Token流；错误捕获与轻量校验；getAllTokens/getErrors
  │   ├── LexicalError.java          // 词法错误模型（文件/行/列/消息）
  │   └── LexicalException.java      // 词法异常（无堆栈，单行描述）
  │
  ├── scanners/
  │   ├── AbstractTokenScanner.java  // 模板方法与工具：readWhile 等
  │   ├── WhitespaceTokenScanner.java// 跳过空白（非换行）
  │   ├── NewlineTokenScanner.java   // 识别 NEWLINE
  │   ├── CommentTokenScanner.java   // // 与 /* */ 注释（FSM）
  │   ├── NumberTokenScanner.java    // 数字/小数/后缀（FSM, 后缀集 "bslfBSLF"）
  │   ├── IdentifierTokenScanner.java// 标识符→交由 TokenFactory 归类
  │   ├── StringTokenScanner.java    // 双引号字符串与转义（FSM，保留原文）
  │   ├── OperatorTokenScanner.java  // = == != > >= < <= && || % 等
  │   ├── SymbolTokenScanner.java    // : , . + - * / ( ) [ ]
  │   └── UnknownTokenScanner.java   // 兜底：抛 LexicalException
  │
  ├── token/
  │   ├── Token.java                 // Token(type, lexeme, raw, line, col)；Token.eof(line)
  │   ├── TokenFactory.java          // 关键字/类型/布尔判定；create(raw,line,col)
  │   └── TokenType.java             // 全量 Token 枚举
  │
  └── utils/
      └── TokenPrinter.java          // 调试打印：line/col/type/lexeme
```

## 处理流程

1. **构造引擎**：`LexerEngine(source, sourceName)`
2. **扫描**：按扫描器链顺序尝试（空白→换行→注释→数字→标识符→字符串→运算符→符号→未知）
3. **异常转错误**：扫描器抛出的 `LexicalException` 被捕获为 `LexicalError`，并**跳过错误词素残余**
4. **补 EOF**：在 Token 流末尾追加 `EOF`
5. **轻量校验**：对 `declare` 语句做就地校验（可选 `const` + 单一标识符）
6. **可选打印**：调试模式下使用 `TokenPrinter` 输出
7. **结果获取**：`getAllTokens()` / `getErrors()`

## 扩展与定制

* **新增关键字/类型**：修改 `TokenFactory` 中 `KEYWORDS/TYPES` 集合
* **新增运算符**：在 `OperatorTokenScanner`/`SymbolTokenScanner` 中扩展 `switch` 分支
* **数字后缀**：`NumberTokenScanner` 的 `SUFFIX_CHARS` 可按需扩展
* **自定义校验**：在 `LexerEngine#validateTokens()` 中添加新的轻量规则
* **新增扫描器**：继承 `AbstractTokenScanner` 并在 `LexerEngine` 的扫描器链中插入（顺序很关键，通常从“更具体/更长”到“更通用/兜底”）
