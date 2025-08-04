# Snow-Lang 语法

## 快速入门

一个简单的 snow 程序

```snow
module: Main
    function: main
        returns: int
        body:

            return 1 + 1
        end body
    end function
end module
```

## 基础

### 注释
```snow
// 单行注释

/*
    多行注释
    多行注释
    多行注释
*/
```

### 数据类型

bool 类型: 

两种值: `true` 和 `false`




数值类型: 

| Number   | keyword |
|----------|---------|
| byte8    | byte    |
| short16  | short   |
| int32    | int     |
| long64   | long    |
| float32  | float   |
| double64 | double  |

默认整数的类型为 int，浮点数的类型为 double。

数值字面量后缀: 

| 数值字面量后缀 | 例子 |
|---------|----|
| b、B     | 7b |
| s、S     | 7s |
| l、L     | 7l |
| f、F     | 7f |


### 变量
定义变量的形式如下，中括号表示可选: 

```snow
declare name: type [= initial_value]
```

其中 `name` 是变量名，`type` 是变量类型，`initial_value` 是初始值

例: 

```snow
declare x: int
declare y: long = 123456789
```

读取变量值的方法，直接写变量的名字即可: 
```snow
x
```

设置变量值的方法，先写变量名，后面接 `=`，最后写一个表达式即可: 
```snow
x = 10
```

于是可以通过这种方式让变量参与计算并保存结果: 
```snow
x = y + 1
```
读取 `y` 的值加 1 并保存到变量 `x`。

变量只能定义在函数体中、函数参数列表、loop 初始化器中。

## 流程控制
### if
if 语句的形式如下，else 是可选的: 

```snow
if cond then
    // 条件成立执行的代码
else
    // 以上条件不成立执行的代码
end if
```

cond 可以是表达式（结果为 bool 类型）或者 bool 字面量

例: 

```snow
module: Main
    function: main
        returns: int
        body:
            if 5 > 7 then
                return 5
            else
                return 7
            end if

            return 0
        end body
    end function
end module
```

### loop
loop 语句的形式如下: 
```snow
loop:
    init:
        // 循环开始前可声明循环变量，有且只能声明一个
        declare i: int = 1
    cond:
        // 循环条件，成立则执行 body 的代码，
        // 不成立则退出循环，有且只能写一条
        i <= 100
    step:
        // 循环体执行完后执行的代码，有且只能写一条
        i = i + 1
    body:
        // 每次执行的代码写这里
    end body
end loop
```

例子（求 1 ~ 100 的和）: 
```snow
module: Main
    function: main
        returns: int
        body:
            declare sum: int = 0
            loop:
                init:
                    declare i: int = 1
                cond:
                    i <= 100
                step:
                    i = i + 1
                body:
                    sum = sum + i
                end body
            end loop

            return sum
        end body
    end function
end module
```

## 函数
函数的形式如下: 
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
其中 add 是函数名，params 下面是参数列表（可省略），与变量的定义类似，但是不允许赋初值，
接着 returns 设置返回值类型，最后的 body 为函数体。

## 模块

一个模块可以包含多个函数，
通过 import 语句导入模块，
snow 会自动将同名模块的函数合并。

在我们最初的例子中，就用了 module 这个关键字。让我们回忆一下: 

```snow
module: Main
    function: main
        returns: int
        body:

            return 1 + 1
        end body
    end function
end module
```

可以看到模块名是 Main，里面有函数 main。

假如现在有一个模块 Math，代码如下: 
```snow
// Math.snow
module: Math
    function: add
        params:
            declare a: int
            declare b: int
        returns: int
        body:
            return a + b
        end body
    end function
end module
```

可以使用 import 来导入 Math 模块: 
```snow
// main.snow
module: Main
    import: Math
    function: main
        returns: int
        body:

            return Math.add(5, 7)
        end body
    end function
end module
```

可以同时导入多个模块，用逗号（半角）分隔模块名即可: 
```snow
// main.snow
module: Main
    import: Math, Time
    function: main
        returns: int
        body:

            return Math.add(5, 7)
        end body
    end function
end module
```
