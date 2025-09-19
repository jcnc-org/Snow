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
 * 实现 MEMINFO () -> map { heapTotal, heapFree, heapUsed, heapMax, physicalTotal?, physicalFree?, physicalUsed?, ... }
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
                } catch (Throwable ignored) { }

                try {
                    // getProcessCpuLoad / getSystemCpuLoad return double in [0.0,1.0] or NaN
                    info.put("processCpuLoad", sunOs.getProcessCpuLoad());
                    info.put("systemCpuLoad", sunOs.getCpuLoad());
                } catch (Throwable ignored) { }
            }
        } catch (Throwable t) {
            // 如果无法获取平台级信息，则忽略（仍返回 heap 信息）
        }

        // push the map back onto the operand stack
        stack.push(info);
    }
}
