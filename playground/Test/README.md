# Snow语言测试文件组织说明

本目录包含对Snow编程语言语法特性的测试文件，已按功能分类组织。

## 目录结构

### basic_tests/

包含Snow语言基本语法特性的测试文件：

- basic_module.snow - 模块定义测试
- variable_types.snow - 数据类型测试
- functions.snow - 函数定义和调用测试
- control_structures.snow - 控制结构测试
- strings.snow - 字符串操作测试
- globals.snow - 全局变量测试
- access_control.snow - 访问控制测试

### advanced_tests/

包含Snow语言高级特性的测试文件：

- arrays_updated2.snow - 一维数组操作测试
- multidimensional_arrays.snow - 多维数组操作测试
- structs.snow - 结构体测试
- inheritance_simple.snow - 继承测试
- syscalls_simple.snow - 系统调用测试

### comprehensive_tests/

包含综合测试文件：

- final_comprehensive_test_fixed.snow - 最终综合测试

## 测试执行

要运行特定的测试文件，可以使用以下命令：

```
snow compile run playground/Test/[category]/[test_file].snow
```

例如：

```
snow compile run playground/Test/basic_tests/basic_module.snow
```