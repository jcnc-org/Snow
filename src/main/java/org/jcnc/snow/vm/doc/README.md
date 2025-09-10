# Snow VM - 虚拟机（指令集 & 执行引擎）模块

> Snow 运行时的栈式虚拟机 —— 负责**装载文本指令**、**解释执行**、并提供**类型化算术/比较、控制流、系统调用与 I/O**等能力

## 项目简介

本模块实现了 Snow 的**指令集虚拟机（VM）**，用于运行由编译器/汇编器生成的“文本化指令序列”。  
它采用**命令（Command）模式 + 统一操作码表（`VMOpCode`）+ 工厂注册（`CommandFactory`）**的结构，配合**运行时栈/局部变量表/调用栈
**与**方法栈帧**，构成清晰可扩展的执行层。

执行流程：`VMLauncher/VMInitializer` 解析指令文件 → `CommandLoader` 读入并清洗文本 → `VirtualMachineEngine` 主循环按 PC
逐条解释执行（跳过空行与 `#` 注释），通过 `CommandExecutionHandler` 分发到各具体指令类，直到 `RET`（根帧）或 `HALT` 正常终止。

## 核心功能

* **统一的指令与分发模型**
    * 抽象接口：`interfaces.Command#execute(String[] parts, int pc, OperandStack, LocalVariableStore, CallStack)`
    * 操作码表：`engine.VMOpCode`（以 `int` 编码，分段保留：**0x0000–0x00DF 类型算术/比较/装载**、**0x00E0–0x00E2 引用操作**、
      **0x0100– 栈操作**、**0x0200– 流程控制**、**0x0300– 寄存器移动**、**0x0400– 系统**）
    * 指令工厂：`factories.CommandFactory` 将 `opCode → Command` 静态注册，`CommandExecutionHandler#handle` 统一查表并调用

* **执行引擎与运行时**
    * 引擎主循环：`engine.VirtualMachineEngine`
        * 维护 **PC**、`OperandStack`（操作数栈）、`LocalVariableStore`（局部变量表）、`CallStack`（调用栈）
        * **根栈帧**：首次执行前 `ensureRootFrame()` 推入；`RET` 在根帧返回 **PROGRAM_END**（`Integer.MAX_VALUE`）以优雅终止
        * 解析规则：每行第一段为**十进制操作码**，后续为操作数；空行与以 `#` 开头的行被忽略
    * 调度与容错：`execution.CommandExecutionHandler` 进行指令分发，捕获异常并保证 VM 安全退出
    * 执行器外壳：`engine.VMCommandExecutor` 封装运行与日志

* **类型化指令族（算术/比较/装载）**
    * **整数/定长**：`byte8`（B\*）、`short16`（S\*）、`int32`（I\*）、`long64`（L\*）
        * 典型操作：`ADD/SUB/MUL/DIV/MOD/NEG/INC`、按位 `AND/OR/XOR`、`PUSH/LOAD/STORE`、比较 `CE/CNE/CG/CGE/CL/CLE`
        * 对应类示例：`IAddCommand`、`LAndCommand`、`BStoreCommand`、`SCECommand`…
    * **浮点**：`float32`（F\*）、`double64`（D\*）
        * 典型操作：`ADD/SUB/MUL/DIV/MOD/NEG/INC`、`PUSH/LOAD/STORE`、比较（无按位操作）
        * 对应类示例：`FAddCommand`、`DSubCommand`、`FLoadCommand`、`DCGECommand`…
    * **类型转换**：`type/conversion` 下提供 **全组合**数值转换（如 `I2L/F2D/D2I/B2S` 等），一律通过 `VMOpCode` 明确定义

* **控制流与调用**
    * **无条件跳转**：`JumpCommand`（`JUMP`）
    * **调用/返回**：`CallCommand`（`CALL`，从栈按**左到右**顺序取实参，建立新 `StackFrame` 并设置返回地址），`RetCommand`（
      `RET`，弹栈帧并回到调用点；根帧返回触发终止）
    * **栈操作**：`PopCommand`（`POP`）、`DupCommand`（`DUP`）、`SwapCommand`（`SWAP`）
    * **寄存器移动**：`MovCommand`（`MOV src,dst`，在同一局部变量表内复制槽位）

* **引用/对象支持**
    * 引用操作：`R_PUSH / R_LOAD / R_STORE`（分别用于将引用压栈、从局部变量表加载/存储引用）
    * 运行时模型：
        * `module.StackFrame`（保存返回地址、`LocalVariableStore`、`OperandStack`、`MethodContext`）
        * `module.CallStack`（带上限保护的调用栈）

* **系统调用与 I/O**
    * 统一入口：`SyscallCommand`（`SYSCALL`），失败时约定**向栈压 `-1`**
    * 文件/网络/控制台子命令（示例）：`PRINT/PRINTLN`、`OPEN/CLOSE/READ/WRITE/SEEK`、`PIPE/DUP`、
      `SOCKET/CONNECT/BIND/LISTEN/ACCEPT`、`SELECT`，以及**数组访问** `ARR_GET/ARR_SET`
    * 文件描述符表：`io.FDTable` 维护 **虚拟 fd → NIO Channel** 映射（内置 `0:stdin / 1:stdout / 2:stderr`）
    * 路径与装载：`io.FilePathResolver`、`io.FileIOUtils` 负责解析参数、读取文本并**去注释/去空行**

* **调试与可视化**
    * 日志工具：`utils.LoggingUtils`
    * 状态输出：`utils.VMUtils#printVMState`、`utils.VMStateLogger`
    * 变量观察：`module.LocalVariableStore#handleMode()` 在 `DEBUG` 模式下（且非 GraalVM native-image）弹出 Swing 面板查看局部变量表
    * 运行入口：`VMLauncher` / `VMInitializer`（示例流程：解析文件 → 载入指令 → 运行 → 打印 VM 状态）

## 模块结构

```
vm/
  ├── VMLauncher.java                 // 入口壳：解析参数，启动 VM
  ├── VMInitializer.java              // 启动流程：读文件 → 执行 → 打印状态
  │
  ├── engine/                         // 执行引擎与操作码
  │   ├── VirtualMachineEngine        // 主循环：维护 PC / 栈 / 局部表 / 调用栈
  │   ├── VMCommandExecutor           // 执行器外壳与异常处理
  │   └── VMOpCode                    // 统一操作码定义（按功能分段编码）
  │
  ├── execution/                      // 指令装载与分发
  │   ├── CommandLoader               // 从文件读取文本指令（去注释/空行）
  │   └── CommandExecutionHandler     // 查表分发并执行 Command
  │
  ├── factories/
  │   └── CommandFactory              // 静态注册：opCode → 指令实例
  │
  ├── interfaces/
  │   └── Command                     // 指令统一接口：execute(...)
  │
  ├── commands/                       // 具体指令实现（Command 模式）
  │   ├── flow/control/               // 控制流：JUMP / CALL / RET
  │   ├── stack/control/              // 栈：POP / DUP / SWAP
  │   ├── register/control/           // 局部槽位移动：MOV
  │   ├── system/control/             // 系统调用/终止：HALT / SYSCALL
  │   ├── ref/control/                // 引用：R_PUSH / R_LOAD / R_STORE
  │   └── type/                       // 类型化算术/比较/装载与转换
  │       ├── control/
  │       │   ├── byte8/              // B_*：8 位有符号整数
  │       │   ├── short16/            // S_*：16 位有符号整数
  │       │   ├── int32/              // I_*：32 位有符号整数
  │       │   ├── long64/             // L_*：64 位有符号整数
  │       │   ├── float32/            // F_*：32 位浮点
  │       │   └── double64/           // D_*：64 位浮点
  │       └── conversion/             // 跨类型转换：X2Y（如 I2L、F2D 等）
  │
  ├── module/                         // 运行时数据结构
  │   ├── OperandStack                // 操作数栈
  │   ├── LocalVariableStore          // 局部变量表（自动扩容/紧凑化）
  │   ├── CallStack                   // 调用栈（含深度保护）
  │   ├── StackFrame                  // 栈帧：返回地址 + 局部表 + 栈 + 方法元信息
  │   └── MethodContext               // 方法上下文（名称/实参，用于调试）
  │
  ├── io/                             // I/O 与文件系统桥接
  │   ├── FDTable                     // 虚拟 fd ↔ NIO Channel 映射（含 0/1/2）
  │   ├── FileIOUtils                 // 读取并清洗指令文本
  │   └── FilePathResolver            // 解析指令文件路径
  │
  └── utils/                          // 通用工具与调试
      ├── LoggingUtils                // 统一日志
      ├── VMUtils                     // 打印 VM 状态等
      └── VMStateLogger               // 状态日志封装
```

