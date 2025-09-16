package org.jcnc.snow.vm.io;

import java.util.concurrent.ConcurrentHashMap;

public class ThreadRegistry {
    private static final ConcurrentHashMap<Long, Thread> threads = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Object> results = new ConcurrentHashMap<>();

    public static void register(Thread t) {
        threads.put(t.threadId(), t);
    }

    public static Thread get(long tid) {
        return threads.get(tid);
    }

    public static void unregister(long tid) {
        threads.remove(tid);
        results.remove(tid);
    }

    public static void setResult(long tid, Object result) {
        results.put(tid, result);
    }

    public static Object getResult(long tid) {
        return results.get(tid);
    }
}
