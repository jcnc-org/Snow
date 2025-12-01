// 简单的Java性能测试程序
class JavaBenchmark {

    // 获取当前时间（毫秒）
    public static long getTimeMs() {
        return System.currentTimeMillis();
    }

    // 算术运算测试
    public static void benchmarkArithmetic() {
        System.out.print("Arithmetic test... ");
        long startTime = getTimeMs();

        int result = 0;
        for (int i = 0; i < 100000; i++) {
            result = result + i;
        }

        long endTime = getTimeMs();
        System.out.println("completed in " + (endTime - startTime) + " ms");
    }

    // 字符串操作测试
    public static void benchmarkStringOps() {
        System.out.print("String operations test... ");
        long startTime = getTimeMs();

        String str = "";
        for (int i = 0; i < 1000; i++) {
            str = str + "a";
        }

        long endTime = getTimeMs();
        System.out.println("completed in " + (endTime - startTime) + " ms (length: " + str.length() + ")");
    }

    // 循环测试
    public static void benchmarkLoops() {
        System.out.print("Loop test... ");
        long startTime = getTimeMs();

        int sum = 0;
        for (int i = 0; i < 1000000; i++) {
            sum = sum + 1;
        }

        long endTime = getTimeMs();
        System.out.println("completed in " + (endTime - startTime) + " ms");
    }

    // 简单加法函数
    public static int add(int a, int b) {
        return a + b;
    }

    // 函数调用测试
    public static void benchmarkFunctionCalls() {
        System.out.print("Function calls test... ");
        long startTime = getTimeMs();

        int result = 0;
        for (int i = 0; i < 10000; i++) {
            result = result + add(1, 2);
        }

        long endTime = getTimeMs();
        System.out.println("completed in " + (endTime - startTime) + " ms");
    }

    public static void main(String[] args) {
        System.out.println("Simple Java Performance Benchmark");
        System.out.println("==============================");

        // 算术运算测试
        benchmarkArithmetic();

        // 字符串操作测试
        benchmarkStringOps();

        // 循环测试
        benchmarkLoops();

        // 函数调用测试
        benchmarkFunctionCalls();
    }
}