package org.jcnc.snow.vm.runtime;

import org.jcnc.snow.vm.value.Value;

import java.util.ArrayList;
import java.util.List;

public final class SnowArrayObject implements HeapObject {
    private final ArrayList<Value> items;

    public SnowArrayObject(List<Value> items) {
        this.items = new ArrayList<>(items == null ? List.of() : items);
    }

    public SnowArrayObject() {
        this.items = new ArrayList<>();
    }

    @Override
    public HeapObjectKind kind() {
        return HeapObjectKind.ARRAY;
    }

    public int length() {
        return items.size();
    }

    public Value get(int index) {
        return items.get(index);
    }

    public void set(int index, Value value) {
        items.set(index, value);
    }

    public void push(Value value) {
        items.add(value);
    }

    public Value pop() {
        if (items.isEmpty()) throw new IndexOutOfBoundsException("empty array");
        return items.removeLast();
    }

    public void insert(int index, Value value) {
        items.add(index, value);
    }

    public Value remove(int index) {
        return items.remove(index);
    }

    public void resize(int newLen) {
        if (newLen < 0) throw new IllegalArgumentException("newLen must be >= 0");
        while (items.size() > newLen) items.removeLast();
        while (items.size() < newLen) items.add(Value.NULL);
    }

    public void clear() {
        items.clear();
    }

    public List<Value> snapshot() {
        return List.copyOf(items);
    }
}