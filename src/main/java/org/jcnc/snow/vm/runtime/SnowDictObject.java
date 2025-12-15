package org.jcnc.snow.vm.runtime;

import org.jcnc.snow.vm.value.Value;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SnowDictObject is a stable, specifiable dictionary object.
 * Keys are strings; values are Snow {@link Value}s.
 *
 * <p>Internally it uses a Java map, but that map must never cross the ABI boundary.</p>
 */
public final class SnowDictObject implements HeapObject {
    private final LinkedHashMap<String, Value> entries;

    public SnowDictObject() {
        this.entries = new LinkedHashMap<>();
    }

    public SnowDictObject(Map<String, Value> initial) {
        this.entries = new LinkedHashMap<>(initial == null ? Map.of() : initial);
    }

    @Override
    public HeapObjectKind kind() {
        return HeapObjectKind.DICT;
    }

    public void put(String key, Value value) {
        entries.put(key, value);
    }

    public Value get(String key) {
        return entries.getOrDefault(key, Value.NULL);
    }

    public Map<String, Value> snapshot() {
        return Map.copyOf(entries);
    }
}