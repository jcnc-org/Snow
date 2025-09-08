# Snow Compiler - IR 模块

> Snow 编译器的中间表示模块 —— 负责构建、组织和输出中间表示指令（IR）

## 项目简介

**IR（Intermediate Representation）** 是 Snow 编译器项目的核心模块，承担中间表示的构建、组织与管理任务。
它用于在前端语法分析与后端目标代码生成之间，提供结构清晰、便于优化和转换的抽象表示形式。

本 IR 模块采用**类 SSA（Static Single Assignment）** 的思路：以**虚拟寄存器**为中心组织数据流，配合*
*统一的操作码集（`IROpCode`）**、**访问者模式（`IRVisitor`）**与**构建器体系**，形成可扩展、可调试、且便于后续优化与代码生成的
IR 层。

## 核心功能

* **统一的中间表示模型**

    * 基础抽象：`IRProgram`、`IRFunction`、`IRInstruction`、`IRValue`
    * 访问者：`IRVisitor`（便于打印、优化、后端生成等多种遍历任务）
* **IR 构建器体系**

    * 顶层：`IRProgramBuilder`（将 AST 根节点集构造成 `IRProgram`）
    * 语句：`StatementBuilder`/`…handlers`（if / loop / return / decl / assign / 表达式语句等）
    * 表达式：`ExpressionBuilder`/`…handlers`（二元/一元、字面量、标识符、调用、索引、成员访问、new/数组字面量等）
    * 上下文：`IRContext`（维护当前函数、作用域与寄存器分配）
* **灵活的指令层级结构**

    * 算术/逻辑：`BinaryOperationInstruction`、`UnaryOperationInstruction`
    * 常量与加载：`LoadConstInstruction`
    * 控制流：`IRLabelInstruction`、`IRJumpInstruction`、`IRCompareJumpInstruction`
    * 调用与返回：`CallInstruction`、`ReturnInstruction`/`IRReturnInstruction`
    * 指令工厂：`InstructionFactory`（统一封装“加载/二元/一元/跳转/移动”等生成流程）
* **寄存器与常量模型**

    * `IRVirtualRegister`（虚拟寄存器，形如 `%0/%1`）
    * `IRConstant`（编译期常量，数字/布尔/字符串/数组等）
    * `IRLabel`（控制流标签）
* **操作码与类型提升**

    * `IROpCode` 定义全量操作码（整数/浮点不同位宽的算术、比较、转换，`LOAD/STORE/CONST`，`JUMP/LABEL/CALL/RET` 等）
    * `IROpCodeMappings`/`ExpressionUtils`/`ComparisonUtils` 负责从 AST 运算符与操作数类型推导到具体 IR
      操作码，并进行必要的类型提升与比较归类
* **IR 打印与调试支持**

    * `IRPrinter`：基于访问者输出 IR（便于调试/测试）
    * `IRFunction#toString`、`IRProgram#toString`：函数/程序级 IR 文本化输出
* **全局辅助**

    * `GlobalConstTable` 支持跨模块编译期常量登记与折叠
    * `GlobalFunctionTable` 记录已知函数及返回类型（便于调用/检查）
    * 语句工具：`ConditionalJump`（统一构建“比较+条件跳转”模式）、`ConstFolder`
    * 表达式工具：`IndexRefHelper`、`TryFoldConst`（尽力在构建期折叠字面量组合）

## 模块结构

```
ir/
  ├── builder/                     // 构建器体系：把 AST 转成 IR
  │   ├── core/                    // 构建期上下文与工厂：IRContext / InstructionFactory / ProgramBuilder / Scope
  │   ├── expression/              // 表达式构建主入口与接口
  │   ├── handlers/                // 表达式处理器：二元/一元/调用/字面量/标识符/索引/成员/new/数组等
  │   ├── statement/               // 语句构建：FunctionBuilder / StatementBuilder / 上下文
  │   │   ├── handlers/            // 语句处理器：If / Loop / Return / Declaration / Assignment / IndexAssignment / Break / Continue / ExprStmt
  │   │   └── utils/               // 条件跳转模式、常量折叠等
  │   └── utils/                   // 构建期的通用小工具（索引帮助、尝试常量折叠等）
  │
  ├── common/                      // 全局表：常量/函数元信息登记（编译期使用）
  │
  ├── core/                        // IR 核心抽象与通用设施
  │   ├── IRProgram / IRFunction   // 程序/函数容器与文本输出
  │   ├── IRInstruction            // 指令抽象基类
  │   ├── IRValue                  // 值抽象（由常量/寄存器/标签等实现）
  │   ├── IRVisitor                // 访问者接口（打印/优化/后端遍历）
  │   ├── IROpCode                 // 全量操作码定义（算术/比较/转换/内存/控制流/调用返回等）
  │   ├── IROpCodeMappings         // 运算符到 IROpCode 的映射表（按位宽分类）
  │   └── IRPrinter                // 访问者实现：打印 IR
  │
  ├── instruction/                      // 具体 IR 指令类型
  │   ├── BinaryOperationInstruction    // 二元运算：dest = a (op) b
  │   ├── UnaryOperationInstruction     // 一元运算：dest = (op) a
  │   ├── LoadConstInstruction          // 常量加载：dest = const
  │   ├── IRLabelInstruction            // 标签定义：Lx:
  │   ├── IRJumpInstruction             // 无条件跳转：jump Lx
  │   ├── IRCompareJumpInstruction      // 比较+条件跳转：cmp a,b -> Lx
  │   ├── CallInstruction               // 函数调用
  │   ├── ReturnInstruction             // 函数返回（可 void/可带返回值）
  │   └── IRReturnInstruction           // 返回指令的变体（兼容用途）
  │
  ├── utils/                       // IR 构建期/选择期的通用工具
  │   ├── ExpressionUtils          // 运算符解析、类型提升、从 AST 选择 IROpCode
  │   ├── ComparisonUtils          // 不同位宽/类型的比较操作分类与归并
  │   └── IROpCodeUtils            // 比较操作码求反等辅助
  │
  └── value/                       // 值模型：在指令里的操作数
      ├── IRVirtualRegister        // 虚拟寄存器：%0、%1…
      ├── IRConstant               // 常量值：数字/布尔/字符串/数组等
      └── IRLabel                  // 控制流标签：Lx
```