<p align="center">
<img src="docs/README/IMG/icon/IMG_Snow.svg" alt="Snow Icon" width="125" height="125">
<h1 align="center" style="margin: 30px 0 30px; font-weight: bold;">Snow编程语言</h1>

<p align="center">
  <a href="https://gitee.com/jcnc-org/snow/stargazers">
    <img src="https://gitee.com/jcnc-org/snow/badge/star.svg?theme=dark"
         alt="Gitee Star" height="20">
  </a>
  <a href="https://gitcode.com/jcnc-org/snow">
    <img src="https://gitcode.com/jcnc-org/snow/star/badge.svg"
         alt="GitCode Star" height="20">
  </a>
</p>


<p align="center">
    <a href="https://gitee.com/jcnc-org/snow/blob/main/LICENSE">
        <img src="https://img.shields.io/badge/%20license-Apache--2.0%20-blue" alt="">
    </a>
    <a href="https://gitee.com/jcnc-org/snow/tree/v0.13.0/">
        <img src="https://img.shields.io/badge/version-v0.13.0-blue" alt="">
    </a>
</p>



<p align="center">
    <a href="https://gitee.com/jcnc-org/snow/releases">
        <img src="https://img.shields.io/badge/Windows-Passing-49%2C198%2C84.svg?style=falt&logo=Windows" alt="">
    </a>
    <a href="https://gitee.com/jcnc-org/snow/releases">
        <img src="https://img.shields.io/badge/Ubuntu-Passing-49%2C198%2C84.svg?style=falt&logo=Ubuntu" alt="">
    </a>
    <a href="https://gitee.com/jcnc-org/snow/releases">
        <img src="https://img.shields.io/badge/MacOS-Passing-49%2C198%2C84.svg?style=falt&logo=Apple" alt="">
    </a>
</p>

## 项目简介

**Snow** 是一门面向 AI 时代的新型编程语言，灵感源自大模型（LLM）的发展趋势。它的设计初衷是让 LLM 更容易生成和理解代码，从而提升人与 AI 协同编程的效率。

该项目完整实现了 Snow 语言的编译流程，包括 **词法分析、语法分析、语义分析、中间表示（IR）生成**，以及最终的 **虚拟机（VM）指令生成与执行**。通过这一完整的编译-执行链路，开发者可以将 `.snow` 源文件编译为 `.water` 虚拟机指令，并直接在 **SnowVM** 上运行。

从源码编译、构建管理、依赖管理、项目标准化、可视化调试面板到原生镜像发布，全部由 Snow 官方工具完成，降低学习与集成成本。



## 集成开发环境

Snow 专属 IDE — **IDEology**

仓库地址：[https://gitee.com/jcnc-org/IDEology](https://gitee.com/jcnc-org/IDEology)

Snow IntelliJ IDEA 插件

仓库地址：[https://gitee.com/jcnc-org/snow-intelli-j](https://gitee.com/jcnc-org/snow-intelli-j)


> 目前 IDEology 尚未提供正式发行版，开发者可通过源码自行编译获得使用体验。

![IMG_IDE_5.png](docs/README/IMG/IMG_IDE_5.png)

## 背景理念

Snow 语言受到 LLM 驱动代码生成趋势的启发,强调简单而清晰的语法和严格的类型系统,以帮助 LLM 更好地理解程序。


相关背景: [心路历程](docs/Snow-Lang-Journey/Snow-Lang-Journey.md)


## Snow-Lang 官网

[https://snow-lang.com](https://snow-lang.com)

## 下载 Snow 发行版

[https://gitee.com/jcnc-org/snow/releases](https://gitee.com/jcnc-org/snow/releases)

### Snow SDK 目录结构

Snow SDK 包含以下目录结构：

```
SnowSDK/
├── bin/           # 可执行文件目录
└── lib/           # 标准库目录
    ├── os/        # 操作系统相关库
    ├── std/       # 标准库
    └── syscall/   # 系统调用库
```

### Arch Linux 安装

- 通过 [AUR 仓库](https://aur.archlinux.org/packages/snow)或[自建源仓库](https://github.com/taotieren/aur-repo)安装 `snow` 发行版。

```bash
# AUR
yay -Syu snow
# 或自建源
sudo pacman -Syu snow
# 或安装包组
sudo pacman -Syu snow-lang
```

- 通过 [AUR 仓库](https://aur.archlinux.org/packages/snow-git)或[自建源仓库](https://github.com/taotieren/aur-repo)安装 `snow-git` 开发版。

```bash
# AUR
yay -Syu snow-git
# 或自建源
sudo pacman -Syu snow-git
# 或安装包组
sudo pacman -Syu snow-lang-git
``` 

## 相关文档
[Snow-Lang 指南](docs/Snow-Lang-Syntax/Snow-Lang-Syntax.md)

[Snow-Lang 语法规范](docs/Snow-Lang-Syntax/Snow-Lang-Grammar-Specification.md)


[Git 管理规范](docs/Snow-Lang-Git-Management/Snow-Lang-Git-Management.md)

[SnowVM OpCode 指令表](docs/SnowVM-OpCode/SnowVM-OpCode.md)

[Snow-Lang GraalVM AOT 打包指南](docs/Snow-Lang-GraalVM-AOT-Native-Image-Package/Snow-Lang-GraalVM-AOT-Native-Image-Package.md)


## 开发环境安装

1. **开发环境准备**: 
    1. 安装集成开发环境 [IntelliJ IDEA](https://www.jetbrains.com/idea/download)
    2. 安装 Java 开发工具 [Graalvm-jdk-25](https://www.graalvm.org/downloads/)

2. **获取源码**: 
   将项目源码下载或克隆到本地目录。
    ```bash
    git clone https://gitee.com/jcnc-org/snow.git
    ```

    
## 编译 Snow 源代码

### 1. 独立编译 (Standalone Compilation)

独立编译不依赖 `.cloud` 文件，而是直接使用 `snow` 编译器进行 `.snow` 文件的编译和执行。

#### 独立编译步骤: 

1. **运行编译器:**
   你可以通过以下命令来编译单个或多个 `.snow` 文件，或者递归编译一个目录中的所有 `.snow` 源文件为`.water`虚拟机指令。

    * **单个文件编译:**

      ```bash
      snow compile [SnowCode].snow
      ```

    * **多个文件编译:**

      ```bash
      snow compile [SnowCode1].snow [SnowCode2].snow [SnowCode3].snow -o [Name]
      ```

    * **目录递归编译:**

      ```bash
      snow -d path/to/source_dir
      ```

2. **查看编译输出:**
   编译过程会输出源代码、抽象语法树（AST）、中间表示（IR）以及虚拟机指令等内容。你可以看到如下几个分段输出: 

    * **AST**（抽象语法树）部分以 JSON 格式输出。
    * **IR**（中间表示）部分会列出逐行的中间代码。
    * **VM code**（虚拟机指令）会展示虚拟机的字节码指令。

3. **默认执行模式:**
   编译器会在 **RUN 模式** 下运行，**DEBUG 模式**显示详细的执行过程和状态，并且在虚拟机中执行编译后的代码，最后会打印出所有局部变量的值。

---

### 2. **集成编译 (Integrated Compilation)**

集成编译需要使用 `.cloud` 文件来指定项目的配置和结构，适用于项目标准化、依赖管理、构建管理和项目分发等场景。

#### 集成编译命令: 

1. **基本用法:**

   ```bash
     snow [OPTIONS] <command>
   ```

2. **命令选项:**

    * `-h, --help`: 显示帮助信息并退出。
    * `-v, --version`: 打印 Snow 编程语言的版本并退出。

3. **可用命令:**

    * `compile`: 将 `.snow` 源文件编译成虚拟机字节码文件（`.water`）。此命令会使用 `.cloud` 文件来指导编译过程。
    * `clean`: 清理构建输出和本地缓存，移除中间产物，释放磁盘空间。
    * `version`: 打印 Snow 的版本。
    * `run`: 运行已编译的虚拟机字节码文件（`.water`）。
    * `init`: 初始化一个新项目，生成 `project.cloud` 文件。
    * `generate`: 根据 `project.cloud` 生成项目目录结构。
    * `build`: 构建当前项目，按顺序解析依赖、编译和打包。

4. **例如，执行集成编译命令:**

   ```bash
   snow compile [SnowCode].snow
   ```

    * 此命令会使用 `.cloud` 文件中的配置信息来指导编译过程，并生成 `.water`。

5. **使用帮助:**
   如果你需要了解某个命令的详细选项，可以使用: 

   ```bash
   snow <command> --help
   ```

   例如，查看 `compile` 命令的具体选项: 

   ```bash
   snow compile --help
   ```

---

## 示例代码片段

以下是一个简单的 Snow 代码示例,演示模块定义,导入和函数声明的基本语法: 

```snow
module: Math
    function: main
        returns: int
        body:
            Math.factorial(6)
            return 0
        end body
    end function

    function: factorial
        params:
            declare n:int
        returns: int
        body:
            declare num1:int = 1
            loop:
                init:
                    declare counter:int = 1
                cond:
                    counter <= n
                step:
                    counter = counter + 1
                body:
                    num1 = num1 * counter
                end body
            end loop
            return num1
        end body
    end function
end module
```

上述代码定义了一个名为 `Math` 的模块，其中包含两个函数: 

* `main`: 不接收任何参数，返回类型为 `int`。在函数体内调用了 `Math.factorial(6)`，然后返回 `0`。
* `factorial`: 接收一个 `int` 类型的参数 `n`，返回类型为 `int`。函数体内先声明并初始化局部变量 `num1` 为 `1`，然后通过一个
  `loop` 循环（从 `counter = 1` 到 `counter <= n`）依次将 `num1` 乘以 `counter`，循环结束后返回 `num1`，即 `n` 的阶乘值。


> 更多示例代码见 [playground 目录](https://gitee.com/jcnc-org/snow/tree/main/playground) 

## 项目结构说明

项目采用 Maven 多模块构建，源码结构如下:

### 根级模块结构

```
Snow/
├── snow-backend/        # 独立模块：编译器后端
├── snow-common/         # 独立模块：通用工具
├── snow-ir/             # 独立模块：中间表示（IR）
├── snow-lexer/          # 独立模块：词法分析
├── snow-parser/         # 独立模块：语法分析
├── snow-semantic/       # 独立模块：语义分析
├── snow-vm/             # 独立模块：虚拟机
├── src/main/java/org/jcnc/snow/  # 主源码目录
├── pom.xml              # Maven 聚合配置
└── ...
```

### 主源码目录结构（src/main/java/org/jcnc/snow/）

* **`compiler/`** - Snow 编译器核心

    * **`lexer/`** - 词法分析模块
        * `core/`: 词法分析器主逻辑
        * `scanners/`: 各类 Token 扫描器（关键字、字符串、数字等）
        * `token/`: Token 定义和工具
        * `utils/`: 词法分析辅助工具

    * **`parser/`** - 语法分析模块
        * `ast/`: 抽象语法树（AST）节点定义
        * `context/`: 解析上下文管理
        * `core/`: 解析器主逻辑
        * `expression/`: 表达式解析
        * `statement/`: 语句解析
        * `module/`: 模块解析
        * `function/`: 函数解析
        * `struct/`: 结构体解析
        * `utils/`: 语法分析辅助工具
        * `factory/`: 工厂类

    * **`semantic/`** - 语义分析模块
        * `core/`: 语义分析器主逻辑
        * `analyzers/`: 具体的语义分析器（类型检查、符号表等）
        * `type/`: 类型系统定义和管理
        * `symbol/`: 符号表管理
        * `error/`: 错误处理
        * `utils/`: 语义分析辅助工具

    * **`ir/`** - 中间表示（IR）模块
        * `core/`: IR 核心数据结构（函数、基本块、指令等）
        * `builder/`: IR 构建器（表达式、语句、函数等）
        * `instruction/`: IR 指令定义
        * `value/`: IR 值类型
        * `common/`: 公共工具
        * `utils/`: IR 辅助工具

    * **`backend/`** - 编译器后端模块
        * `core/`: 后端编译核心
        * `generator/`: 虚拟机指令生成器
        * `builder/`: 指令构建器
        * `alloc/`: 寄存器分配
        * `utils/`: 后端辅助工具

* **`vm/`** - 虚拟机（SnowVM）

    * **`engine/`** - 执行引擎
        * 寄存器/栈管理
        * 指令执行逻辑

    * **`commands/`** - 虚拟机指令集
        * 各类指令的具体实现

    * **`execution/`** - 执行流程控制
        * 指令顺序执行
        * 分支跳转管理

    * **`io/`** - 输入输出
        * 指令加载
        * 文件解析
        * 标准库 I/O 实现

    * **`gui/`** - 可视化调试
        * Swing 调试面板
        * 局部变量表展示

    * **`module/`** - 模块管理
        * 标准库模块加载

    * **`interfaces/`** - 公共接口

    * **`factories/`** - 工厂类

    * **`utils/`** - 工具类
        * 日志、调试输出等

    * **`VMInitializer.java`** - VM 初始化器
    * **`VMLauncher.java`** - VM 启动器

* **`pkg/`** - 构建与包管理系统（snow pkg）

    * **`dsl/`** - DSL 解析器
        * `.cloud` 配置文件解析

    * **`tasks/`** - 预设任务
        * clean / compile / run / package / publish 等

    * **`resolver/`** - 依赖解析
        * 本地/远程仓库访问
        * 缓存管理

    * **`lifecycle/`** - 生命周期管理
        * pre/post 脚本钩子

    * **`model/`** - 数据模型
        * 项目模型
        * 依赖模型
        * 版本模型

    * **`utils/`** - 工具函数
        * 文件操作
        * 日志记录
        * 校验和计算

* **`cli/`** - 命令行前端

    * **`commands/`** - 子命令实现
        * compile / run / clean / init / generate / build 等

    * **`api/`** - 公共接口
        * 选项解析
        * 终端交互抽象

    * **`utils/`** - 工具类
        * 终端颜色、进度条
        * 异常格式化

    * **`SnowCLI.java`** - 主入口

* **`common/`** - 通用工具模块

    * **`Mode.java`** - 运行模式（RUN、DEBUG）
    * **`SnowConfig.java`** - 配置管理
    * **`NumberLiteralHelper.java`** - 数字字面量处理
    * **`StringEscape.java`** - 字符串转义处理


## 版权声明

版权所有 © 2025 许轲（Luke），代表 SnowLang 项目。  
仓库地址: <https://gitee.com/jcnc-org/snow>  
本项目依据 [Apache 2.0 许可证](LICENSE) 进行许可和发布。

“SnowLang 项目”为由许轲（Luke）发起的独立开源项目。  
未来，项目可能会成立正式的组织或实体，以进一步负责本项目的开发和管理。

## 支持我们

如果你喜欢我们的项目，欢迎给我们一个 Star！  
你们的关注和支持，是我们团队持续进步的动力源泉！谢谢大家！


## 加入我们

- 微信: `xuxiaolankaka`
- QQ: `1399528359`
- 邮箱: `luke.k.xu [at] hotmail.com`

