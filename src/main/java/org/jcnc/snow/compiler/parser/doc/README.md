# Snow Compiler - Parser 模块

> Snow 编译器的语法分析（Parser）模块 —— 负责把词法单元（Token）转换为抽象语法树（AST）

## 项目简介

**Parser** 是 Snow 编译器前端的核心模块，承担**语法解析**与**AST 构建**的职责。  
它位于词法分析（Lexer）与后续 IR/优化阶段之间，读取 `TokenStream`，以**Pratt 表达式解析**与**工厂分派的语句/顶层解析器**
为骨架，构建结构清晰、便于后续处理的 AST。

本 Parser 模块采用**可扩展的 Parselet 体系**与**工厂派发**的思路：以**AST 节点模型**为中心组织语法树，配合**统一的错误模型
**、**可恢复的解析引擎**与**调试/序列化工具**，形成可扩展、可调试、且便于后续 IR 构建与代码生成的语法层。

## 核心功能

* **统一的 AST 模型**

    * 基础抽象：`Node`、`ExpressionNode`、`StatementNode`、`NodeContext`（行/列/文件）
    * 结构节点：`ModuleNode`、`StructNode`、`FunctionNode`、`ParameterNode`、`ImportNode` 等
    * 语句节点：`DeclarationNode`、`AssignmentNode`、`IfNode`、`LoopNode`、`ReturnNode`、`BreakNode`、`ContinueNode`、
      `ExpressionStatementNode`
    * 表达式节点：`IdentifierNode`、`NumberLiteralNode`、`StringLiteralNode`、`BoolLiteralNode`、`ArrayLiteralNode`、
      `UnaryExpressionNode`、`BinaryExpressionNode`、`CallExpressionNode`、`IndexExpressionNode`、`MemberExpressionNode`、
      `NewExpressionNode`

* **解析引擎与上下文**

    * 引擎：`ParserEngine`（驱动顶层解析；**聚合错误并可同步恢复**）
    * 上下文：`ParserContext`（封装 `TokenStream` 与源信息）
    * Token 读写：`TokenStream`（`peek/next/match/expect`、自动跳过评论/多余换行、提供 EOF 保护）
    * 错误模型：`ParseException`（`MissingToken` / `UnexpectedToken` / `UnsupportedFeature`）与 `ParseError` DTO（统一错误收集与报告）

* **表达式解析体系（Pratt Parser）**

    * 入口：`PrattExpressionParser`，注册**前缀/中缀 Parselet**
    * 前缀：`IdentifierParselet`、`NumberLiteralParselet`、`StringLiteralParselet`、`BoolLiteralParselet`、
      `ArrayLiteralParselet`、`GroupingParselet`、`UnaryOperatorParselet`（`-`/`!`）、`NewObjectParselet`
    * 中缀：`BinaryOperatorParselet`（加/减/乘/除/比较/逻辑/等于…）、`CallParselet`（函数调用）、`IndexParselet`（`[]`）、
      `MemberParselet`（`.`）
    * 优先级：`Precedence`（`LOWEST`→`OR`→`AND`→`EQUALITY`→`COMPARISON`→`SUM`→`PRODUCT`→`UNARY`→`CALL`；**CALL 级别绑定最强
      **）

* **语句解析体系**

    * 统一接口：`StatementParser`
    * 工厂派发：`StatementParserFactory`（根据首关键字选择解析器；默认回退为 `ExpressionStatementParser`）
    * 语句解析器：  
      `DeclarationStatementParser` / `Assignment`（由 `ExpressionStatementParser` 识别）/ `IfStatementParser` /
      `LoopStatementParser` / `ReturnStatementParser` / `BreakStatementParser` / `ContinueStatementParser` /
      `ExpressionStatementParser`
    * 语句工具：`ParserUtils`（匹配区块头尾、跳过多余换行等）、`FlexibleSectionParser`（注册式**可乱序**命名小节解析器，用于
      `function/loop/struct` 等复杂体）

* **顶层/模块/函数/结构体解析**

    * 顶层接口：`TopLevelParser`
    * 顶层工厂：`TopLevelParserFactory`（`module` / `function` 显式注册；其他回退到脚本模式）
    * `ScriptTopLevelParser`：**脚本模式**（无 `module` 时直接解析一条顶层语句，后续由 IR 包装为 `_start`）
    * `ModuleParser`：解析 `module ... end module`（含 `imports/globals/struct/function` 等子区块）
    * `ImportParser`：解析 `import: a, b, c`
    * `FunctionParser`：解析 `function`（`params/returns/body` 三小节，参数/返回/函数体）
    * `StructParser`：解析 `struct`（字段声明、可选 `extends`、构造器 `init`、方法 `method` 等子区块）

* **操作符与优先级/结合性**

    * 二元：`+ - * / %`、比较（`< <= > >=`）、等值（`== !=`）、逻辑（`&& ||`）等，由
      `BinaryOperatorParselet(Precedence, leftAssoc)` 控制优先级与结合性
    * 一元：`-`（取负）、`!`（逻辑非），由 `UnaryOperatorParselet` 解析
    * 访问/调用：成员访问 `.`、下标 `[]`、函数调用 `()`、对象创建 `new`，均以**CALL 级优先级**绑定

* **AST 打印与调试支持**

    * `ASTPrinter`：以缩进树形打印 AST；支持把 `Module/Struct/Function/If/Loop/Expr` 等直观输出
    * `ASTJsonSerializer`：把 AST 序列化为 `Map/List` 结构（便于测试对比、工具联动）
    * `JsonFormatter` / `JSONParser`：JSON 美化与解析（测试/调试辅助）

* **全局辅助**

    * `FlexibleSectionParser`：**注册驱动**的命名小节解析器（`condition+parser` 组合）
    * `ParserUtils`：通用匹配&容错（例如跳过空行、匹配 `end xxx`）
    * `TopLevelParserFactory` / `StatementParserFactory`：**集中注册点**，便于扩展语法

## 模块结构

```
parser/
├── ast/                           // AST 节点定义
│   ├── base/                      // AST 基类：Node / ExpressionNode / StatementNode / NodeContext
│   ├── ...                        // 具体节点：Module / Struct / Function / Parameter / Import
│   ├── ...                        // 语句：Declaration / Assignment / If / Loop / Return / Break / Continue / ExprStmt
│   └── ...                        // 表达式：Identifier / Literals / Unary / Binary / Call / Index / Member / New / Array
│
├── base/
│   └── TopLevelParser.java        // 顶层解析接口
│
├── context/                       // 解析上下文与错误模型
│   ├── ParserContext.java         // 封装 TokenStream + 源信息
│   ├── TokenStream.java           // peek / next / match / expect / 跳过注释&换行
│   ├── ParseException.java        // 解析期异常基类（行/列/原因）
│   ├── ParseError.java            // 错误 DTO（聚合输出）
│   ├── MissingToken.java          // 必需 token 缺失
│   ├── UnexpectedToken.java       // 意外 token
│   └── UnsupportedFeature.java    // 暂未支持的语法
│
├── core/
│   └── ParserEngine.java          // 解析引擎：驱动顶层、聚合错误、同步恢复
│
├── expression/                    // 表达式解析（Pratt）
│   ├── base/                      // ExpressionParser / PrefixParselet / InfixParselet
│   ├── PrattExpressionParser.java // Pratt 解析入口，注册所有前缀/中缀 parselet
│   ├── Precedence.java            // 运算符优先级枚举
│   ├── NumberLiteralParselet.java // 数字字面量
│   ├── StringLiteralParselet.java // 字符串字面量
│   ├── BoolLiteralParselet.java   // 布尔字面量
│   ├── ArrayLiteralParselet.java  // 数组字面量
│   ├── IdentifierParselet.java    // 标识符
│   ├── GroupingParselet.java      // 括号分组 (...)
│   ├── UnaryOperatorParselet.java // -x / !x
│   ├── BinaryOperatorParselet.java// a+b / a\*b / 比较 / 逻辑 / 等值
│   ├── CallParselet.java          // 调用 foo(...)
│   ├── IndexParselet.java         // 下标 a\[b]
│   ├── MemberParselet.java        // 成员 a.b
│   └── NewObjectParselet.java     // new Type(args)
│
├── factory/
│   ├── TopLevelParserFactory.java // 根据关键字分派：module/function；否则脚本模式
│   └── StatementParserFactory.java// 根据关键字分派语句解析器，默认 ExprStmt
│
├── function/
│   ├── FunctionParser.java        // function 解析：params / returns / body
│   └── ASTPrinter.java            // 调试用 AST 打印器（文本/JSON）
│
├── module/
│   ├── ModuleParser.java          // module ... end module（含 imports/globals/struct/function 子区块）
│   └── ImportParser.java          // import: a, b, c
│
├── struct/
│   └── StructParser.java          // struct 定义（字段、可选 extends、init/method 等）
│
├── statement/                     // 语句解析器
│   ├── StatementParser.java
│   ├── DeclarationStatementParser.java
│   ├── ExpressionStatementParser.java
│   ├── IfStatementParser.java
│   ├── LoopStatementParser.java
│   ├── ReturnStatementParser.java
│   ├── BreakStatementParser.java
│   └── ContinueStatementParser.java
│
├── top/
│   └── ScriptTopLevelParser.java  // 脚本模式（无 module）
│
└── utils/                         // 工具集
├── ParserUtils.java               // 匹配 end/跳过空行等通用操作
├── FlexibleSectionParser.java     // 注册式命名小节解析（函数/循环/结构体）
├── ASTJsonSerializer.java         // AST → Map/List（便于测试/调试）
├── JsonFormatter.java             // JSON 美化
└── JSONParser.java                // 简单 JSON 解析（测试/工具链）
```
