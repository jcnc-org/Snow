# Snow Compiler - CLI 模块

> Snow 编程语言的命令行工具 —— 负责解析命令、组织任务编排并委托 pkg 层完成构建/运行/发布等操作

## 项目简介

**CLI（Command Line Interface）** 是 Snow 项目对外的统一入口，提供从初始化项目、解析依赖、编译、打包、运行到发布的一站式指令集。  
该模块以**薄封装/强委托**为设计目标：在 CLI 层完成参数解析、子命令分发与生命周期编排，将实际业务逻辑（编译/运行/打包/依赖解析等）委托给 `pkg` 层的任务与工具执行。

核心入口类 `SnowCLI` 通过注册一组 `CLICommand` 子命令（如 `build` / `compile` / `run` / `publish` 等），统一处理**全局帮助**、**版本信息**、**未知命令兜底**与**退出码约定**，并支持在将来通过 `ServiceLoader`/注册表扩展新的命令。

## 核心功能

* **统一的命令模型**

    * 抽象接口：`CLICommand`（`getName`/`getDescription`/`printUsage`/`execute`）
    * 入口调度：`SnowCLI`（注册命令、解析 argv、分发执行、捕获异常并以退出码退出）
    * 全局帮助与选项：`CLIUtils`（`help/-h/--help` 标志、打印全局使用说明与可用子命令列表；`Option` 记录类型）
    * 版本信息：`VersionUtils#loadVersion` 读取 `version.properties` 中的 `snow.version`，`SnowCLI.SNOW_VERSION` 对外输出

* **命令体系（内置子命令一览）**

    * `version`：输出当前 Snow 工具版本（`VersionCommand`）
    * `init`：在当前目录快速生成 `project.cloud` 示例（`InitCommand`，基于 `ProjectCloudExample`）
    * `install`：解析项目并预下载依赖到本地缓存（`InstallCommand` → `DependencyResolver`）
    * `generate`：依据 `project.cloud` 生成基本目录结构/骨架（`GenerateCommand` → `GenerateTask`）
    * `compile`：编译源代码（可在 Local/Cloud 场景下工作，`CompileCommand` → `CompileTask`）
    * `run`：运行已编译的 VM 字节码（`.water`）（`RunCommand` → `RunTask`）
    * `build`：一键构建（依赖解析 → 编译 → 打包）（`BuildCommand` → `LifecycleManager` + `CompileTask`/`PackageTask`）
    * `clean`：清理构建产物与本地缓存（`CleanCommand` → `CleanTask`）
    * `publish`：将制品发布到远端仓库（`PublishCommand` → `PublishTask`）

* **与 pkg 层的解耦与编排**

    * DSL 解析：`CloudDSLParser` 读取 `project.cloud` 为 `Project` 模型
    * 依赖解析与缓存：`DependencyResolver`（默认缓存目录位于用户主目录 `~/.snow/cache`）
    * 生命周期编排：`LifecycleManager` / `LifecyclePhase` 注册并顺序执行任务（如 `RESOLVE_DEPENDENCIES` / `COMPILE` / `PACKAGE` / `PUBLISH` / `CLEAN` / `INIT` 等）
    * 具体任务：`CompileTask`、`RunTask`、`PackageTask`、`GenerateTask`、`CleanTask`、`PublishTask`（CLI 仅做参数组装与委托）

* **错误处理与退出码约定**

    * 未知命令：打印全局帮助与可用子命令列表
    * `--help`：打印对应子命令使用说明并以 **0** 退出
    * 执行异常：捕获并打印错误信息，以 **1** 退出
    * 正常结束：各命令返回 **0** 表示成功

* **可扩展性与可维护性**

    * 子命令实现无状态、线程安全，便于后续通过**注册表或 `ServiceLoader`** 插拔扩展
    * 入口类集中注册命令，统一管理**命令名到构造器**的映射（`Map<String, Supplier<CLICommand>>`）
    * 通过 `Mode` / `SnowConfig`（导入于 `org.jcnc.snow.common`）为不同环境（如 Local / Cloud）预留模式切换与参数来源

## 模块结构

```
cli/
  ├── SnowCLI.java                 // CLI 入口：解析 argv、注册/分发子命令、处理全局帮助与退出码
  │
  ├── api/
  │   └── CLICommand.java          // 命令抽象接口：name/description/printUsage/execute
  │
  ├── commands/                    // 具体子命令实现（薄封装，委托 pkg 层任务）
  │   ├── BuildCommand.java        // 构建：解析依赖 → 编译 → 打包（LifecycleManager + CompileTask/PackageTask）
  │   ├── CleanCommand.java        // 清理：清除 build 产物与缓存（CleanTask）
  │   ├── CompileCommand.java      // 编译：支持本地/云两种参数来源（CompileTask）
  │   ├── GenerateCommand.java     // 生成：根据 project.cloud 生成骨架（GenerateTask）
  │   ├── InitCommand.java         // 初始化：输出 project.cloud 示例（ProjectCloudExample）
  │   ├── InstallCommand.java      // 安装依赖：解析并预热依赖缓存（DependencyResolver）
  │   ├── PublishCommand.java      // 发布：将制品上传到远程仓库（PublishTask）
  │   ├── RunCommand.java          // 运行：执行 .water 字节码（RunTask）
  │   └── VersionCommand.java      // 版本：打印 snow 版本号
  │
  └── utils/
      ├── CLIUtils.java            // 全局帮助/选项与用法输出（含 Option 记录类型）
      ├── ProjectCloudExample.java // 示例 DSL（project.cloud）模板内容
      └── VersionUtils.java        // 从 version.properties 读取 snow.version
```
