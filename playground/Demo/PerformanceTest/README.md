# Snow语言性能测试套件

## 概述

这个目录包含了用于测试Snow语言性能的示例程序和相关文档。

## 文件说明

- [Main.snow] - 主程序入口，调用完整的基准测试
- [Benchmark.snow] - 完整的基准测试实现
- [SimpleBenchmark.snow] - 简化版的基准测试
- [PERFORMANCE_REPORT.md] - 性能测试报告
- [PERFORMANCE_TESTING_GUIDE.md] - 性能测试指南

## 如何运行性能测试

### 运行完整基准测试

```bash
# 编译并运行完整基准测试
snow compile Main.snow Benchmark.snow run

# 或者使用目录模式
snow compile -d . -o performance-test run
```

### 运行简化版基准测试

```bash
# 编译并运行简化版基准测试
snow compile SimpleBenchmark.snow run
```

## 性能测试内容

1. **算术运算性能** - 测试基本数学运算的执行速度
2. **字符串操作性能** - 测试字符串连接和操作的效率
3. **循环执行性能** - 测试循环结构的执行效率
4. **函数调用开销** - 测试函数调用的性能开销

## 查看详细文档

- [性能测试报告](PERFORMANCE_REPORT.md) - 包含详细的测试结果和分析
- [性能测试指南](PERFORMANCE_TESTING_GUIDE.md) - 提供创建和运行性能测试的详细指导

## 注意事项

1. 测试结果可能因硬件和系统环境不同而有所差异
2. 建议多次运行测试以获得更准确的结果
3. 在进行性能测试时，尽量关闭其他占用系统资源的程序