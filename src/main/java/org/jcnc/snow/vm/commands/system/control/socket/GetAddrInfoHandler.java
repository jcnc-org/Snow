package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.net.InetAddress;

/**
 * {@code GetAddrInfoHandler} 实现 GETADDRINFO (0x140E) 系统调用，
 * 用于解析主机名和服务端口，返回可用于 socket 连接的地址列表。
 *
 * <p><b>Stack</b>：入参 {@code (host:String, service:String, hints:any?)} →
 * 出参 {@code (Object[][])}</p>
 *
 * <p><b>语义</b>：将 host/service 解析为一组 {addr, port, family} 的数组。</p>
 *
 * <p><b>返回</b>：地址信息二维数组：
 * <ul>
 *   <li>[i][0] = addr:String</li>
 *   <li>[i][1] = port:int</li>
 *   <li>[i][2] = family:int (4 或 6)</li>
 * </ul>
 * </p>
 */
public class GetAddrInfoHandler implements SyscallHandler {

    /**
     * 处理 GETADDRINFO 调用。
     *
     * @param stack     操作数栈，依次提供 host、service、hints
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception 参数无效或主机解析失败时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 参数顺序: hints → service → host
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
        Object[][] results = new Object[addresses.length][3];
        for (int i = 0; i < addresses.length; i++) {
            InetAddress addr = addresses[i];
            results[i][0] = addr.getHostAddress();
            results[i][1] = port;
            results[i][2] = (addr instanceof java.net.Inet6Address) ? 6 : 4;
        }

        // 5. 压回二维数组
        stack.push(results);
    }
}
