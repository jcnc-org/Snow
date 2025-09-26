package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;
import org.jcnc.snow.vm.io.EnvRegistry;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * {@code ExecHandler} 实现 EXEC (0x1502) 系统调用，
 * 启动新进程并立即终结当前 VM 实例（无返回）。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (env:Map<String,String>, argv:List<String>, path:String)} → 无返回
 * </p>
 *
 * <p><b>语义：</b>
 * <ul>
 *   <li>以指定可执行文件 {@code path}，参数 {@code argv}，环境变量 {@code env} 启动新进程</li>
 *   <li>先传递当前进程所有 setenv 层变量（{@link EnvRegistry#snapshot()}），再以 {@code env} 覆盖/清除</li>
 *   <li>IO 继承当前进程（{@code inheritIO}）</li>
 *   <li>调用后当前 VM 直接终止，不再返回</li>
 * </ul>
 * </p>
 *
 * <p><b>返回：</b>
 * <ul>
 *   <li>本系统调用无返回值，调用后当前进程终止（无返回栈项）</li>
 * </ul>
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>path 不是 String 时抛出 {@link IllegalArgumentException}</li>
 *   <li>进程启动失败时抛出 {@link java.io.IOException}</li>
 *   <li>栈参数不足时抛出 {@link IllegalStateException}</li>
 * </ul>
 * </p>
 *
 * <p><b>注意事项：</b>
 * <ul>
 *   <li>本操作是不可逆的；当前虚拟机进程会被强制终止</li>
 *   <li>如参数类型不符，则不会启动新进程</li>
 * </ul>
 * </p>
 */
public class ExecHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 参数检查
        if (stack.size() < 3) {
            throw new IllegalStateException("EXEC 需要 3 个参数 (env:Map, argv:List, path:String)");
        }

        // 2. 入参顺序：env → argv → path（栈是 LIFO，pop 顺序相反）
        Object envObj = stack.pop();
        Object argvObj = stack.pop();
        Object pathObj = stack.pop();

        // 3. 检查 path 类型
        if (!(pathObj instanceof String path)) {
            throw new IllegalArgumentException("EXEC: path 必须是 String");
        }

        // 4. 参数类型兼容处理
        @SuppressWarnings("unchecked")
        List<String> argv = (argvObj instanceof List) ? (List<String>) argvObj : List.of();
        @SuppressWarnings("unchecked")
        Map<String, String> env = (envObj instanceof Map) ? (Map<String, String>) envObj : Map.of();

        // 5. 构造命令行
        List<String> command = new ArrayList<>(argv.size() + 1);
        command.add(path);
        command.addAll(argv);

        ProcessBuilder pb = new ProcessBuilder(command);

        // 6. 合并环境变量
        Map<String, String> pbEnv = pb.environment();
        pbEnv.putAll(EnvRegistry.snapshot());
        if (!env.isEmpty()) {
            pbEnv.putAll(env);
        }

        pb.inheritIO(); // 继承当前进程 IO

        // 7. 启动新进程
        pb.start();

        // 8. 终止当前 VM（无返回，立即退出）
        Runtime.getRuntime().halt(0);
    }
}
