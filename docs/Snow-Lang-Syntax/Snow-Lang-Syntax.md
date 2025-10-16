# Snow编程语言语法规则

## 目录

1. [快速入门](#快速入门)
2. [基础语法](#基础语法)
3. [数据类型](#数据类型)
4. [变量声明](#变量声明)
5. [函数定义](#函数定义)
6. [控制结构](#控制结构)
7. [结构体](#结构体)
8. [数组](#数组)
9. [系统调用](#系统调用)
10. [标准库](#标准库)
11. [运算符](#运算符)
12. [字符串操作](#字符串操作)
13. [程序入口](#程序入口)
14. [访问控制](#访问控制)
15. [语法规范参考](#语法规范参考)
16. [最佳实践和注意事项](#最佳实践和注意事项)

## 快速入门

一个简单的Snow程序示例：

```snow
module: Main
    import: std_io
    
    function: main
        returns: void
        body:
            std_io.println("Hello, Snow!")
        end body
    end function
end module
```

## 基础语法

### 模块定义

Snow程序以模块为基本单位组织代码：

```snow
module: ModuleName
    // 模块内容
end module
```

**重要注意事项**：

- 结构体必须在globals块之前定义
- 模块内元素的正确顺序：结构体 → globals → 函数

### 导入语句

使用import关键字导入其他模块：

```snow
import: std_io
import: syscall.time
// 导入多个模块
import: std_io, Math, syscall.time
```

### 注释

Snow支持单行和多行注释：

```snow
// 单行注释

/*
    多行注释
    多行注义
*/
```

### 全局变量声明

在模块中使用globals块声明全局变量：

```snow
globals:
    declare variableName: type = value
    declare const constantName: type = value
```

## 数据类型

Snow语言支持以下基本数据类型：

| 类型名    | 关键字     | 位数 | 描述                     |
|--------|---------|----|------------------------|
| 字节型    | byte    | 8  | 8位有符号整数                |
| 短整型    | short   | 16 | 16位有符号整数               |
| 整型     | int     | 32 | 32位有符号整数（默认整数类型）       |
| 长整型    | long    | 64 | 64位有符号整数               |
| 单精度浮点数 | float   | 32 | 32位IEEE-754浮点数         |
| 双精度浮点数 | double  | 64 | 64位IEEE-754浮点数（默认浮点类型） |
| 字符串    | string  | -  | 字符串类型                  |
| 布尔型    | boolean | -  | 布尔类型（true/false）       |
| 空类型    | void    | -  | 无返回值类型                 |
| 任意类型   | any     | -  | 任意类型                   |

### 数值字面量后缀

为指定具体类型，可在数值字面量后加后缀字母（大小写均可）：

| 后缀  | 类型    | 例子       |
|-----|-------|----------|
| b/B | byte  | 7b, -2B  |
| s/S | short | 123s     |
| l/L | long  | 5l, 123L |
| f/F | float | 3.14f    |

没有后缀的整数字面量自动为`int`，没有后缀的浮点字面量自动为`double`。

## 变量声明

使用declare关键字声明变量：

```snow
// 声明变量
declare variableName: type = initialValue
declare variableName: type  // 未初始化

// 示例
declare n: int = 1
declare str: string = "Hello"
declare arr: int[] = [1, 2, 3]
declare flag: boolean = true
```

变量只能定义在函数体中、函数参数列表、loop初始化器中，或在globals块中声明全局变量。

## 函数定义

函数使用以下语法定义：

```snow
function: functionName
    params:
        declare param1: type
        declare param2: type
    returns: returnType
    body:
        // 函数体
        return value
    end body
end function
```

示例：

```snow
function: add
    params:
        declare a: int
        declare b: int
    returns: int
    body:
        return a + b
    end body
end function
```

## 控制结构

### 条件语句

```snow
// 简单if语句
if condition then
    // 条件为真时执行
end if

// if-else语句
if condition then
    // 条件为真时执行
else
    // 条件为假时执行
end if
```

示例：

```snow
if n1 == 1 then
    if n2 == 2 then
        n3 = 3
    end if
end if
```

### 循环语句

Snow使用loop关键字定义循环：

```snow
loop:
    init:
        // 初始化语句
    cond:
        // 循环条件
    step:
        // 步进语句
    body:
        // 循环体
    end body
end loop
```

示例：

```snow
loop:
    init:
        declare i: int = 0
    cond:
        i < 10
    step:
        i = i + 1
    body:
        std_io.println(i)
    end body
end loop
```

### 跳转语句

- `break`: 跳出当前循环
- `continue`: 跳过当前循环迭代，继续下一次循环

## 结构体

Snow支持结构体定义，类似于面向对象语言中的类：

```snow
struct: StructName
    fields:
        declare fieldName: type
    
    init:
        params:
            declare param: type
        body:
            this.fieldName = param
        end body
    end init
    
    function: methodName
        params:
            // 参数声明
        returns: returnType
        body:
            // 方法体
        end body
    end function
end struct
```

### 继承

结构体支持继承：

```snow
struct: ChildStruct extends ParentStruct
    // 子结构体内容
end struct
```

**重要注意事项**：

- 父类和子类的同名字段必须保持类型一致
- 在构造函数中使用`super()`调用父类构造函数

示例：

```snow
// 定义Animal结构体
struct: Animal
    fields:
        declare name: int
        declare age: int

    init:
        params:
            n: int
            a: int
        body:
            this.name = n
            this.age = a
        end body
    end init

    function: getName
        returns: int
        body:
            return this.name
        end body
    end function
end struct

struct: Dog extends Animal
    fields:
        declare breed: int
    
    init:
        params:
            n: int
            a: int
            b: int
        body:
            super(n, a)
            this.breed = b
        end body
    end init

    function: getBreed
        returns: int
        body:
            return this.breed
        end body
    end function
end struct

// 实例化Dog
declare dog: Dog = new Dog(1, 3, 2)
```

### 实例化

使用new关键字创建结构体实例：

```snow
declare instance: StructName = new StructName(parameters)
```

## 数组

Snow支持多维数组：

```snow
// 一维数组
declare arr: int[] = [1, 2, 3]

// 多维数组
declare matrix: int[][] = [[1, 2], [3, 4]]
declare cube: int[][][] = [
    [[1, 2], [3, 4]],
    [[5, 6], [7, 8]]
]

// 访问数组元素
declare element: int = arr[0]
declare value: int = matrix[1][1]
```

**重要注意事项**：

- 数组没有`.length`属性，需要手动管理数组长度
- 访问数组元素时要注意边界检查

示例：

```snow
// 遍历数组
declare arr: int[] = [1, 2, 3, 4, 5]
declare arrLength: int = 5
loop:
    init:
        declare i: int = 0
    cond:
        i < arrLength
    step:
        i = i + 1
    body:
        std_io.println("arr[" + i + "] = " + arr[i])
    end body
end loop
```

## 系统调用

Snow通过syscall函数访问底层系统功能：

```snow
declare result: type = syscall("syscall_code", parameters)
```

示例：

```snow
// 获取单调时钟的毫秒数
declare ms: long = syscall("0x1703")

// 获取字符串长度（注意：字符串没有.length属性）
declare length: int = syscall("0x1801", str)
```

## 标准库

Snow提供了一些标准库模块：

### std_io模块

提供标准输入输出功能：

```snow
std_io.print(value)      // 输出不换行
std_io.println(value)    // 输出并换行
std_io.eprint(value)     // 输出到错误流不换行
std_io.eprintln(value)   // 输出到错误流并换行
std_io.read_line()       // 从标准输入读取一行
```

### syscall.time模块

提供时间相关功能：

```snow
time.mono_ms()    // 获取单调时钟的毫秒数
time.now_ms()     // 获取当前时间的毫秒数
time.sleep_ms(ms) // 休眠指定毫秒数
```

## 运算符

Snow支持常见的运算符：

### 算术运算符

- `+`: 加法
- `-`: 减法
- `*`: 乘法
- `/`: 除法
- `%`: 取模

### 比较运算符

- `==`: 等于
- `!=`: 不等于
- `<`: 小于
- `>`: 大于
- `<=`: 小于等于
- `>=`: 大于等于

### 逻辑运算符

- `&&`: 逻辑与
- `||`: 逻辑或
- `!`: 逻辑非

### 赋值运算符

- `=`: 赋值

## 字符串操作

Snow支持字符串字面量和转义序列：

```snow
// 基本字符串
declare str: string = "Hello World"

// 包含转义序列的字符串
declare strWithNewline: string = "Hello\nWorld"  // 包含换行符
declare strWithPath: string = "C:\\Snow\\file.txt"  // 包含反斜杠
declare strWithQuotes: string = "He said \"Hello\""  // 包含双引号
declare unicodeStr: string = "\u4F60\u597D"  // Unicode转义

// 字符串连接
declare greeting: string = "Hello" + " " + "World"

// 字符串与变量连接
std_io.println("The value is: " + variable)
std_io.println("Result: " + (x + y))
```

注意：在Snow语言中，可以直接使用`+`操作符连接字符串和变量，也可以将变量作为参数传递给输出函数。

**重要注意事项**：

- 字符串没有`.length`属性
- 字符串操作需要通过标准库函数或系统调用来实现

支持的转义序列：

- `\n`: 换行
- `\t`: 制表符
- `\\`: 反斜杠
- `\"`: 双引号
- `\'`: 单引号
- `\r`: 回车
- `\b`: 退格
- `\f`: 换页
- `\uXXXX`: Unicode字符

## 程序入口

Snow程序的入口点是main函数：

```snow
function: main
    returns: void  // 或其他返回类型
    body:
        // 程序主体
    end body
end function
```

示例：

```snow
module: Main
    import: std_io
    import: syscall.time

    function: main
        returns: void
        body:
            std_io.println("Starting performance benchmark...")
            
            // 调用其他函数
            benchmarkArithmetic()
            benchmarkStringOps()
        end body
    end function

    function: benchmarkArithmetic
        returns: void
        body:
            declare startTime: long = time.mono_ms()
            
            declare result: int = 0
            loop:
                init:
                    declare i: int = 0
                cond:
                    i < 1000000
                step:
                    i = i + 1
                body:
                    result = result + i * 2 - 1
                end body
            end loop
            
            declare endTime: long = time.mono_ms()
            std_io.println("Arithmetic test: " + (endTime - startTime) + " ms, result = " + result)
        end body
    end function
end module
```

## 访问控制

Snow支持访问控制约定：

- **私有成员与方法**: 以单个下划线 `_` 开头的变量名、字段名或方法名，**仅在其所属结构体或模块内部可见**。
    - 例如 `_foo`, `_barMethod`。
- **公有成员与方法**: 非下划线开头的变量、字段、方法，默认为公有。可在模块外部通过模块名/别名前缀访问。

示例：

```snow
// module: foo
globals:
    declare _secret: int = 42      // 仅 foo 模块内部可见
    declare visible: int = 1       // 公有

function: _hidden                  // 仅 foo 模块内部可见
    returns: int
    body:
        return 100
    end body
end function

function: publicFunc
    returns: int
    body:
        return _secret + 1         // 合法
    end body
end function

// module: bar
import: foo

declare x: int = foo.visible       // 合法
// declare y: int = foo._secret    // 编译错误 AccessDenied
// declare z: int = foo._hidden()  // 编译错误 AccessDenied
```

## 语法规范参考

### 保留关键字

Snow语言的保留关键字包括：

```
module import end module globals struct end struct
function end function params returns body end body
declare if then else end if loop init cond step
end loop break continue self
```

### 标识符规则

- 标识符只允许英文大小写字母 (A-Z a-z)、数字 (0-9) 与下划线 _
- 首字符不能为数字
- 区分大小写: Foo 与 foo 为不同标识符
- 保留字禁止用作标识符

### 作用域规则

1. **模块顶层**: 全局变量、结构体、模块级函数
2. **结构体内部**: 字段、方法、构造函数
3. **函数/方法**: 形参与局部变量
4. **局部嵌套块** (if / loop 等) 的局部变量

### 唯一性规则

- 同一模块的结构体名、模块级函数名、全局变量名不得重名
- 结构体内部字段名与方法名不得相同
- 允许有多个与结构体名同名的函数（即构造函数重载），但其参数签名必须互不相同

## 最佳实践和注意事项

### 1. 类型一致性

- 在继承关系中，父类和子类的同名字段必须保持类型一致
- 字符串类型在继承中可能存在限制，建议优先使用基本数据类型

### 2. 数组操作

- 数组没有`.length`属性，需要手动管理数组长度
- 在访问数组元素时要注意边界检查，避免运行时错误

### 3. 字符串处理

- 字符串没有`.length`属性，需要通过系统调用或其他方式获取长度
- 字符串操作主要通过标准库函数实现

### 4. 模块结构

- 结构体必须在globals块之前定义
- 遵循正确的模块元素顺序：结构体 → globals → 函数

### 5. 错误处理

- 注意处理编译时和运行时错误
- 合理使用访问控制约定保护私有成员