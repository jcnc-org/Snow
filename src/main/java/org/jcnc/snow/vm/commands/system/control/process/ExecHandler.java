package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.List;
import java.util.Map;

public class ExecHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 入参顺序：env → argv → path
        Object envObj = stack.pop();
        Object argvObj = stack.pop();
        String path = (String) stack.pop();

        // 参数解析
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

