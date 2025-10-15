# Snow语言性能测试套件

## 概述

这个目录包含了用于测试Snow语言性能的示例程序和相关文档。

## 文件说明

- [Main.snow](Main.snow) - 主程序入口，调用完整的基准测试
- [Benchmark.snow](Benchmark.snow) - 完整的基准测试实现
- [SimpleBenchmark.snow](SimpleBenchmark.snow) - 简化版的基准测试
- [JavaBenchmark.java](JavaBenchmark.java) - Java版本的基准测试（用于性能比较）
- [PERFORMANCE_REPORT.md](PERFORMANCE_REPORT.md) - 性能测试报告
- [PERFORMANCE_TESTING_GUIDE.md](PERFORMANCE_TESTING_GUIDE.md) - 性能测试指南
- [PERFORMANCE_COMPARISON.md](PERFORMANCE_COMPARISON.md) - Snow语言与Java性能比较报告

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

### 运行Java基准测试（用于比较）

```bash
# 编译并运行Java基准测试
javac JavaBenchmark.java && java JavaBenchmark
```

## 性能测试内容

1. **算术运算性能** - 测试基本数学运算的执行速度
2. **字符串操作性能** - 测试字符串连接和操作的效率
3. **循环执行性能** - 测试循环结构的执行效率
4. **函数调用开销** - 测试函数调用的性能开销

## 性能比较结果

根据测试结果，Snow语言与Java在性能方面存在显著差异：

- **算术运算**：Snow比Java慢约1940倍
- **字符串操作**：Snow比Java慢约1275倍
- **循环执行**：Snow比Java慢约79倍
- **函数调用**：Snow比Java慢（Java几乎无开销）

详细比较结果请参见[性能比较报告](PERFORMANCE_COMPARISON.md)。

## 查看详细文档

- [性能测试报告](PERFORMANCE_REPORT.md) - 包含详细的测试结果和分析
- [性能测试指南](PERFORMANCE_TESTING_GUIDE.md) - 提供创建和运行性能测试的详细指导
- [性能比较报告](PERFORMANCE_COMPARISON.md) - Snow语言与Java的性能比较分析

## 注意事项

1. 测试结果可能因硬件和系统环境不同而有所差异
2. 建议多次运行测试以获得更准确的结果
3. 在进行性能测试时，尽量关闭其他占用系统资源的程序
4. Snow语言作为解释型语言，性能与编译型语言存在天然差距，这是正常现象