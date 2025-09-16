package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.List;
import java.util.Map;

/**
 * {@code ExecHandler} 实现 EXEC (0x1103) 系统调用，
 * 用于在虚拟机中以指定环境和参数执行新进程，并模拟 exec 行为。
 *
 * <p><b>Stack</b>：入参 {@code (env:Map<String,String>, argv:List<String>, path:String)} → 无返回</p>
 *
 * <p><b>语义</b>：以指定的路径 {@code path} 和参数 {@code argv} 及环境变量 {@code env} 启动新进程，
 * 并模拟 exec：当前 JVM 在启动新进程后立即退出。</p>
 *
 * <p><b>返回</b>：无（JVM 退出）</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>参数类型错误时抛出 {@link IllegalArgumentException}</li>
 *   <li>进程启动失败时抛出 {@link java.io.IOException}</li>
 * </ul>
 * </p>
 */
public class ExecHandler implements SyscallHandler {

    /**
     * 处理 EXEC 调用。
     *
     * @param stack     操作数栈，依次提供 env、argv、path 参数
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception 当参数错误或进程启动失败时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 入参顺序：env → argv → path
        Object envObj = stack.pop();
        Object argvObj = stack.pop();
        Object pathObj = stack.pop();

        // 参数解析与类型检查
        if (!(pathObj instanceof String path)) {
            throw new IllegalArgumentException("path 必须为 String 类型");
        }
        List<String> argv = (argvObj instanceof List) ? (List<String>) argvObj : List.of();
        Map<String, String> env = (envObj instanceof Map) ? (Map<String, String>) envObj : Map.of();

        // 构建命令：path + argv
        List<String> command = new java.util.ArrayList<>();
        command.add(path);
        command.addAll(argv);

        ProcessBuilder pb = new ProcessBuilder(command);
        if (!env.isEmpty()) {
            pb.environment().putAll(env);
        }

        // 继承当前进程的 I/O
        pb.inheritIO();

        // 启动新进程
        Process process = pb.start();

        // 模拟 exec：等待新进程启动成功后，立即退出当前 JVM
        System.exit(0);
    }
}
