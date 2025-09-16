package org.jcnc.snow.vm.io;

import java.util.concurrent.ConcurrentHashMap;

public class ProcessRegistry {
    private static final ConcurrentHashMap<Long, Process> processes = new ConcurrentHashMap<>();

    public static void register(Process p) {
        processes.put(p.pid(), p);
    }

    public static Process get(long pid) {
        return processes.get(pid);
    }

    public static void unregister(long pid) {
        processes.remove(pid);
    }

    public static Iterable<Process> all() {
        return processes.values();
    }
}
