# Snow‑Lang 语法规范

---

## 0 · 符号约定

* ⟦ … ⟧: 可选项（0 或 1 次）
* { … }\*: 可重复项（0 次或多次）

---

## 1 · 词汇结构

### 1.1 编码与字符集

源文件采用 UTF‑8 编码。除注释外，标识符只允许英文大小写字母 (A‑Z a‑z)、数字 (0‑9) 与下划线 _；首字符不能为数字。

```ebnf
identifier  ::=  [A-Za-z_][A-Za-z0-9_]* ;
```

* **区分大小写**: Foo 与 foo 为不同标识符。
* 保留字 (见 §1.3) **禁止** 用作标识符。

### 1.2 分隔符与强制空白

相邻两个标记之间 **推荐** 至少以 1 个空白字符分隔，除非记号本身带有定界符 (( ) , : = < > 等)。示例: 

```snow
module: Foo            // 推荐
module:Foo             // 不推荐
```

### 1.3 保留关键字

```
module import end module globals struct end struct
function end function params returns body end body
declare if then else end if loop init cond step
end loop break continue self
```

以上列表 **均为关键词**，大小写固定，不能作为标识符。

### 1.4 文字量 (Literal)

* **整型**: 123 0 -42
* **浮点**: 3.14 0.0
* **布尔**: true false
* **字符串**: 未来版本保留；当前规范未定义。

### 1.5 注释

* **单行注释**: 以 // 起，至当行行尾。
* **多行注释**: /* … */ 可跨行。**不可嵌套**；嵌套会在最内层 */ 处终止外层，导致编译错误。

---

### 1.6 换行与缩进

* **只有换行有语义**: 以行末冒号（:）打开一个块时（如 module:、function:、if、loop: 等），块体**必须另起新行**。
* **缩进没有语义**: 缩进仅用于提高代码可读性，对语法无影响。缩进不一致不会报错。
* 块体通过显式关闭关键字（如 end module、end function 等）结束。
* 若关闭关键字与开始关键字之间的缩进不一致，不会报错，仍以关闭关键字为准。

> 块体结构完全由行分隔和显式关闭关键字决定，缩进仅为美观，不影响代码执行。

---

## 2 · 模块与导入

### 2.1 模块声明

```snow
module: <ModuleName>
    …
end module
```

* 一个源文件只能出现 **一次** module: 声明，且文件名与模块名无必然关系。
* 模块名可使用点号（.）分隔表示包层级，例如 util.math。
* **允许** 不同文件夹下声明同名模块，但模块全名（含包路径，用点分隔）在全项目中必须唯一。
* 若项目中出现重复的模块全名，编译阶段将报重定义错误。

例如: 
> src/util/math.snow         // module: util.math
> src/core/math.snow         // module: core.math
>
> 两者都声明了 module: math，但由于包路径不同（util.math 与 core.math），互不冲突。

### 2.2 导入

```snow
import: <ModuleA>⟦ as <AliasA>⟧, <ModuleB>⟦ as <AliasB>⟧, …
```

* **别名 (Alias)** 可为任何合法标识符，放在 as 关键字之后。
* **重复导入**: 对同一模块多次导入（无论是否带 alias）只解析一次，其余忽略告警。
* **循环依赖**: 当前规范未定义，若出现编译器可拒绝或延迟解析。
* **子模块**（诸如 A.B）暂不支持。

### 2.3 全路径引用

* 跨模块引用必须使用 _Prefix_._Name_，其中 *Prefix* 是模块名或导入时的别名。
  例如: Math.Point 或 M.sin。
* **解析顺序**: 未加前缀的标识符只在本模块查找；找不到则视为编译错误，不会隐式搜索导入模块。

---

## 3 · 命名与作用域

### 3.1 作用域层级

1. **模块顶层**: 全局变量、结构体、模块级函数。
2. **结构体内部**: 字段、方法、构造函数。
3. **函数／方法**: 形参与局部变量。
4. **局部嵌套块** (if / loop 等) 的局部变量。

### 3.2 唯一性规则

* **模块顶层唯一**: 同一模块的 *结构体名*、*模块级函数名*、*全局变量名* **不得重名**。
* **结构体内部**: 字段名与方法名不得相同；**允许有多个与结构体名同名的函数（即构造函数重载），但其参数签名必须互不相同。**
* **构造函数重载**: 结构体内部可以声明多个与结构体名同名的函数作为构造函数，参数类型与数量必须不同，否则属于 DuplicateName 错误。
* **跨层级遮蔽**: 内层可定义与外层同名标识符（除关键字限制外），遵循最近作用域原则。

### 3.3 访问控制约定

* **私有成员与方法**: 以单个下划线 `_` 开头的变量名、字段名或方法名，**仅在其所属结构体或模块内部可见**。外部不可访问，编译器应报错 `AccessDenied`。
  - 例如 `_foo`, `_barMethod`。
* **公有成员与方法**: 非下划线开头的变量、字段、方法，默认为公有。可在模块外部通过模块名/别名前缀访问。

#### 影响范围
- **模块级变量与函数**: `globals` 和 `function` 语句声明的以 `_` 开头者，仅限本模块访问。
- **结构体字段与方法**: 声明为 `_name`、`_doSomething` 的，仅结构体本身或其方法体可访问。
---

#### 访问示例

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
declare y: int = foo._secret       // 编译错误 AccessDenied
declare z: int = foo._hidden()     // 编译错误 AccessDenied
```

## 4 · 声明语法

### 4.1 全局变量

```snow
globals:
    declare VarName: Type ⟦= Expr⟧
```

* 后续对该变量赋值时 **不得** 使用 declare。

### 4.2 结构体

```snow
struct: StructName
    declare field1: Type1
    declare field2: Type2
    …

    // ──────────── 构造函数(可以不存在, 可重载) ──────────────
    function: StructName
        params:
            declare p1: Type1
            …
        body:
            self.field1 = p1
            …
        end body
    end function

    function: StructName
        params:
            declare q1: TypeQ
            declare q2: TypeQ
        body:
            // ...
        end body
    end function
    // ...可继续声明更多参数签名各异的构造函数...
    // ─────────────────────────────────────────────

    // ────────── 结构体内函数 (可以不存在) ──────────
    function: method1         
        ⟦params:
            declare x: TypeX
            …⟧
        returns: ReturnType
        body:
            …
        end body
    end function
    // ─────────────────────────────────────────────

end struct
```

**实例化**: 

* declare p: Point = Point(1, 2)
* declare p2: Point = Point(3)

- 实参顺序 = 构造函数 params: 声明顺序；与字段声明顺序无关。
- **构造函数重载时**，调用 Point(...) 时根据参数数量和类型匹配唯一的构造函数。若无匹配或多重匹配，则为编译错误。
- 不支持命名实参、缺省实参或字段名字面量初始化。

### 4.3 函数

```snow
function: FuncName
    returns: Type            // 无返回值的时候写 void
    ⟦params:
        declare x: TypeX     // 无参数的时候不写 params
        …⟧
    body:
        …
    end body
end function
```

* **返回检查**: 若 returns: 指定类型非 void，所有控制流路径必须显式 return 该类型值。
* 在 loop 内或经 break 跳出后能到达的路径亦计入检查；若缺失，编译错误。

---

## 5 · 语句

### 5.1 变量声明与赋值

```snow
declare x: int = 0
x = 1
```

* 同一作用域中 declare 仅能出现一次。

### 5.2 条件语句

```snow
if Condition then
    …
else
    …
end if
```

* Condition 类型必须为 bool。

### 5.3 循环语句

```snow
loop:
    init:
        declare i: int = 0
    cond:
        i < 10
    step:
        i = i + 1
    body:
        …
    end body
end loop
```

* **作用域**: init 块声明的变量仅在本 loop 的 init/cond/step/body 有效。
* break 立即终止当前循环；continue 跳过剩余 body，执行 step → cond。

---

## 6 · 类型系统

### 6.1 数值类型

Snow-Lang 支持下列**数值类型**，用于声明变量、参数、结构体字段、函数返回等: 

| 类型名      | 关键字    | 位数 | 描述                        |
|----------|--------|----|---------------------------|
| byte8    | byte   | 8  | 8 位有符号整数                  |
| short16  | short  | 16 | 16 位有符号整数                 |
| int32    | int    | 32 | 32 位有符号整数（默认整数类型）         |
| long64   | long   | 64 | 64 位有符号整数                 |
| float32  | float  | 32 | 32 位 IEEE-754 浮点数         |
| double64 | double | 64 | 64 位 IEEE-754 浮点数（默认浮点类型） |

**说明**
* 没有无符号整型，所有整型均为有符号。
* `int` 为整数常量与变量的默认类型。
* `double` 为浮点常量与变量的默认类型。
* `bool` 类型只表示真/假，不允许与数值类型直接互转。

#### 数值字面量后缀

为指定具体类型，可在数值字面量后加后缀字母（大小写均可）: 

| 后缀 | 类型     | 例子       |
|----|--------|----------|
| b  | byte   | 7b, -2B  |
| s  | short  | 123s     |
| i  | int    | 100i     |
| l  | long   | 5l, 123L |
| f  | float  | 3.14f    |
| d  | double | 1.0d     |

- 没有后缀的整数字面量自动为 `int`。
- 没有后缀的浮点字面量自动为 `double`。

**示例: **
```snow
declare a: int = 7        // int    (默认)
declare b: long = 9l      // long
declare c: float = 2.5f   // float
declare d: double = 2.5   // double (默认)
declare e: byte = 127b    // byte
declare f: short = 100s   // short
````

---

### 6.2 布尔类型

* 关键字为 `bool`
* 字面量为 `true` 或 `false`
* 仅用于逻辑判断、条件分支，不与整型互转

---

### 6.3 数组类型

支持一维和多维数组。数组类型以 `T[]` 表示元素类型为 T 的一维数组，多维数组以 `T[][]`、`T[][][]` 依次类推。

#### 声明与初始化

````snow
declare arr: int[] = [1, 2, 3]
declare matrix: double[][] = [
    [1.1, 2.2],
    [3.3, 4.4]
]
declare cube: bool[][][] = [
    [[true, false], [false, true]],
    [[false, false], [true, true]]
]
````

#### 访问与赋值

````snow
arr[0] = 10
matrix[1][1] = 5.6
declare x: int = arr[2]
declare y: double = matrix[0][1]
````

---

### 6.4 结构体类型

* 使用 `struct` 关键字声明
* 结构体类型为用户自定义类型，**值类型**（赋值、传参时会拷贝整个结构体）
* 字段类型可以为任何合法类型（包括数组、其它结构体）

````snow
struct: Point
    declare x: int
    declare y: int
end struct

declare a: Point = Point(1, 2)
````

---

### 6.5 传值说明

* **所有变量、参数、返回值**均为**值传递**（按值拷贝）
* 结构体、数组在赋值与传参时，均会复制整个值；后续修改不会影响原对象


---

## 7 · 名字解析算法（概览）

1. **输入**: 未限定前缀的标识符 N，当前作用域 S。
2. 自内向外遍历作用域链查找 N；首次匹配即确定绑定。
3. 若遍历至模块顶层仍未匹配，编译错误 *UnresolvedIdentifier*。
4. 限定名 Prefix.N: 直接在模块表中查 Prefix（包括别名），成功后在该模块顶层查找 N；找不到即 *UnresolvedQualifiedIdentifier*。

---

## 8 · 编译单位与入口

* **单一入口**: 编译器需指定某模块某函数作为启动入口。
* **模块初始化**: globals 块中的带初值变量在程序启动时自顶向下按出现顺序初始化；不同模块按依赖拓扑顺序初始化（循环依赖未定义）。

---

## 9 · 错误分类

| 编译期错误代码              | 产生条件                       |
|----------------------|----------------------------|
| DuplicateName        | 违反唯一性规则；结构体内有参数签名完全相同的构造函数 |
| UnresolvedIdentifier | 名字无法解析                     |
| ReturnMissing        | 非 void 函数缺少 return         |
| TypeMismatch         | 赋值或返回类型不兼容                 |
| ImportCycle          | （可选）检测到循环依赖                |
| CtorAmbiguous        | 构造函数重载时参数匹配不唯一             |
| CtorNotFound         | 构造函数重载时无匹配参数签名             |
| AccessDenied         | 访问了以 `_` 开头的私有变量或方法但不在允许范围 |

---


## 10 · 完整示例

````snow
module: RectExample
    import: Geometry

    struct: Point
        declare x: int
        declare y: int

        // 构造函数1: 两个参数
        function: Point
            params:
                declare x: int
                declare y: int
            body:
                self.x = x
                self.y = y
            end body
        end function

        // 构造函数2: 一个参数
        function: Point
            params:
                declare xy: int
            body:
                self.x = xy
                self.y = xy
            end body
        end function
    end struct

    struct: Rectangle
        declare left_top: Point
        declare right_bottom: Point

        function: Rectangle
            params:
                declare x1: int
                declare y1: int
                declare x2: int
                declare y2: int
            body:
                self.left_top = Point(x1, y1)
                self.right_bottom = Point(x2, y2)
            end body
        end function

        function: Rectangle
            params:
                declare width: int
                declare height: int
            body:
                self.left_top = Point(0, 0)
                self.right_bottom = Point(width, height)
            end body
        end function

        function: width
            returns: int
            body:
                return self.right_bottom.x - self.left_top.x
            end body
        end function

        function: height
            returns: int
            body:
                return self.right_bottom.y - self.left_top.y
            end body
        end function

        function: area
            returns: int
            body:
                return self.width() * self.height()
            end body
        end function
    end struct

    function: main
        returns: int
        body:
            declare rect1: Rectangle = Rectangle(0, 0, 10, 10)
            declare rect2: Rectangle = Rectangle(5, 6)
            declare result: int = 0
            if rect1.area() > 50 then
                loop:
                    init:
                        declare i: int = 1
                    cond:
                        i <= rect1.width()
                    step:
                        i = i + 1
                    body:
                        if i == 3 then
                            continue
                        end if
                        if i == 8 then
                            break
                        end if
                        result = result + i
                    end body
                end loop
            else
                result = rect1.area()
            end if
            return result
        end body
    end function

end module
````

---

## 11 · 构造函数重载示例

````snow
struct: Point
    declare x: int
    declare y: int

    function: Point
        params:
            declare x: int
            declare y: int
        body:
            self.x = x
            self.y = y
        end body
    end function

    function: Point
        params:
            declare xy: int
        body:
            self.x = xy
            self.y = xy
        end body
    end function
end struct

declare a: Point = Point(1, 2)  // 匹配第一个构造函数
declare b: Point = Point(5)     // 匹配第二个构造函数
````