package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.net.InetAddress;
import java.util.*;

public class GetAddrInfoHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 取参数 (注意栈顺序: hints → service → host)
        Object hintsObj = stack.pop();
        String service = (String) stack.pop();
        String host = (String) stack.pop();

        // 2. 解析 service → port
        int port;
        try {
            port = Integer.parseInt(service);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid service/port: " + service);
        }

        // 3. 解析 host → IP 列表
        InetAddress[] addresses = InetAddress.getAllByName(host);

        // 4. 构造返回数组
        List<Map<String, Object>> results = new ArrayList<>();
        for (InetAddress addr : addresses) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("addr", addr.getHostAddress());
            entry.put("port", port);
            entry.put("family", addr instanceof java.net.Inet6Address ? 6 : 4);
            results.add(entry);
        }

        // 5. 压回数组 (假设 OperandStack 支持直接存放 List)
        stack.push(results);
    }
}
