# Snow Build - 包管理模块

> Snow 构建工具中的 **pkg** 子系统 —— 负责解析 `.cloud` 配置、解析依赖、 orchestrate 构建生命周期并执行各阶段任务

## 项目简介

**包管理模块（pkg）** 是 Snow 构建工具的关键组成部分，承担“从配置到产物” 的整条流水线：

1. **DSL 解析**：读取并解析 `.cloud` 配置文件，生成统一的项目/依赖/构建配置模型；
2. **生命周期编排**：按 *INIT → RESOLVE\_DEPENDENCIES → COMPILE → PACKAGE → PUBLISH → CLEAN* 的顺序驱动构建流程；
3. **依赖解析与缓存**：按需下载缺失依赖并存入本地缓存，离线优先；
4. **任务执行**：在各生命周期阶段调用对应 `Task` 实现（清理、编译、打包、发布等）；
5. **配置模板展开**：支持在配置中使用 `@{key}` 形式的占位符，并在构建前统一替换。

整个模块强调 **可扩展性** 与 **内聚职责**：DSL → Model、Lifecycle → Task、Resolver → Repository 各自解耦，可独立演进。

## 核心功能

| 功能              | 关键类                                                      | 说明                                              |
|-----------------|----------------------------------------------------------|-------------------------------------------------|
| **CloudDSL 解析** | `CloudDSLParser`                                         | 支持区块语法、嵌套 build 展平、注释过滤，解析为扁平 `Map`             |
| **模型对象**        | `Project / Dependency / Repository / BuildConfiguration` | 只读数据类，提供静态工厂方法 *fromFlatMap()*                  |
| **生命周期管理**      | `LifecycleManager`, `LifecyclePhase`                     | 注册并顺序执行阶段任务，阶段可扩展                               |
| **任务体系**        | `Task` + `*Task` 实现                                      | Clean / Compile / Package / Publish 四大内置任务，易于新增 |
| **依赖解析器**       | `DependencyResolver`                                     | 支持本地缓存、HTTP 下载、URI 解析、断点续传（基于 NIO）              |
| **模板变量替换**      | `BuildConfiguration`                                     | 在加载阶段把 `@{key}` 替换为外部属性值                        |

## 模块结构

```
pkg/
  ├── dsl/
  │   └── CloudDSLParser.java        // .cloud DSL 解析
  ├── lifecycle/
  │   ├── LifecycleManager.java      // 阶段编排
  │   └── LifecyclePhase.java        // 阶段枚举
  ├── model/
  │   ├── Project.java               // 项目信息
  │   ├── BuildConfiguration.java    // 构建配置（支持变量）
  │   ├── Dependency.java            // 依赖描述
  │   └── Repository.java            // 仓库描述
  ├── resolver/
  │   └── DependencyResolver.java    // 依赖解析与缓存
  └── tasks/
      ├── Task.java                  // 任务接口
      ├── CleanTask.java             // 清理
      ├── CompileTask.java           // 编译
      ├── PackageTask.java           // 打包
      └── PublishTask.java           // 发布
```

## 典型流程概览

1. 解析配置

2. 注册任务并执行

3. 解析依赖（在 RESOLVE\_DEPENDENCIES 阶段内部）

## 开发环境

* JDK 24 或更高版本
* Maven 构建管理
* 推荐 IDE：IntelliJ IDEA