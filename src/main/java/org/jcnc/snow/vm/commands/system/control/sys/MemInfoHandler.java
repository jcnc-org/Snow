package org.jcnc.snow.vm.commands.system.control.sys;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code MemInfoHandler} 实现 MEMINFO (0x1906) 系统调用，
 * 用于获取内存与系统资源信息。
 *
 * <p><b>Stack</b>：入参 {@code ()} → 出参 {@code (map:Map<String,Object>)}</p>
 *
 * <p><b>语义</b>：收集 JVM 堆内存信息以及在可用时的操作系统物理内存 / CPU 负载等指标，返回为键值映射。</p>
 *
 * <p><b>返回（典型键）</b>：
 * <ul>
 *   <li>{@code "heapTotal"}: JVM 堆已分配总字节数（long）</li>
 *   <li>{@code "heapFree"}: JVM 堆空闲字节数（long）</li>
 *   <li>{@code "heapUsed"}: JVM 堆已使用字节数（long）</li>
 *   <li>{@code "heapMax"}: JVM 堆最大可用字节数（long）</li>
 *   <li>{@code "physicalTotal"}: 物理内存总量（long，平台可用时提供）</li>
 *   <li>{@code "physicalFree"}: 物理内存空闲量（long，平台可用时提供）</li>
 *   <li>{@code "physicalUsed"}: 物理内存已用量（long，平台可用时提供）</li>
 * </ul>
 * </p>
 *
 * <p><b>异常</b>：收集平台级指标时若遇到权限或平台差异，处理器会忽略这些额外项并仍返回 JVM heap 信息；因此通常不会向上抛出异常。</p>
 */
public class MemInfoHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // JVM heap info
        Runtime rt = Runtime.getRuntime();
        long heapTotal = rt.totalMemory();
        long heapFree = rt.freeMemory();
        long heapUsed = heapTotal - heapFree;
        long heapMax = rt.maxMemory();

        Map<String, Object> info = new HashMap<>();
        info.put("heapTotal", heapTotal);
        info.put("heapFree", heapFree);
        info.put("heapUsed", heapUsed);
        info.put("heapMax", heapMax);

        // Try to get OS-level physical memory info if available
        try {
            // Use ManagementFactory and cast if com.sun.management.OperatingSystemMXBean is present
            OperatingSystemMXBean base = ManagementFactory.getOperatingSystemMXBean();
            if (base instanceof com.sun.management.OperatingSystemMXBean sunOs) {

                long physTotal = sunOs.getTotalMemorySize();
                long physFree = sunOs.getFreeMemorySize();
                long physUsed = physTotal - physFree;

                info.put("physicalTotal", physTotal);
                info.put("physicalFree", physFree);
                info.put("physicalUsed", physUsed);

                // Additional useful metrics (may return -1 or NaN on some platforms)
                try {
                    info.put("committedVirtual", sunOs.getCommittedVirtualMemorySize());
                } catch (Throwable ignored) {
                }

                try {
                    // getProcessCpuLoad / getSystemCpuLoad return double in [0.0,1.0] or NaN
                    info.put("processCpuLoad", sunOs.getProcessCpuLoad());
                    info.put("systemCpuLoad", sunOs.getCpuLoad());
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            // 如果无法获取平台级信息，则忽略（仍返回 heap 信息）
        }

        // push the map back onto the operand stack
        stack.push(info);
    }
}
