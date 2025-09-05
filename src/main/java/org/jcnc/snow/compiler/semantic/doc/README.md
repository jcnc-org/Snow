# Snow Compiler - 语义分析（Semantic）模块

> Snow 编译器的语义分析层 —— 负责 **AST** 的类型检查、符号/模块/函数签名管理与语义错误收集

## 项目简介

**语义分析（Semantic Analysis）** 是 Snow 编译器在语法之后、IR 之前的关键阶段。
该模块在遍历 AST 的同时，完成**类型推断与检查**、**符号与作用域解析**、**模块/结构体/函数签名登记**、以及**错误汇总与报告**，为后续 IR 生成与优化提供可靠、结构化的类型与符号信息。

语义层采用“**注册表 + 访问器接口 + 分析器实现**”的可插拔设计：以 `AnalyzerRegistry` 为分发中心、`ExpressionAnalyzer`/`StatementAnalyzer` 为契约、配套 **模块/函数签名三阶段流程** 与 **类型系统**，形成清晰、易扩展、便于调试的语义分析框架。

## 核心功能

* **顶层调度与三阶段流程**

    * `SemanticAnalyzer`：语义分析总控，按阶段运行

        1. **模块注册** `ModuleRegistry#registerUserModules`
        2. **签名登记** `SignatureRegistrar#register`（函数/方法/构造函数）
        3. **函数体检查** `FunctionChecker#check`
    * `SemanticAnalyzerRunner`：对外统一入口（从 AST 过滤 `ModuleNode`，执行语义并输出报告）

* **分析器体系（可插拔）**

    * 注册中心：`AnalyzerRegistry`（基于 AST 节点类型分发到对应分析器，含兜底 `UnsupportedExpressionAnalyzer`）
    * 基础契约：`ExpressionAnalyzer`、`StatementAnalyzer`
    * 注册器：`AnalyzerRegistrar`（一次性注册全部语句/表达式分析器）

* **表达式分析器（部分列举）**

    * 字面量：`NumberLiteralAnalyzer`、`StringLiteralAnalyzer`、`BoolLiteralAnalyzer`、`ArrayLiteralAnalyzer`
    * 变量与成员：`IdentifierAnalyzer`、`MemberExpressionAnalyzer`（支持 `模块.成员` 与 `结构体.字段/方法`）
    * 计算与取值：`BinaryExpressionAnalyzer`、`UnaryExpressionAnalyzer`、`IndexExpressionAnalyzer`
    * 调用与构造：`CallExpressionAnalyzer`（支持 模块.函数 / 结构体方法 / 普通函数）、`NewExpressionAnalyzer`
    * 兜底：`UnsupportedExpressionAnalyzer`

* **语句分析器（部分列举）**

    * 声明与赋值：`DeclarationAnalyzer`、`AssignmentAnalyzer`、`IndexAssignmentAnalyzer`
    * 控制流：`IfAnalyzer`、`LoopAnalyzer`、`ReturnAnalyzer`、`BreakAnalyzer`、`ContinueAnalyzer`
    * 表达式语句：对 `ExpressionStatementNode` 直接转发到表达式分析器

* **符号与作用域模型**

    * `Symbol` / `SymbolKind` / `SymbolTable`（链式父子作用域；`resolve` 向上查找；`define` 当前层定义）
    * `ModuleInfo`：每个模块的元信息（**imports**、**functions**、**structs**、**globals**）
    * `ModuleRegistry`：将用户模块写入全局上下文以供跨模块解析

* **类型系统与类型提升**

    * 核心接口：`Type`（`isCompatible`/`isNumeric`/`name`/`widen`）
    * 具体类型：`BuiltinType`（byte/short/int/long/float/double/string/boolean/void）、`ArrayType`、`FunctionType`、`StructType`
    * 内建注册：`BuiltinTypeRegistry`（注册所有内置基本类型；示例内置模块 `os` 及函数 `syscall(string,int):void`）
    * 类型解析：`Context#parseType` 支持 **多维数组**（如 `int[][]`）、**结构体类型**（`模块.结构体`）
    * 数值宽化：在声明/赋值/调用/构造场景支持 **数值类型自动宽化**（`Type.widen`）

* **模块/结构体/函数签名登记**

    * `SignatureRegistrar`：遍历模块，登记**结构体**（字段/方法/父类）、**自由函数**、**构造函数**的 `FunctionType`
    * 跨模块访问：`MemberExpressionAnalyzer`/`CallExpressionAnalyzer` 结合 `ModuleInfo#imports` 做**导入校验**

* **常见语义规则（内置检查）**

    * **If/Loop 条件**：必须为 `boolean`（`TypeUtils.isLogic` 定义的逻辑约束）
    * **声明初始化/赋值**：类型**兼容或可宽化**，否则报错
    * **返回语句**：返回值与函数签名一致；**非 void 函数**必须至少出现一条 `return`
    * **数组**：字面量元素类型需一致；`arr[index]` 的 `index` 必须是**数值类型**；下标赋值需与元素类型一致
    * **成员与调用解析**：支持
      `变量.字段`/`变量.方法`（结构体实例）
      `模块.常量/变量/函数`（需导入或同模块）
      普通函数名解析（当前作用域/模块）

* **错误报告与调试**

    * `SemanticError`：携带源位置信息（文件/行/列），友好 `toString` 文本
    * `SemanticAnalysisReporter`：统一汇总打印；可配置**仅打印**或**打印并退出**
    * `Context#log(boolean verbose)`：按需输出细粒度调试日志

## 模块结构

```
semantic/
  ├── analyzers/                        // 语义分析器体系：注册与分发
  │   ├── AnalyzerRegistry.java         // AST 节点类型 -> 分析器 实例映射（含兜底）
  │   ├── TypeUtils.java                // 语义层类型工具（逻辑/布尔判定等）
  │   ├── base/
  │   │   ├── ExpressionAnalyzer.java   // 表达式分析器接口
  │   │   └── StatementAnalyzer.java    // 语句分析器接口
  │   ├── expression/                   // 各类表达式分析器
  │   │   ├── NumberLiteralAnalyzer.java
  │   │   ├── StringLiteralAnalyzer.java
  │   │   ├── BoolLiteralAnalyzer.java
  │   │   ├── IdentifierAnalyzer.java
  │   │   ├── BinaryExpressionAnalyzer.java
  │   │   ├── UnaryExpressionAnalyzer.java
  │   │   ├── ArrayLiteralAnalyzer.java
  │   │   ├── IndexExpressionAnalyzer.java
  │   │   ├── MemberExpressionAnalyzer.java
  │   │   ├── CallExpressionAnalyzer.java
  │   │   ├── NewExpressionAnalyzer.java
  │   │   └── UnsupportedExpressionAnalyzer.java
  │   └── statement/                    // 各类语句分析器
  │       ├── DeclarationAnalyzer.java
  │       ├── AssignmentAnalyzer.java
  │       ├── IndexAssignmentAnalyzer.java
  │       ├── IfAnalyzer.java
  │       ├── LoopAnalyzer.java
  │       ├── ReturnAnalyzer.java
  │       ├── BreakAnalyzer.java
  │       └── ContinueAnalyzer.java
  │
  ├── core/                             // 顶层流程与全局上下文
  │   ├── SemanticAnalyzerRunner.java   // 语义分析入口：筛选模块节点并运行
  │   ├── SemanticAnalyzer.java         // 三阶段总控（注册模块/登记签名/函数检查）
  │   ├── Context.java                  // 全局上下文：模块表、错误表、日志、类型解析
  │   ├── ModuleRegistry.java           // 模块注册（写入 Context#modules）
  │   ├── SignatureRegistrar.java       // 函数/方法/构造签名登记
  │   ├── FunctionChecker.java          // 函数体遍历与作用域/return 检查
  │   ├── ModuleInfo.java               // 模块元信息（imports/functions/structs/globals）
  │   ├── BuiltinTypeRegistry.java      // 内置类型/内置模块与函数（如 os.syscall）
  │   └── AnalyzerRegistrar.java        // 将全部分析器注册进 AnalyzerRegistry
  │
  ├── symbol/
  │   ├── Symbol.java                   // 符号项（name/type/kind）
  │   ├── SymbolKind.java               // 符号分类（VARIABLE/CONSTANT/FUNCTION/MODULE）
  │   └── SymbolTable.java              // 链式作用域：define/resolve
  │
  ├── type/
  │   ├── Type.java                     // 类型接口 + 辅助（isCompatible/isNumeric/widen/name）
  │   ├── BuiltinType.java              // 内置基础类型（整型/浮点/字符串/布尔/void）
  │   ├── ArrayType.java                // 数组类型（支持多维）
  │   ├── FunctionType.java             // 函数类型（参数列表 + 返回类型）
  │   └── StructType.java               // 结构体类型（字段/方法/父类/成员解析）
  │
  ├── error/
  │   └── SemanticError.java            // 语义错误模型（含源位置）
  │
  └── utils/
      └── SemanticAnalysisReporter.java // 错误汇总打印与退出策略
```

## 设计要点

* **阶段化**：将“全局签名收集”和“函数体检查”解耦，避免前向引用/跨模块依赖导致的顺序问题。
* **作用域安全**：`SymbolTable` 通过父指针实现块级/函数级/模块级多层作用域；`If`/`Loop` 分支各自使用独立作用域。
* **解析顺序**：成员与调用遵循“先模块、后结构体、再本地符号”的解析策略，结合 `imports` 做跨模块访问控制。
* **类型宽化**：对数值类型在声明/赋值/调用/构造时按 `Type.widen` 进行安全宽化；字符串调用形参支持从数值到 string 的隐式接收（见 `CallExpressionAnalyzer`）。

## 扩展

新增语法节点仅需

    1. 实现对应 `ExpressionAnalyzer`/`StatementAnalyzer`，
    2. 在 `AnalyzerRegistrar#registerAll` 中注册，
    3. 必要时在 `SignatureRegistrar` 扩展签名收集逻辑。