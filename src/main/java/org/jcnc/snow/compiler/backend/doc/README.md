# Snow Compiler - Backend（IR→VM）模块

> Snow 编译器的后端代码生成模块 —— 负责把 IR 翻译为 Snow虚拟机（Snow VM）的指令序列

## 项目简介

**Backend（IR→VM）** 模块承接 IR 层产物，完成**寄存器分配**、**操作码映射与类型提升**、**指令选择与输出组装**，最终生成可在 Snow VM 上执行的指令流。
模块采用“**指令生成器（InstructionGenerator）+ 注册表**”的可插拔架构：每种 IR 指令对应一个生成器；`VMCodeGenerator` 负责分发与调度；`VMProgramBuilder` 负责落盘（emit）、标签/调用回填（fixup）与函数边界管理。

## 核心功能

* **端到端的 IR→VM 翻译流水线**

    * `RegisterAllocator`：为 `IRVirtualRegister` 分配 VM 局部槽位（线性扫描，顺序分配）
    * `VMCodeGenerator`：按函数遍历 IR 指令，分派到具体生成器并统一收尾（`main` → `HALT`，其它 → `RET`）
    * `VMProgramBuilder`：集中管理 **emit/标签/地址表/分支回填/调用回填**，以及槽位类型标注

* **指令生成器体系（Instruction Generators）**

    * 抽象接口：`InstructionGenerator<T extends IRInstruction>`
    * 注册表：`InstructionGeneratorProvider#defaultGenerators()` 一次性集中暴露默认实现
    * 已内置的生成器：

        * 算术/逻辑：`BinaryOpGenerator`、`UnaryOpGenerator`
        * 常量加载：`LoadConstGenerator`（支持**字符串转义**、**数组/嵌套数组**、`f/L` 等**类型后缀**）
        * 控制流：`LabelGenerator`、`JumpGenerator`、`CmpJumpGenerator`（比较+条件跳转，含前缀对齐与类型提升）
        * 调用与返回：`CallGenerator`（含 **syscall 子命令** 与 **\_*setindex** 特例*\*）、`ReturnGenerator`

* **寄存器与常量模型对接**

    * 槽位映射：`Map<IRVirtualRegister, Integer>`（由 `RegisterAllocator` 产出）
    * 槽位类型：`VMProgramBuilder#setSlotType/getSlotType` 维护 `'B/S/I/L/F/D/R'` 类型前缀
    * 字符串常量池：`CallGenerator#registerStringConst(regId, value)` 用于从寄存器实参恢复 syscall 子命令等

* **操作码映射与类型提升**

    * `IROpCodeMapper`：将 IR 的 `IROpCode` 映射成 VM 的**助记名**（如 `I_ADD/I_CE/...`）
    * `OpHelper`：把助记名安全转换为 **VM 实际 opcode 数值**（与 `VMOpCode` 常量同步，脚本生成）
    * `TypePromoteUtils`：数值类型**提升与转换**（`promote/convert/str`），比较前缀自动矫正（如 `I_C* ↔ L_C*`）

* **输出与回填（Fixup）机制**

    * **调用回填**：`emitCall(target, nArgs)` 支持三态 —— 绝对地址 / 虚调用（`@Class::method`）/ 占位回填
    * **分支回填**：`emitBranch(op, label)` 先占位，`registerLabel` 后统一修补
    * **名称解析**：支持全名/简名唯一匹配与**继承层级**的成员解析（参见 `patchRemainingFixesByInheritance`）

* **小型 Peephole 优化**

    * `BinaryOpGenerator` 识别 `ADD_*` 的 `+0` 情形并降解为 **MOV/STORE**（避免无效运算）
    * 比较指令统一产出 **0/1** 到目标槽位（`true/false` 归一为 `I` 型）

## 模块结构

```
backend/
  ├── alloc/                        // 寄存器分配
  │   └── RegisterAllocator.java    // 线性扫描：为 IR 虚拟寄存器分配 VM 槽位
  │
  ├── builder/                      // 后端组装与输出
  │   ├── VMCodeGenerator.java      // 按函数驱动生成器，统一收尾（HALT/RET）
  │   └── VMProgramBuilder.java     // emit/标签/地址表/分支与调用回填/槽位类型
  │
  ├── core/
  │   └── InstructionGenerator.java // 生成器接口：supportedClass()/generate(...)
  │
  ├── generator/                    // 各类 IR 指令 → VM 指令的具体生成器
  │   ├── InstructionGeneratorProvider.java // 默认生成器注册表
  │   ├── BinaryOpGenerator.java    // 二元：算术/比较（含 +0 消解、bool 结果归一）
  │   ├── UnaryOpGenerator.java     // 一元：NEG/NOT 等（按槽位前缀选择 LOAD/STORE）
  │   ├── LoadConstGenerator.java   // 常量/数组/字符串转义与类型后缀
  │   ├── LabelGenerator.java       // 标签落点
  │   ├── JumpGenerator.java        // 无条件跳转
  │   ├── CmpJumpGenerator.java     // 比较+条件跳转（类型提升、前缀矫正）
  │   ├── CallGenerator.java        // 普通调用 / 虚调用 / syscall / __setindex_*
  │   └── ReturnGenerator.java      // 返回（main→HALT，其它→RET）
  │
  └── utils/
      ├── IROpCodeMapper.java       // IROpCode → 助记名
      ├── OpHelper.java             // 助记名 → VMOpCode 数值（自动生成映射）
      └── TypePromoteUtils.java     // 类型提升/显式转换/前缀字符工具
```

## 生成流程

1. **寄存器分配**：对每个 `IRFunction` 调用 `RegisterAllocator#allocate(fn)` → 产出 `slotMap`。
2. **创建输出器**：`VMProgramBuilder out = new VMProgramBuilder()`。
3. **构建代码生成器**：`VMCodeGenerator gen = new VMCodeGenerator(slotMap, out, InstructionGeneratorProvider.defaultGenerators())`。
4. **逐函数生成**：`gen.generate(fn)`（内部：遍历 IR 指令 → 分派到对应 `InstructionGenerator` → 结束处自动发出 `HALT/RET`）。
5. **收集结果**：`out.build()` / `out.getCode()` 取得最终 VM 指令序列（`String` 列表），可直接喂给 VM 或写出文本。

## 扩展指南

* **新增 IR 指令类型**

    1. 定义生成器类并实现 `InstructionGenerator<T>`；
    2. 在 `InstructionGeneratorProvider#defaultGenerators()` 注册；
    3. 如需新助记名，补充 `IROpCodeMapper` 与（若新增 VM 指令）`VMOpCode`→`OpHelper` 的映射。

* **支持新的数值类型或转换**

    * 在 `TypePromoteUtils` 中补充优先级/转换表与前缀字符；
    * 根据需要在 `CmpJumpGenerator`/`UnaryOpGenerator` 中处理加载与前缀对齐。

* **自定义调用约定**

    * 扩展 `VMProgramBuilder#emitCall` 或在 `CallGenerator` 中增加分支（例如内建函数、运行时服务等）。