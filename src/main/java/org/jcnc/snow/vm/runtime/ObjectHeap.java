package org.jcnc.snow.vm.runtime;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ObjectHeap {
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, HeapObject> objects = new ConcurrentHashMap<>();

    public int alloc(HeapObject obj) {
        int id = nextId.getAndIncrement();
        objects.put(id, obj);
        return id;
    }

    public HeapObject get(int id) {
        HeapObject obj = objects.get(id);
        if (obj == null) throw new SnowPanicException("Invalid heap object id: " + id);
        return obj;
    }
}